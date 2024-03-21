package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ChangeUnit(id = "measure_group_strat_id_update", order = "1", author = "madie_dev")
public class UpdateMeasureGroupStratificationsChangeUnit {
  @Execution
  public void updateMeasureGroupStratifications(MeasureRepository measureRepository) throws Exception {
    log.debug("Entering updateMeasureGroupStratifications()");

    List<Measure> measureList = measureRepository.findAllByModel(ModelType.QDM_5_6.getValue());
    log.info("found QDM measures: {}", measureList.size());
    final AtomicBoolean changed = new AtomicBoolean();
    measureList.forEach(measure -> {
        if (!measure.isActive() || CollectionUtils.isEmpty(measure.getGroups())) {
            return;
        }
        changed.set(false);

        measure.getGroups().forEach(group -> {
            if (CollectionUtils.isEmpty(group.getStratifications())) {
                return;
            }

            group.getStratifications().forEach(stratification -> {
                if (stratification != null && StringUtils.isBlank(stratification.getId())) {
                    stratification.setId(UUID.randomUUID().toString());
                    changed.set(true);
                }
            });
        });

        if (changed.get()) {
            measureRepository.save(measure);
        }
    });

  }

  @RollbackExecution
  public void rollbackExecution(MeasureRepository measureRepository) throws Exception {
    log.debug("Entering rollbackExecution() ");
    // do nothing...we don't want to delete IDs..
  }
}
