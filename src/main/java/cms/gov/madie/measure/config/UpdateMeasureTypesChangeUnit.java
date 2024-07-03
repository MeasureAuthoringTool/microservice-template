package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.BaseConfigurationTypes;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.QdmMeasure;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ChangeUnit(id = "measure_types_update", order = "1", author = "madie_dev")
public class UpdateMeasureTypesChangeUnit {

  @Execution
  public void updateMeasureTypes(MeasureRepository measureRepository) {
    log.info("Updating measure types");
    List<Measure> measures = measureRepository.findAll();

    HashMap<BaseConfigurationTypes, BaseConfigurationTypes> updatedMeasureTypeReplacementsMap =
        new HashMap<>();
    updatedMeasureTypeReplacementsMap.put(
        BaseConfigurationTypes.COST_OR_RESOURCE_USE, BaseConfigurationTypes.RESOURCE_USE);
    updatedMeasureTypeReplacementsMap.put(
        BaseConfigurationTypes.PATIENT_ENGAGEMENT_OR_EXPERIENCE, BaseConfigurationTypes.EXPERIENCE);

    if (CollectionUtils.isNotEmpty(measures)) {
      measures.forEach(
          measure -> {
            if (measure.getModel().equalsIgnoreCase(ModelType.QDM_5_6.getValue())) {
              QdmMeasure qdmMeasure = (QdmMeasure) measure;
              if (CollectionUtils.isNotEmpty(qdmMeasure.getBaseConfigurationTypes())) {
                List<BaseConfigurationTypes> updatedMeasureTypes =
                    qdmMeasure.getBaseConfigurationTypes().stream()
                        .map(
                            baseConfigurationType ->
                                updatedMeasureTypeReplacementsMap.getOrDefault(
                                    baseConfigurationType, baseConfigurationType))
                        .collect(Collectors.toList());

                qdmMeasure.setBaseConfigurationTypes(updatedMeasureTypes);
                measureRepository.save(qdmMeasure);
              }
            }
          });
    }
    log.info("Completed updateMeasureTypes()");
  }

  @RollbackExecution
  public void rollbackExecution() {
    log.debug("Something went wrong while updating measure types.");
  }
}
