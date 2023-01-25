package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BadVersionRequestException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.MeasureNotDraftableException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.utils.ControllerUtil;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class VersionService {

  private final ActionLogService actionLogService;
  private final MeasureRepository measureRepository;
  private final ElmTranslatorClient elmTranslatorClient;
  private final FhirServicesClient fhirServicesClient;

  private static final String VERSION_TYPE_MAJOR = "MAJOR";
  private static final String VERSION_TYPE_MINOR = "MINOR";
  private static final String VERSION_TYPE_PATCH = "PATCH";

  public Measure createVersion(String id, String versionType, String username, String accessToken)
      throws Exception {

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

    ControllerUtil.verifyAuthorization(username, measure);

    validateMeasureForVersioning(measure, username, accessToken);

    measure.getMeasureMetaData().setDraft(false);
    measure.setLastModifiedAt(Instant.now());
    measure.setLastModifiedBy(username);

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

    ResponseEntity<String> result =
        fhirServicesClient.saveMeasureInHapiFhir(savedMeasure, accessToken);

    log.info(
        "User [{}] successfully saved versioned measure with ID [{}] in HAPI FHIR",
        username,
        (result != null ? result.getBody() : " null"));

    return savedMeasure;
  }

  public Measure createDraft(String id, String measureName, String username) {
    Measure measure =
        measureRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Measure", id));

    ControllerUtil.verifyAuthorization(username, measure);
    if (!isDraftable(measure)) {
      throw new MeasureNotDraftableException(measure.getMeasureName());
    }
    Measure measureDraft = measure.toBuilder().build();
    measureDraft.setId(null);
    measureDraft.setVersionId(UUID.randomUUID().toString());
    measureDraft.setMeasureName(measureName);
    measureDraft.getMeasureMetaData().setDraft(true);
    // TODO: MAT-5237 add groups and tests back
    measureDraft.setGroups(List.of());
    measureDraft.setTestCases(List.of());
    var now = Instant.now();
    measureDraft.setCreatedAt(now);
    measureDraft.setLastModifiedAt(now);
    measureDraft.setCreatedBy(username);
    Measure savedDraft = measureRepository.save(measureDraft);
    log.info(
        "User [{}] created a draft for measure with id [{}]. Draft id is [{}]",
        username,
        measure.getId(),
        savedDraft.getId());
    actionLogService.logAction(savedDraft.getId(), Measure.class, ActionType.DRAFTED, username);
    return savedDraft;
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
    if (measure.getTestCases() != null
        && measure.getTestCases().stream()
            .filter(p -> !p.isValidResource())
            .findFirst()
            .isPresent()) {
      log.error(
          "User [{}] attempted to version measure with id [{}] which has invalid test cases",
          username,
          measure.getId());
      throw new BadVersionRequestException(
          "Measure", measure.getId(), username, "Measure has invalid test cases.");
    }
  }

  protected Version getNextVersion(Measure measure, String versionType) throws Exception {
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
    return "library " + cqlLibraryName + " version " + "\'" + version + "\'";
  }
}
