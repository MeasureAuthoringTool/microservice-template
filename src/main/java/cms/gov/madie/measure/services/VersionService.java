package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BadVersionRequestException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.MeasureNotDraftableException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.ExportRepository;
import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Export;
import gov.cms.madie.models.measure.FhirMeasure;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.ReviewMetaData;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class VersionService {

  private final ActionLogService actionLogService;
  private final MeasureRepository measureRepository;
  private final ElmTranslatorClient elmTranslatorClient;
  private final FhirServicesClient fhirServicesClient;
  private final ExportRepository exportRepository;
  private final MeasureService measureService;

  public enum VersionValidationResult {
    VALID,
    TEST_CASE_ERROR
  }

  private static final String VERSION_TYPE_MAJOR = "MAJOR";
  private static final String VERSION_TYPE_MINOR = "MINOR";
  private static final String VERSION_TYPE_PATCH = "PATCH";

  public VersionValidationResult checkValidVersioning(
      String id, String versionType, String username, String accessToken) {
    Measure measure = validateVersionOptions(id, versionType, username, accessToken);

    //    if test cases are invalid but no exception has been thrown the versioning may continue.
    if (measure.getTestCases() != null
        && measure.getTestCases().stream().anyMatch(p -> !p.isValidResource())) {
      log.warn(
          "User [{}] attempted to version measure with id [{}] which has invalid test cases",
          username,
          measure.getId());
      return VersionValidationResult.TEST_CASE_ERROR;
    }
    return VersionValidationResult.VALID;
  }

  public Measure createVersion(String id, String versionType, String username, String accessToken)
      throws Exception {
    Measure measure = validateVersionOptions(id, versionType, username, accessToken);

    if (measure instanceof FhirMeasure) {
      return versionFhirMeasure(versionType, username, accessToken, measure);
    }
    return versionQdmMeasure(versionType, username, measure);
  }

  private Measure versionQdmMeasure(String versionType, String username, Measure measure) throws Exception {
    return version(versionType, username, measure);
  }

  private Measure versionFhirMeasure(String versionType, String username, String accessToken, Measure measure) throws Exception {
    Measure savedMeasure = version(versionType, username, measure);
    var measureBundle = fhirServicesClient.getMeasureBundle(savedMeasure, accessToken, "export");
    saveMeasureBundle(savedMeasure, measureBundle, accessToken, username);
    return savedMeasure;
  }

  private Measure version(String versionType, String username, Measure measure) throws Exception {
    measure.getMeasureMetaData().setDraft(false);
    measure.setLastModifiedAt(Instant.now());
    measure.setLastModifiedBy(username);
    setMeasureReviewMetaData(measure);
    Version oldVersion = measure.getVersion();
    Version newVersion = getNextVersion(measure, versionType);
    measure.setVersion(newVersion);
    String newCql =
        measure
            .getCql()
            .replace(
                generateLibraryContentLine(measure.getCqlLibraryName(), oldVersion),
                generateLibraryContentLine(measure.getCqlLibraryName(), newVersion));
    measure.setCql(newCql);
    Measure savedMeasure = measureRepository.save(measure);
    actionLogService.logAction(
        measure.getId(),
        Measure.class,
        VERSION_TYPE_MAJOR.equalsIgnoreCase(versionType)
            ? ActionType.VERSIONED_MAJOR
            : (VERSION_TYPE_MINOR.equalsIgnoreCase(versionType)
                ? ActionType.VERSIONED_MINOR
                : ActionType.VERSIONED_REVISIONNUMBER),
        username);
    log.info(
        "User [{}] successfully versioned measure with ID [{}]", username, savedMeasure.getId());
    return savedMeasure;
  }

  private Measure validateVersionOptions(
      String id, String versionType, String username, String accessToken) {
    Measure measure =
        measureRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Measure", id));

    if (!VERSION_TYPE_MAJOR.equalsIgnoreCase(versionType)
        && !VERSION_TYPE_MINOR.equalsIgnoreCase(versionType)
        && !VERSION_TYPE_PATCH.equalsIgnoreCase(versionType)) {
      throw new BadVersionRequestException(
          "Measure", measure.getId(), username, "Invalid version request.");
    }
    measureService.verifyAuthorization(username, measure);
    validateMeasureForVersioning(measure, username, accessToken);
    return measure;
  }

  public Measure createDraft(String id, String measureName, String username) {
    Measure measure =
        measureRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Measure", id));

    measureService.verifyAuthorization(username, measure);
    if (!isDraftable(measure)) {
      throw new MeasureNotDraftableException(measure.getMeasureName());
    }
    Measure measureDraft = measure.toBuilder().build();
    measureDraft.setId(null);
    measureDraft.setVersionId(UUID.randomUUID().toString());
    measureDraft.setMeasureName(measureName);
    measureDraft.getMeasureMetaData().setDraft(true);
    measureDraft.setGroups(cloneMeasureGroups(measure.getGroups()));
    measureDraft.setTestCases(cloneTestCases(measure.getTestCases(), measureDraft.getGroups()));
    var now = Instant.now();
    measureDraft.setCreatedAt(now);
    measureDraft.setLastModifiedAt(now);
    measureDraft.setCreatedBy(username);
    setMeasureReviewMetaDataForDraft(measureDraft);
    Measure savedDraft = measureRepository.save(measureDraft);
    log.info(
        "User [{}] created a draft for measure with id [{}]. Draft id is [{}]",
        username,
        measure.getId(),
        savedDraft.getId());
    actionLogService.logAction(savedDraft.getId(), Measure.class, ActionType.DRAFTED, username);
    return savedDraft;
  }

  private List<Group> cloneMeasureGroups(List<Group> groups) {
    if (!CollectionUtils.isEmpty(groups)) {
      return groups.stream()
          .map(group -> group.toBuilder().id(ObjectId.get().toString()).build())
          .collect(Collectors.toList());
    }
    return List.of();
  }

  private List<TestCase> cloneTestCases(List<TestCase> testCases, List<Group> draftGroups) {
    if (!CollectionUtils.isEmpty(testCases)) {
      return testCases.stream()
          .map(
              testCase -> {
                List<TestCaseGroupPopulation> updatedTestCaseGroupPopulations = new ArrayList<>();
                List<TestCaseGroupPopulation> testCaseGroups = testCase.getGroupPopulations();
                if (!CollectionUtils.isEmpty(testCaseGroups)) {
                  AtomicInteger indexHolder = new AtomicInteger();
                  updatedTestCaseGroupPopulations.addAll(
                      testCaseGroups.stream()
                          .map(
                              testCaseGroupPopulation ->
                                  testCaseGroupPopulation
                                      .toBuilder()
                                      .groupId(
                                          draftGroups.get(indexHolder.getAndIncrement()).getId())
                                      .build())
                          .toList());
                }
                return testCase
                    .toBuilder()
                    .id(ObjectId.get().toString())
                    .groupPopulations(updatedTestCaseGroupPopulations)
                    .build();
              })
          .collect(Collectors.toList());
    }
    return List.of();
  }

  /** Returns false if there is already a draft for the measure family. */
  private boolean isDraftable(Measure measure) {
    return !measureRepository.existsByMeasureSetIdAndActiveAndMeasureMetaDataDraft(
        measure.getMeasureSetId(), true, true);
  }

  private void validateMeasureForVersioning(Measure measure, String username, String accessToken) {
    if (!measure.getMeasureMetaData().isDraft()) {
      log.error(
          "User [{}] attempted to version measure with id [{}] which is not in a draft state",
          username,
          measure.getId());
      throw new BadVersionRequestException(
          "Measure", measure.getId(), username, "Measure is not in a draft state.");
    }
    if (measure.isCqlErrors()) {
      log.error(
          "User [{}] attempted to version measure with id [{}] which has CQL errors",
          username,
          measure.getId());
      throw new BadVersionRequestException(
          "Measure", measure.getId(), username, "Measure has CQL errors.");
    }
    if (StringUtils.isBlank(measure.getCql())) {
      log.error(
          "User [{}] attempted to version measure with id [{}] which has empty CQL",
          username,
          measure.getId());
      throw new BadVersionRequestException(
          "Measure", measure.getId(), username, "Measure has no CQL.");
    } else {
      final ElmJson elmJson = elmTranslatorClient.getElmJson(measure.getCql(), accessToken);
      if (elmTranslatorClient.hasErrors(elmJson)) {
        throw new CqlElmTranslationErrorException(measure.getMeasureName());
      }
    }
  }

  protected Version getNextVersion(Measure measure, String versionType) {
    Version version;

    if (VERSION_TYPE_MAJOR.equalsIgnoreCase(versionType)) {
      version =
          measureRepository
              .findMaxVersionByMeasureSetId(measure.getMeasureSetId())
              .orElse(new Version());
      return version.toBuilder().major(version.getMajor() + 1).minor(0).revisionNumber(0).build();

    } else if (VERSION_TYPE_MINOR.equalsIgnoreCase(versionType)) {
      version =
          measureRepository
              .findMaxMinorVersionByMeasureSetIdAndVersionMajor(
                  measure.getMeasureSetId(), measure.getVersion().getMajor())
              .orElse(new Version());
      return version.toBuilder().minor(version.getMinor() + 1).revisionNumber(0).build();

    } else if (VERSION_TYPE_PATCH.equalsIgnoreCase(versionType)) {
      version =
          measureRepository
              .findMaxRevisionNumberByMeasureSetIdAndVersionMajorAndMinor(
                  measure.getMeasureSetId(),
                  measure.getVersion().getMajor(),
                  measure.getVersion().getMinor())
              .orElse(new Version());
      return version.toBuilder().revisionNumber(version.getRevisionNumber() + 1).build();
    }

    return new Version();
  }

  private String generateLibraryContentLine(String cqlLibraryName, Version version) {
    return "library " + cqlLibraryName + " version " + "'" + version + "'";
  }

  protected void setMeasureReviewMetaData(Measure measure) {
    Instant today = Instant.now();
    if (measure.getReviewMetaData() == null) {
      ReviewMetaData reviewMetaData =
          ReviewMetaData.builder().approvalDate(today).lastReviewDate(today).build();
      measure.setReviewMetaData(reviewMetaData);
    } else {
      measure.getReviewMetaData().setApprovalDate(today);
      measure.getReviewMetaData().setLastReviewDate(today);
    }
  }

  protected void setMeasureReviewMetaDataForDraft(Measure measure) {
    if (measure.getReviewMetaData() != null) {
      measure.getReviewMetaData().setApprovalDate(null);
      measure.getReviewMetaData().setLastReviewDate(null);
    }
  }

  private void saveMeasureBundle(
      Measure savedMeasure, String measureBundle, String accessToken, String username) {
    Export export =
        Export.builder().measureId(savedMeasure.getId()).measureBundleJson(measureBundle).build();
    Export savedExport = exportRepository.save(export);
    log.info(
        "User [{}] successfully saved versioned measure's export data with ID [{}]",
        username,
        savedExport.getId());
    ResponseEntity<String> result =
        fhirServicesClient.saveMeasureInHapiFhir(savedMeasure, accessToken);
    if (result.getStatusCode() == HttpStatus.OK) {
      log.info(
          "User [{}] successfully saved versioned measure with ID [{}] in HAPI FHIR",
          username,
          result.getBody());
    } else {
      log.info(
          "User [{}] failed to save versioned measure in HAPI FHIR: {}",
          username,
          result.getBody());
    }
  }
}
