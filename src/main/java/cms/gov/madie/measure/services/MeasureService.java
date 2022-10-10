package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.exceptions.InvalidCmsIdException;
import cms.gov.madie.measure.exceptions.InvalidDeletionCredentialsException;
import cms.gov.madie.measure.exceptions.InvalidMeasurementPeriodException;
import cms.gov.madie.measure.exceptions.InvalidVersionIdException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.resources.DuplicateKeyException;
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

    if (measurementPeriodEnd.compareTo(measurementPeriodStart) < 0) {
      throw new InvalidMeasurementPeriodException(
          "Measurement period end date should be greater than or"
              + " equal to measurement period start date.");
    }
  }

  public void checkDeletionCredentials(String username, String createdBy) {
    if (!username.equals(createdBy)) {
      throw new InvalidDeletionCredentialsException(username);
    }
  }

  public void verifyAuthorization(String username, Measure measure) {
    if (!measure.getCreatedBy().equals(username)
        && (CollectionUtils.isEmpty(measure.getAcls())
            || !measure.getAcls().stream().anyMatch(acl -> acl.getUserId().equals(username)))) {
      throw new UnauthorizedException("Measure", measure.getId(), username);
    }
  }

  public String bundleMeasure(Measure measure, String accessToken) {
    if (measure == null) {
      return null;
    }
    try {
      final ElmJson elmJson = elmTranslatorClient.getElmJson(measure.getCql(), accessToken);
      if (elmTranslatorClient.hasErrors(elmJson)) {
        throw new CqlElmTranslationErrorException(measure.getMeasureName());
      }
      measure.setElmJson(elmJson.getJson());
      measure.setElmXml(elmJson.getXml());

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
