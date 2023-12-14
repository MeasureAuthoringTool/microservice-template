package cms.gov.madie.measure.factories;

import cms.gov.madie.measure.exceptions.UnsupportedTypeException;
import cms.gov.madie.measure.services.ModelValidator;
import gov.cms.madie.models.common.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ModelValidatorFactory {

  private final Map<String, ModelValidator> modelValidatorMap;

  public ModelValidatorFactory(Map<String, ModelValidator> modelValidatorMap) {
    this.modelValidatorMap = modelValidatorMap;
  }

  public ModelValidator getModelValidator(ModelType model) {
    final String key = model.getShortValue() + "ModelValidator";
    if (!modelValidatorMap.containsKey(key)) {
      throw new UnsupportedTypeException(this.getClass().getName(), model.toString());
    }
    return modelValidatorMap.get(key);
  }
}
