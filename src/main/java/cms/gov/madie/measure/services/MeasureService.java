package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.InvalidCmsIdException;
import cms.gov.madie.measure.exceptions.InvalidDeletionCredentialsException;
import cms.gov.madie.measure.exceptions.InvalidMeasurementPeriodException;
import cms.gov.madie.measure.exceptions.InvalidVersionIdException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.resources.DuplicateKeyException;
import cms.gov.madie.measure.utils.MeasureUtil;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Measure;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import gov.cms.madie.models.measure.MeasureErrorType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class MeasureService {
  private final MeasureRepository measureRepository;
  private final ElmTranslatorClient elmTranslatorClient;
  private final MeasureUtil measureUtil;

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

    checkCmsIdChanged(updatingMeasure.getCmsId(), existingMeasure.getCmsId());

    if (measureUtil.isMeasurementPeriodChanged(updatingMeasure, existingMeasure)) {
      validateMeasurementPeriod(
          updatingMeasure.getMeasurementPeriodStart(), updatingMeasure.getMeasurementPeriodEnd());
    }

    Measure outputMeasure = updatingMeasure;
    if (measureUtil.isMeasureCqlChanged(existingMeasure, updatingMeasure)) {
      try {
        outputMeasure =
            measureUtil.validateAllMeasureGroupReturnTypes(updateElm(updatingMeasure, accessToken));
        // no errors were encountered so remove the ELM JSON error
        // TODO: remove this when backend validations for CQL/ELM are enhanced
        outputMeasure.setErrors(measureUtil.removeError(outputMeasure.getErrors(), MeasureErrorType.ERRORS_ELM_JSON));
      } catch (CqlElmTranslationErrorException ex) {
        outputMeasure =
            updatingMeasure
                .toBuilder()
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
