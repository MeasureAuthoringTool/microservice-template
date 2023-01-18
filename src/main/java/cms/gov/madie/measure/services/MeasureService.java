package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.exceptions.InvalidCmsIdException;
import cms.gov.madie.measure.exceptions.InvalidDeletionCredentialsException;
import cms.gov.madie.measure.exceptions.InvalidMeasurementPeriodException;
import cms.gov.madie.measure.exceptions.InvalidVersionIdException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.resources.DuplicateKeyException;
import cms.gov.madie.measure.validations.CqlDefinitionReturnTypeValidator;
import cms.gov.madie.measure.validations.CqlObservationFunctionValidator;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import gov.cms.madie.models.measure.Population;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@AllArgsConstructor
public class MeasureService {
  private final MeasureRepository measureRepository;
  private final FhirServicesClient fhirServicesClient;
  private final ElmTranslatorClient elmTranslatorClient;
  private final CqlDefinitionReturnTypeValidator cqlDefinitionReturnTypeValidator;
  private final CqlObservationFunctionValidator cqlObservationFunctionValidator;

  public Measure updateMeasure(final Measure existingMeasure, final String username, Measure updatingMeasure, final String accessToken) {
    // TODO: fill this in
    if (isCqlLibraryNameChanged(updatingMeasure, existingMeasure)) {
      checkDuplicateCqlLibraryName(updatingMeasure.getCqlLibraryName());
    }

    checkVersionIdChanged(
        updatingMeasure.getVersionId(), existingMeasure.getVersionId());
    checkCmsIdChanged(updatingMeasure.getCmsId(), existingMeasure.getCmsId());

    if (isMeasurementPeriodChanged(updatingMeasure, existingMeasure)) {
      validateMeasurementPeriod(
          updatingMeasure.getMeasurementPeriodStart(), updatingMeasure.getMeasurementPeriodEnd());
    }

    if (isMeasureCqlChanged(existingMeasure, updatingMeasure)) {
      log.info("Detected CQL change for measure with ID [{}] by user [{}]...updating ELM", updatingMeasure.getId(), username);
      updatingMeasure = updateElm(updatingMeasure, accessToken);
      updatingMeasure = validateAllMeasureGroupReturnTypes(updatingMeasure);
    }
    log.info("saving with errors on measure: {}", updatingMeasure.getErrors());
    updatingMeasure.setLastModifiedBy(username);
    updatingMeasure.setLastModifiedAt(Instant.now());
    // prevent users from overwriting the createdAt/By
    updatingMeasure.setCreatedAt(existingMeasure.getCreatedAt());
    updatingMeasure.setCreatedBy(existingMeasure.getCreatedBy());
    return measureRepository.save(updatingMeasure);
  }

  public Measure validateAllMeasureGroupReturnTypes(Measure measure) {
    final String elmJson = measure.getElmJson();
    boolean groupsExistWithPopulations = isGroupsExistWithPopulations(measure);
    if (elmJson == null && groupsExistWithPopulations) {
      log.info("Measure has groups with populations with definitions, but ELM JSON is missing!");
      // TODO: add more context to errors flag here
      measure.setCqlErrors(true);
      measure = measure.toBuilder()
          .error("CQL_RETURN_TYPE_MISMATCH")
          .error("ELM_MISSING")
          .build();
    } else if(elmJson != null && groupsExistWithPopulations) {
      if (measure.getGroups().stream().anyMatch(group ->
          !isGroupReturnTypesValid(group, elmJson))) {
        log.info("Mismatch exists between CQL return types and Population Criteria definition types!");
        measure = measure.toBuilder().error("CQL_RETURN_TYPE_MISMATCH").build();
      } else if (measure.getErrors() != null && measure.getErrors().contains("CQL_RETURN_TYPE_MISMATCH")) {
        log.info("No CQL return type mismatch! Woo!");
        Set<String> updatedErrors = measure.getErrors()
            .stream()
            .filter(e -> !StringUtils.equals("CQL_RETURN_TYPE_MISMATCH", e))
            .collect(Collectors.toSet());
        log.info("updated errors; {}", updatedErrors);
        measure = measure.toBuilder()
            .clearErrors()
            .errors(updatedErrors)
            .build();
      }
    }
    return measure;
  }

  public boolean isGroupReturnTypesValid(final Group group, final String elmJson) {
    try {
      cqlDefinitionReturnTypeValidator.validateCqlDefinitionReturnTypes(group, elmJson);
    } catch (Exception ex) {
      // Either no return types were found in ELM, or return type mismatch exists
      log.error("An error occurred while validating population return types", ex);
      return false;
    }
    try {
      cqlObservationFunctionValidator.validateObservationFunctions(group, elmJson);
    } catch (Exception ex) {
      // Either no return types were found in ELM, or return type mismatch exists
      log.error("An error occurred while validating observation return types", ex);
      return false;
    }
    return true;
  }

  public boolean isGroupsExistWithPopulations(Measure measure) {
    if (measure == null || measure.getGroups() == null || measure.getGroups().isEmpty()) {
      return false;
    }
    return measure.getGroups().stream().anyMatch((group) -> {
      final List<Population> populations = group.getPopulations();
      if (populations == null)
        return false;
      return populations.stream().anyMatch(population ->
        StringUtils.isNotBlank(population.getDefinition())
      );
    });
  }

  public boolean isMeasureCqlChanged(final Measure original, final Measure updated) {
//    if (original == null && updated != null) {
//      log.info("isMeasureCqlChanged - original null, updated not null");
//      return true;
//    }
//    if (original != null && updated == null) {
//      log.info("isMeasureCqlChanged - original not null, updated null");
//      return true;
//    }
//    if (original == null && updated == null) {
//      log.info("isMeasureCqlChanged - original and updated both null");
//      return false;
//    }
    return !StringUtils.equals(original.getCql(), updated.getCql());
  }

  private boolean isCqlLibraryNameChanged(Measure measure, Measure persistedMeasure) {
    return !Objects.equals(persistedMeasure.getCqlLibraryName(), measure.getCqlLibraryName());
  }

  private boolean isMeasurementPeriodChanged(Measure measure, Measure persistedMeasure) {
    return !Objects.equals(
        persistedMeasure.getMeasurementPeriodStart(), measure.getMeasurementPeriodStart())
        || !Objects.equals(
        persistedMeasure.getMeasurementPeriodEnd(), measure.getMeasurementPeriodEnd());
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

      measure = measure.toBuilder()
          .elmJson(elmJson.getJson())
          .elmXml(elmJson.getXml())
          .build();
    }
    return measure;
  }

  public String bundleMeasure(Measure measure, String accessToken) {
    if (measure == null) {
      return null;
    }
    try {
      measure = updateElm(measure, accessToken);

      return fhirServicesClient.getMeasureBundle(measure, accessToken);
    } catch (CqlElmTranslationServiceException | CqlElmTranslationErrorException e) {
      throw e;
    } catch (Exception ex) {
      log.error("An error occurred while bundling measure {}", measure.getId(), ex);
      throw new BundleOperationException("Measure", measure.getId(), ex);
    }
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

  public void checkCmsIdChanged(String changedCmsId, String originalCmsId) {
    if (StringUtils.isBlank(changedCmsId) && !StringUtils.isBlank(originalCmsId)) {
      throw new InvalidCmsIdException(changedCmsId);
    } else if (!StringUtils.isBlank(changedCmsId)
        && !StringUtils.isBlank(originalCmsId)
        && !changedCmsId.equalsIgnoreCase(originalCmsId)) {
      throw new InvalidCmsIdException(changedCmsId);
    }
  }

  public boolean grantAccess(String measureid, String userid, String username) {
    boolean result = false;
    Optional<Measure> persistedMeasure = measureRepository.findById(measureid);
    if (persistedMeasure.isPresent()) {

      Measure measure = persistedMeasure.get();
      measure.setLastModifiedBy(username);
      measure.setLastModifiedAt(Instant.now());
      List<AclSpecification> acls;
      acls = measure.getAcls();
      AclSpecification spec = new AclSpecification();
      spec.setUserId(userid);
      spec.setRoles(
          new ArrayList<>() {
            {
              add(RoleEnum.SHARED_WITH);
            }
          });
      if (acls == null || acls.size() == 0) {
        acls =
            new ArrayList<>() {
              {
                add(spec);
              }
            };
      } else {
        acls.add(spec);
      }
      measure.setAcls(acls);
      measureRepository.save(measure);
      result = true;
    }
    return result;
  }
}
