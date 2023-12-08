package cms.gov.madie.measure.config;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;

import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChangeUnit(id = "migrate_risk_adjustment", order = "1", author = "madie_dev")
public class MigrateRiskAdjustmentChangeUnit {

  @Setter private List<Measure> tempMeasures;

  @Execution
  public void migrateRiskAdjustment(MeasureRepository measureRepository) throws Exception {
    log.debug("Entering migrateRiskAdjustment()");

    List<Measure> measureList = measureRepository.findAll();
    if (CollectionUtils.isNotEmpty(measureList)) {
      setTempMeasures(measureList);
      measureList.stream()
          .filter(
              measure ->
                  measure.getModel().equalsIgnoreCase(ModelType.QI_CORE.getValue())
                      && CollectionUtils.isNotEmpty(measure.getRiskAdjustments()))
          .forEach(
              measure -> {
                StringBuilder sb = new StringBuilder();
                measure
                    .getRiskAdjustments()
                    .forEach(
                        rav -> {
                          sb.append(rav.getDefinition());
                          if (StringUtils.isNotBlank(rav.getDescription())) {
                            sb.append("-");
                            sb.append(rav.getDescription());
                          }
                          sb.append(" \n ");
                        });

                measure.setRiskAdjustmentDescription(
                    measure.getMeasureMetaData() != null
                            && StringUtils.isNotBlank(
                                measure.getMeasureMetaData().getRiskAdjustment())
                        ? measure.getMeasureMetaData().getRiskAdjustment() + "; " + sb.toString()
                        : sb.toString());
                measureRepository.save(measure);
              });
    }
  }

  @RollbackExecution
  public void rollbackExecution(MeasureRepository measureRepository) throws Exception {
    log.debug("Entering rollbackExecution()");
    if (CollectionUtils.isNotEmpty(tempMeasures)) {
      measureRepository.saveAll(tempMeasures);
    }
  }
}
