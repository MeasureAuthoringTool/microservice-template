package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureSetService {

  private final MeasureSetRepository measureSetRepository;
  private final ActionLogService actionLogService;

  public void createMeasureSet(
      final String harpId, final String measureId, final String savedMeasureSetId) {

    boolean isMeasureSetPresent =
        measureSetRepository.findByMeasureSetId(savedMeasureSetId).isPresent();
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
      List<AclSpecification> acls = measureSet.getAcls();
      if (CollectionUtils.isEmpty(acls)) {
        acls = List.of(aclSpec);
      } else {
        acls.add(aclSpec);
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
}
