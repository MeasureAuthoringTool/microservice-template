package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.MeasureSetRepository;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureSetService {

  private final MeasureSetRepository measureSetRepository;
  private final ActionLogService actionLogService;

  public void createMeasureSet(
      final String harpId, final String measureId, final String savedMeasureSetId) {

    boolean isMeasureSetPresent = measureSetRepository.existsByMeasureSetId(savedMeasureSetId);
    if (!isMeasureSetPresent) {
      // only measure owners can transfer in MAT
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
}
