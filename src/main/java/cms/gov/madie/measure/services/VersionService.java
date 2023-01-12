package cms.gov.madie.measure.services;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import cms.gov.madie.measure.exceptions.InternalServerErrorException;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.Version;
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

    Version oldVersion = measure.getVersion();
    Version newVersion = getNextVersion(measure, versionType);
    measure.setVersion(newVersion);

    String newCql =
        measure
            .getCql()
            .replace(
                getLibraryContentLine(measure.getCqlLibraryName(), oldVersion),
                getLibraryContentLine(measure.getCqlLibraryName(), newVersion));
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

  private void validateMeasureForVersioning(Measure measure, String username) {
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

  protected Version getNextVersion(Measure measure, String versionType) {
    Version version;
    try {
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
    } catch (RuntimeException ex) {
      log.error("VersionService::getNextVersion Exception while getting version number", ex);
      throw new InternalServerErrorException(
          "Unable to version measure with id: " + measure.getId(), ex);
    }
    return new Version();
  }

  private String getLibraryContentLine(String cqlLibraryName, Version version) {
    return "library " + cqlLibraryName + " version " + "\'" + version + "\'";
  }
}
