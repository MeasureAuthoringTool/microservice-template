package cms.gov.madie.measure.services;

import gov.cms.madie.models.common.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ModelValidatorFactory {

  private final Map<String, ModelValidator> modelValidatorMap;

  public ModelValidatorFactory(Map<String, ModelValidator> modelValidatorMap) {
    log.info("model validator map: {}", modelValidatorMap);
    this.modelValidatorMap = modelValidatorMap;
  }
  public ModelValidator getModelValidator(ModelType model) {
    return modelValidatorMap.get(model.getShortValue() + "ModelValidator");
  }
}
