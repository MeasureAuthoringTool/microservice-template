package cms.gov.madie.measure.services;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import cms.gov.madie.measure.exceptions.BadVersionRequestException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.utils.ControllerUtil;

@Slf4j
@AllArgsConstructor
@Service
public class VersionService {

  private final ActionLogService actionLogService;
  private final MeasureRepository measureRepository;

  private static final String VERSION_TYPE_MAJOR = "MAJOR";
  private static final String VERSION_TYPE_MINOR = "MINOR";
  private static final String VERSION_TYPE_PATCH = "PATCH";

  public Measure createVersion(String id, String versionType, String username, String accessToken) {

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

    validateMeasureForVersioning(measure, username);

    measure.getMeasureMetaData().setDraft(false);
    measure.setLastModifiedAt(Instant.now());
    measure.setLastModifiedBy(username);

    getNextVersion(measure, versionType);

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

  private void validateMeasureForVersioning(Measure measure, String username) {
    if (!measure.getMeasureMetaData().isDraft()) {
      log.error(
          "User [{}] attempted to version CQL Library with id [{}] which is not in a draft state",
          username,
          measure.getId());
      throw new BadVersionRequestException(
          "Measure", measure.getId(), username, "Measure is not in a draft state.");
    }
    if (measure.isCqlErrors()) {
      log.error(
          "User [{}] attempted to version CQL Library with id [{}] which has CQL errors",
          username,
          measure.getId());
      throw new BadVersionRequestException(
          "Measure", measure.getId(), username, "Measure has CQL errors.");
    }
    if (measure.getTestCases() == null
        || measure.getTestCases().stream()
            .filter(p -> !p.isValidResource())
            .findFirst()
            .isPresent()) {
      log.error(
          "User [{}] attempted to version CQL Library with id [{}] which has invalid resources",
          username,
          measure.getId());
      throw new BadVersionRequestException(
          "Measure", measure.getId(), username, "Measure has invalid resources.");
    }
  }

  protected void getNextVersion(Measure measure, String versionType) {
    if (VERSION_TYPE_MAJOR.equalsIgnoreCase(versionType)) {
      measure.getVersion().setMajor(measure.getVersion().getMajor() + 1);
      measure.getVersion().setMinor(0);
      measure.getVersion().setRevisionNumber(0);
    } else if (VERSION_TYPE_MINOR.equalsIgnoreCase(versionType)) {
      measure.getVersion().setMinor(measure.getVersion().getMinor() + 1);
      measure.getVersion().setRevisionNumber(0);
    } else if (VERSION_TYPE_PATCH.equalsIgnoreCase(versionType)) {
      measure.getVersion().setRevisionNumber(measure.getVersion().getRevisionNumber() + 1);
    }
  }
}
