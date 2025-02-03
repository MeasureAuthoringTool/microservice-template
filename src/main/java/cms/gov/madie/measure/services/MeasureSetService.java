package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.*;
import cms.gov.madie.measure.repositories.GeneratorRepository;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import gov.cms.madie.models.access.AclOperation;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureSetService {

  private final MeasureRepository measureRepository;
  private final MeasureSetRepository measureSetRepository;
  private final GeneratorRepository generatorRepository;
  private final ActionLogService actionLogService;

  public void createMeasureSet(
      final String harpId, final String measureId, final String savedMeasureSetId, String cmsId) {

    boolean isMeasureSetPresent = measureSetRepository.existsByMeasureSetId(savedMeasureSetId);
    if (!isMeasureSetPresent) {
      MeasureSet measureSet =
          MeasureSet.builder()
              .owner(harpId)
              .measureSetId(savedMeasureSetId)
              .cmsId((cmsId != null && !cmsId.equals("0")) ? Integer.parseInt(cmsId) : null)
              .build();
      MeasureSet savedMeasureSet = measureSetRepository.save(measureSet);
      log.info(
          "Measure set [{}] is successfully created for the measure [{}]",
          savedMeasureSet.getId(),
          measureId);
      actionLogService.logAction(
          savedMeasureSet.getId(), Measure.class, ActionType.CREATED, harpId);
    }
  }

  /**
   * This method updates the ACLs based on given AclOperation for a measureSetId.
   *
   * @param measureSetId -> set id of a measure set
   * @param aclOperation -> AclOperation to be updated
   * @return an instance of MeasureSet
   */
  public MeasureSet updateMeasureSetAcls(String measureSetId, AclOperation aclOperation) {
    Optional<MeasureSet> optionalMeasureSet = measureSetRepository.findByMeasureSetId(measureSetId);
    if (optionalMeasureSet.isPresent()) {
      MeasureSet measureSet = optionalMeasureSet.get().toBuilder().build();
      if (AclOperation.AclAction.GRANT == aclOperation.getAction()) {
        if (CollectionUtils.isEmpty(measureSet.getAcls())) {
          // if no acl present, add it
          measureSet.setAcls(aclOperation.getAcls());
        } else {
          // update acl
          aclOperation
              .getAcls()
              .forEach(
                  acl -> {
                    // check if acl already present for the user
                    AclSpecification aclSpecification =
                        findAclSpecificationByUserId(measureSet, acl.getUserId());
                    // if acl does not present, add it
                    if (aclSpecification == null) {
                      measureSet.getAcls().add(acl);
                    } else {
                      aclSpecification.getRoles().addAll(acl.getRoles());
                    }
                  });
        }
      } else if (AclOperation.AclAction.REVOKE == aclOperation.getAction()) {
        aclOperation
            .getAcls()
            .forEach(
                acl -> {
                  // check if acl already present for the user
                  AclSpecification aclSpecification =
                      findAclSpecificationByUserId(measureSet, acl.getUserId());
                  if (aclSpecification != null) {
                    // remove roles from ACL
                    aclSpecification.getRoles().removeAll(acl.getRoles());
                    // after removing the roles if there is no role left, remove acl
                    if (aclSpecification.getRoles().isEmpty()) {
                      measureSet.getAcls().remove(aclSpecification);
                    }
                  }
                });
      }
      MeasureSet updatedMeasureSet = measureSetRepository.save(measureSet);
      log.info("ACL updated for Measure set [{}]", updatedMeasureSet.getId());
      return updatedMeasureSet;
    } else {
      String error =
          String.format(
              "Measure with set id `%s` can not be shared, measure set may not exists.",
              measureSetId);
      log.error(error);
      throw new ResourceNotFoundException(error);
    }
  }

  public MeasureSet updateOwnership(String measureSetId, String userId) {
    Optional<MeasureSet> OptionalMeasureSet = measureSetRepository.findByMeasureSetId(measureSetId);
    if (OptionalMeasureSet.isPresent()) {
      MeasureSet measureSet = OptionalMeasureSet.get();
      measureSet.setOwner(userId);
      MeasureSet updatedMeasureSet = measureSetRepository.save(measureSet);
      log.info("Owner changed in Measure set [{}]", updatedMeasureSet.getId());
      return updatedMeasureSet;
    } else {
      String error =
          String.format(
              "Measure with set id `%s` can not change ownership `%s`, measure set may not exist.",
              measureSetId, userId);
      log.error(error);
      throw new ResourceNotFoundException(error);
    }
  }

  public MeasureSet createAndUpdateCmsId(String measureSetId, String username) {
    Optional<MeasureSet> measureSet = measureSetRepository.findByMeasureSetId(measureSetId);
    if (!measureSet.isPresent()) {
      throw new ResourceNotFoundException(
          "No measure set exists for measure with measure set id " + measureSetId);
    }
    if (measureSet.get().getCmsId() != null) {
      throw new InvalidRequestException(
          "CMS ID already exists. Once a CMS Identifier has been generated it may not "
              + "be modified or removed for any draft or version of a measure.");
    }
    int generatedSequenceNumber = generatorRepository.findAndModify("cms_id");
    measureSet.get().setCmsId(generatedSequenceNumber);
    MeasureSet updatedMeasureSet = measureSetRepository.save(measureSet.get());
    log.info("cms id for the Measure set [{}] is successfully created", updatedMeasureSet.getId());
    actionLogService.logAction(
        updatedMeasureSet.getId(), Measure.class, ActionType.CREATED, username);
    return updatedMeasureSet;
  }

  public String deleteCmsId(String measureId, Integer cmsId, String harpId) {
    Optional<Measure> optionalMeasure = measureRepository.findById(measureId);

    if (optionalMeasure.isPresent()) {
      Measure measure = optionalMeasure.get();

      String measureSetId = measure.getMeasureSetId();
      Optional<MeasureSet> optionalMeasureSet =
          measureSetRepository.findByMeasureSetId(measureSetId);

      if (optionalMeasureSet.isEmpty()) {
        throw new ResourceNotFoundException(
            "No measure set exists for measure with measure set id of " + measureSetId);
      }

      MeasureSet measureSet = optionalMeasureSet.get();

      if (!measureSet.getOwner().equals(harpId)) {
        throw new HarpIdMismatchException(harpId, measureSet.getOwner(), measure.getId());
      }

      if (measureSet.getCmsId() == null) {
        throw new ResourceNotFoundException(
            String.format(
                "No CMS id of %s exists to be deleted "
                    + "within measure set with measure set id of %s",
                cmsId, measureSetId));
      }

      if (!measureSet.getCmsId().equals(cmsId)) {
        throw new InvalidIdException(
            String.format(
                "CMS id of %s passed in does not match CMS id of %s within "
                    + "measure set with measure set id of %s",
                cmsId, measureSet.getCmsId(), measureSetId));
      }

      List<Measure> measures = measureRepository.findAllByMeasureSetIdAndActive(measureSetId, true);

      if (measures.size() > 1) {
        throw new InvalidRequestException(
            String.format(
                "Measure set with measure set id of %s contains more than 1 measure. "
                    + "Cannot delete CMS id when measure set has more than 1 version of measure.",
                measureSetId));
      }

      measureSet.setCmsId(null);
      measureSetRepository.save(measureSet);

      log.info(
          "With the measure id of [{}], successfully queried "
              + "for its measure set with measure set id of [{}] and deleted CMS id "
              + "of [{}] from the measure set",
          measureId,
          measureSetId,
          cmsId);

      return String.format(
          "CMS id of %s was deleted successfully from " + "measure set with measure set id of %s",
          cmsId, measureSetId);
    } else {
      throw new ResourceNotFoundException("No measure exists with measure id of " + measureId);
    }
  }

  public MeasureSet findByMeasureSetId(final String measureSetId) {
    return measureSetRepository.findByMeasureSetId(measureSetId).orElse(null);
  }

  private AclSpecification findAclSpecificationByUserId(MeasureSet measureSet, String userId) {
    if (CollectionUtils.isEmpty(measureSet.getAcls())) {
      return null;
    }
    return measureSet.getAcls().stream()
        .filter(existingAcl -> Objects.equals(existingAcl.getUserId(), userId))
        .findFirst()
        .orElse(null);
  }
}
