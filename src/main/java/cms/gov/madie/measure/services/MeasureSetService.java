package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.*;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import cms.gov.madie.measure.utils.SequenceGeneratorUtil;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureSetService {

  private final MeasureSetRepository measureSetRepository;
  private final ActionLogService actionLogService;
  private final SequenceGeneratorUtil sequenceGeneratorUtil;

  public void createMeasureSet(
      final String harpId, final String measureId, final String savedMeasureSetId) {

    boolean isMeasureSetPresent = measureSetRepository.existsByMeasureSetId(savedMeasureSetId);
    if (!isMeasureSetPresent) {
      MeasureSet measureSet =
          MeasureSet.builder().owner(harpId).measureSetId(savedMeasureSetId).build();
      MeasureSet savedMeasureSet = measureSetRepository.save(measureSet);
      log.info(
          "Measure set [{}] is successfully created for the measure [{}]",
          savedMeasureSet.getId(),
          measureId);
      actionLogService.logAction(
          savedMeasureSet.getId(), Measure.class, ActionType.CREATED, harpId);
    }
  }

  public MeasureSet updateMeasureSetAcls(String measureSetId, AclSpecification aclSpec) {
    Optional<MeasureSet> OptionalMeasureSet = measureSetRepository.findByMeasureSetId(measureSetId);
    if (OptionalMeasureSet.isPresent()) {
      MeasureSet measureSet = OptionalMeasureSet.get();
      if (CollectionUtils.isEmpty(measureSet.getAcls())) {
        measureSet.setAcls(List.of(aclSpec));
      } else {
        measureSet.getAcls().add(aclSpec);
      }
      MeasureSet updatedMeasureSet = measureSetRepository.save(measureSet);
      log.info("SHARED acl added to Measure set [{}]", updatedMeasureSet.getId());
      return updatedMeasureSet;
    } else {
      String error =
          String.format(
              "Measure with set id `%s` can not be shared with `%s`, measure set may not exists.",
              measureSetId, aclSpec.getUserId());
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

  public MeasureSet createCmsId(String measureSetId, String sequenceName, String username) {
    Optional<MeasureSet> measureSet = measureSetRepository.findByMeasureSetId(measureSetId);
    if (!measureSet.isPresent()) {
      throw new InvalidMeasureSetIdException(
          "No measure set exists for measure with measure set id " + measureSetId);
    }
    if (measureSet.get().getCmsId() > 0) {
      throw new InvalidRequestException(
          "cms id exists for measure with measure set id " + measureSetId);
    }
    if (!sequenceName.equals("cms_id")) {
      throw new InvalidRequestException(
          sequenceName + " is not a valid sequence name for generating cms id");
    }
    int generatedSequenceNumber = sequenceGeneratorUtil.generateSequenceNumber(sequenceName);
    measureSet.get().setCmsId(generatedSequenceNumber);
    MeasureSet updatedMeasureSet = measureSetRepository.save(measureSet.get());
    log.info("cms id for the Measure set [{}] is successfully created", updatedMeasureSet.getId());
    actionLogService.logAction(
        updatedMeasureSet.getId(), Measure.class, ActionType.CREATED, username);
    return updatedMeasureSet;
  }

  public MeasureSet findByMeasureSetId(final String measureSetId) {
    return measureSetRepository.findByMeasureSetId(measureSetId).orElse(null);
  }
}
