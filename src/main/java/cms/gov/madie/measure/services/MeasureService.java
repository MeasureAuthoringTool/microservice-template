package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.*;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.resources.DuplicateKeyException;
import cms.gov.madie.measure.utils.MeasureUtil;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class MeasureService {
  private final MeasureRepository measureRepository;
  private final ElmTranslatorClient elmTranslatorClient;
  private final MeasureUtil measureUtil;
  private final ActionLogService actionLogService;
  private final MeasureSetService measureSetService;
  private final TerminologyValidationService terminologyValidationService;

  /**
   * Throws unAuthorizedException, if the measure is not owned by the user or if the measure is not
   * shared with the user
   */
  public void verifyAuthorization(String username, Measure measure) {
    verifyAuthorization(username, measure, List.of(RoleEnum.SHARED_WITH));
  }

  /**
   * Verifies the specified user has privileges on the given measure based on measure owner and the
   * passed roles. Providing null or empty roles will perform an authorization check for owner only.
   *
   * @param username
   * @param measure
   * @param roles
   */
  public void verifyAuthorization(String username, Measure measure, List<RoleEnum> roles) {
    MeasureSet measureSet =
        measure.getMeasureSet() == null
            ? measureSetService.findByMeasureSetId(measure.getMeasureSetId())
            : measure.getMeasureSet();
    if (measureSet == null) {
      throw new InvalidMeasureStateException(
          "No measure set exists for measure with ID " + measure.getId());
    }

    List<RoleEnum> allowedRoles = roles == null ? List.of() : roles;
    if (!measureSet.getOwner().equalsIgnoreCase(username)
        && (CollectionUtils.isEmpty(measureSet.getAcls())
            || measureSet.getAcls().stream()
                .noneMatch(
                    acl ->
                        acl.getUserId().equalsIgnoreCase(username)
                            && acl.getRoles().stream().anyMatch(allowedRoles::contains)))) {
      throw new UnauthorizedException("Measure", measure.getId(), username);
    }
  }

  // TODO: start replacing usage of measureRepository.findById with this method
  public Measure findMeasureById(final String id) {
    return measureRepository
        .findById(id)
        .map(
            m ->
                m.toBuilder()
                    .measureSet(measureSetService.findByMeasureSetId(m.getMeasureSetId()))
                    .build())
        .orElse(null);
  }

  public Measure createMeasure(Measure measure, final String username, String accessToken) {
    log.info("User [{}] is attempting to create a new measure", username);
    checkDuplicateCqlLibraryName(measure.getCqlLibraryName());
    validateMeasurementPeriod(
        measure.getMeasurementPeriodStart(), measure.getMeasurementPeriodEnd());
    updateMeasurementPeriods(measure);
    Measure measureCopy = measure.toBuilder().build();
    Set<MeasureErrorType> errorTypes = new HashSet<>();
    try {
      measureCopy = updateElm(measureCopy, accessToken);
    } catch (CqlElmTranslationErrorException ex) {
      errorTypes.add(MeasureErrorType.ERRORS_ELM_JSON);
    }
    try {
      terminologyValidationService.validateTerminology(measureCopy.getElmJson(), accessToken);
    } catch (InvalidTerminologyException ex) {
      errorTypes.add(MeasureErrorType.INVALID_TERMINOLOGY);
    }
    if (!CollectionUtils.isEmpty(errorTypes)) {
      measureCopy.setCqlErrors(true);
      measureCopy.setErrors(errorTypes);
    }
    Instant now = Instant.now();
    // Clear ID so that the unique GUID from MongoDB will be applied
    measureCopy.setId(null);
    measureCopy.setCreatedBy(username);
    measureCopy.setCreatedAt(now);
    measureCopy.setLastModifiedBy(username);
    measureCopy.setLastModifiedAt(now);
    measureCopy.setVersion(new Version(0, 0, 0));
    measureCopy.setVersionId(UUID.randomUUID().toString());
    measureCopy.setMeasureSetId(UUID.randomUUID().toString());
    if (measureCopy.getMeasureMetaData() != null) {
      measureCopy.getMeasureMetaData().setDraft(true);
    } else {
      MeasureMetaData metaData = new MeasureMetaData();
      metaData.setDraft(true);
      measureCopy.setMeasureMetaData(metaData);
    }

    Measure savedMeasure = measureRepository.save(measureCopy);
    log.info(
        "User [{}] successfully created new measure with ID [{}]", username, savedMeasure.getId());
    actionLogService.logAction(savedMeasure.getId(), Measure.class, ActionType.CREATED, username);

    measureSetService.createMeasureSet(
        username, savedMeasure.getId(), savedMeasure.getMeasureSetId(), null);
    return savedMeasure;
  }

  public Measure updateMeasure(
      final Measure existingMeasure,
      final String username,
      final Measure updatingMeasure,
      final String accessToken) {
    if (measureUtil.isCqlLibraryNameChanged(updatingMeasure, existingMeasure)) {
      checkDuplicateCqlLibraryName(updatingMeasure.getCqlLibraryName());
    }

    if (StringUtils.isBlank(existingMeasure.getVersionId())) {
      existingMeasure.setVersionId(UUID.randomUUID().toString());
    }
    if (StringUtils.isBlank(existingMeasure.getMeasureSetId())) {
      existingMeasure.setMeasureSetId(UUID.randomUUID().toString());
    }

    if (measureUtil.isMeasurementPeriodChanged(updatingMeasure, existingMeasure)) {
      validateMeasurementPeriod(
          updatingMeasure.getMeasurementPeriodStart(), updatingMeasure.getMeasurementPeriodEnd());
      updateMeasurementPeriods(updatingMeasure);
    }

    updateMeasureMetadataIds(updatingMeasure.getMeasureMetaData());

    Measure outputMeasure = updatingMeasure;
    if (measureUtil.isMeasureCqlChanged(existingMeasure, updatingMeasure)
        || measureUtil.isSupplementalDataChanged(existingMeasure, updatingMeasure)
        || measureUtil.isRiskAdjustmentChanged(existingMeasure, updatingMeasure)) {
      try {
        outputMeasure =
            measureUtil.validateAllMeasureDependencies(updateElm(updatingMeasure, accessToken));

        // remove this condition when we validate for teminology service errors in backend
        if (!outputMeasure.isCqlErrors()) {
          outputMeasure.setCqlErrors(updatingMeasure.isCqlErrors());
        }
        // no errors were encountered so remove the ELM JSON error
        // TODO: remove this when backend validations for CQL/ELM are enhanced
        outputMeasure.setErrors(
            measureUtil.removeError(outputMeasure.getErrors(), MeasureErrorType.ERRORS_ELM_JSON));
      } catch (CqlElmTranslationErrorException ex) {
        outputMeasure =
            updatingMeasure.toBuilder()
                .cqlErrors(true)
                .error(MeasureErrorType.ERRORS_ELM_JSON)
                .build();
      }
    } else {
      // prevent users from manually clearing errors!
      outputMeasure.setErrors(existingMeasure.getErrors());
    }
    outputMeasure.getMeasureMetaData().setDraft(existingMeasure.getMeasureMetaData().isDraft());
    outputMeasure.setLastModifiedBy(username);
    outputMeasure.setLastModifiedAt(Instant.now());
    // prevent users from overwriting the createdAt/By
    outputMeasure.setCreatedAt(existingMeasure.getCreatedAt());
    outputMeasure.setCreatedBy(existingMeasure.getCreatedBy());
    // prevent users from overwriting versionId and measureSetId
    outputMeasure.setVersionId(existingMeasure.getVersionId());
    outputMeasure.setMeasureSetId(existingMeasure.getMeasureSetId());
    return measureRepository.save(outputMeasure);
  }

  private void updateMeasurementPeriods(Measure measure) {
    Date startDate = measure.getMeasurementPeriodStart();
    Instant startInstant =
        startDate.toInstant().atOffset(ZoneOffset.UTC).with(LocalTime.MIN).toInstant();
    measure.setMeasurementPeriodStart(Date.from(startInstant));

    Date endDate = measure.getMeasurementPeriodEnd();
    Instant endInstant =
        endDate.toInstant().atOffset(ZoneOffset.UTC).with(LocalTime.MAX).toInstant();
    measure.setMeasurementPeriodEnd(Date.from(endInstant));
  }

  public Page<Measure> getMeasures(boolean filterByCurrentUser, Pageable pageReq, String username) {
    return filterByCurrentUser
        ? measureRepository.findMyActiveMeasures(username, pageReq, null)
        : measureRepository.findAllByActive(true, pageReq);
  }

  public void checkDuplicateCqlLibraryName(String cqlLibraryName) {
    if (StringUtils.isNotEmpty(cqlLibraryName)
        && measureRepository.findByCqlLibraryName(cqlLibraryName).isPresent()) {
      throw new DuplicateKeyException(
          "cqlLibraryName", "CQL library with given name already exists.");
    }
  }

  public void validateMeasurementPeriod(Date measurementPeriodStart, Date measurementPeriodEnd) {
    if (measurementPeriodStart == null || measurementPeriodEnd == null) {
      throw new InvalidMeasurementPeriodException(
          "Measurement period date is required and must be valid");
    }
    SimpleDateFormat checkYear = new SimpleDateFormat("yyyy");
    int checkMeasurementPeriodStart = Integer.parseInt(checkYear.format(measurementPeriodStart));
    int checkMeasurementPeriodEnd = Integer.parseInt(checkYear.format(measurementPeriodEnd));

    if (1900 > checkMeasurementPeriodStart
        || checkMeasurementPeriodStart > 2099
        || 1900 > checkMeasurementPeriodEnd
        || checkMeasurementPeriodEnd > 2099) {
      throw new InvalidMeasurementPeriodException(
          "Measurement periods should be between the years 1900 and 2099.");
    }

    if (measurementPeriodEnd.compareTo(measurementPeriodStart) < 1) {
      throw new InvalidMeasurementPeriodException(
          "Measurement period end date should be greater than measurement period start date.");
    }
  }

  public void checkDeletionCredentials(String username, String createdBy) {
    if (!username.equalsIgnoreCase(createdBy)) {
      throw new InvalidDeletionCredentialsException(username);
    }
  }

  public Measure updateElm(Measure measure, String accessToken) {
    if (measure != null && StringUtils.isNotBlank(measure.getCql())) {
      final ElmJson elmJson = elmTranslatorClient.getElmJson(measure.getCql(), accessToken);
      if (elmTranslatorClient.hasErrors(elmJson)) {
        throw new CqlElmTranslationErrorException(measure.getMeasureName());
      }

      return measure.toBuilder().elmJson(elmJson.getJson()).elmXml(elmJson.getXml()).build();
    }
    return measure;
  }

  public void checkVersionIdChanged(String changedVersionId, String originalVersionId) {
    if (StringUtils.isBlank(changedVersionId) && !StringUtils.isBlank(originalVersionId)) {
      throw new InvalidVersionIdException(changedVersionId);
    } else if (!StringUtils.isBlank(changedVersionId)
        && !StringUtils.isBlank(originalVersionId)
        && !changedVersionId.equalsIgnoreCase(originalVersionId)) {
      throw new InvalidVersionIdException(changedVersionId);
    }
  }

  public boolean grantAccess(String measureId, String userid) {
    boolean result = false;
    Optional<Measure> persistedMeasure = measureRepository.findById(measureId);
    if (persistedMeasure.isPresent()) {
      Measure measure = persistedMeasure.get();
      AclSpecification spec = new AclSpecification();
      spec.setUserId(userid);
      spec.setRoles(
          new ArrayList<>() {
            {
              add(RoleEnum.SHARED_WITH);
            }
          });
      measureSetService.updateMeasureSetAcls(measure.getMeasureSetId(), spec);
      result = true;
    }
    return result;
  }

  public boolean changeOwnership(String measureId, String userid) {
    boolean result = false;
    Optional<Measure> persistedMeasure = measureRepository.findById(measureId);
    if (persistedMeasure.isPresent()) {
      Measure measure = persistedMeasure.get();
      measureSetService.updateOwnership(measure.getMeasureSetId(), userid);
      result = true;
    }
    return result;
  }

  public Map<String, Boolean> getMeasureDrafts(List<String> measureSetIds) {
    Map<String, Boolean> measureSetMap = new HashMap<>();
    List<Measure> measures =
        measureRepository.findAllByMeasureSetIdInAndActiveAndMeasureMetaDataDraft(
            measureSetIds, true, true);
    // for every found measureSetId, put the id & false (not draftable in the map)
    measureSetIds.forEach(
        id -> {
          if (measures.stream().anyMatch(measure -> measure.getMeasureSetId().equals(id))) {
            measureSetMap.put(id, Boolean.FALSE);
          } else { // measures doesn't contain ID
            measureSetMap.put(id, Boolean.TRUE);
          }
        });
    // For measureSetIds that were searched, but not returned put the id & true ( is draftable )

    return measureSetMap;
  }

  public List<String> getAllActiveMeasureIds(boolean draftOnly) {
    return (draftOnly
            ? measureRepository.findAllMeasureIdsByActiveAndMeasureMetaDataDraft(true)
            : measureRepository.findAllMeasureIdsByActive())
        .stream().map(Measure::getId).collect(Collectors.toList());
  }

  public Page<Measure> getMeasuresByCriteria(
      boolean filterByCurrentUser, Pageable pageReq, String username, String criteria) {
    return filterByCurrentUser
        ? measureRepository.findMyActiveMeasures(username, pageReq, criteria)
        : measureRepository.findAllByMeasureNameOrEcqmTitle(criteria, pageReq);
  }

  protected void updateMeasureMetadataIds(MeasureMetaData metaData) {
    updateMeasureDefinitionId(metaData);
    updateReferenceId(metaData);
  }

  protected void updateMeasureDefinitionId(MeasureMetaData metaData) {
    if (metaData != null && !CollectionUtils.isEmpty(metaData.getMeasureDefinitions())) {
      List<MeasureDefinition> measureDefinitions =
          metaData.getMeasureDefinitions().stream()
              .map(
                  measureDefinition ->
                      MeasureDefinition.builder()
                          .id(
                              StringUtils.isBlank(measureDefinition.getId())
                                  ? UUID.randomUUID().toString()
                                  : measureDefinition.getId())
                          .term(measureDefinition.getTerm())
                          .definition(measureDefinition.getDefinition())
                          .build())
              .toList();
      metaData.setMeasureDefinitions(measureDefinitions);
    }
  }

  protected void updateReferenceId(MeasureMetaData metaData) {
    if (metaData != null && !CollectionUtils.isEmpty(metaData.getReferences())) {
      List<Reference> references =
          (List<Reference>)
              metaData.getReferences().stream()
                  .map(
                      reference ->
                          Reference.builder()
                              .id(
                                  StringUtils.isBlank(reference.getId())
                                      ? UUID.randomUUID().toString()
                                      : reference.getId())
                              .referenceText(reference.getReferenceText())
                              .referenceType(reference.getReferenceType())
                              .build())
                  .toList();
      metaData.setReferences(references);
    }
  }

  public List<Measure> findAllByMeasureSetId(String measureSetId) {
    List<Measure> measures = measureRepository.findAllByMeasureSetId(measureSetId);
    return measures;
  }

  public void deleteVersionedMeasures(List<Measure> measures) {
    List<Measure> versionedMeasures =
        measures.stream()
            .filter(
                measure ->
                    measure.getMeasureMetaData() != null && !measure.getMeasureMetaData().isDraft())
            .collect(Collectors.toList());
    if (!CollectionUtils.isEmpty(versionedMeasures)) {
      String deletedMeasureIds =
          versionedMeasures.stream()
              .map(measure -> measure.getId())
              .collect(Collectors.joining(","));
      measureRepository.deleteAll(versionedMeasures);
      log.info("Versioned Measure IDs [{}] are deleted.", deletedMeasureIds);
    }
  }
}
