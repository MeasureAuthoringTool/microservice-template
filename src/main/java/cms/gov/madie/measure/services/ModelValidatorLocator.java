package cms.gov.madie.measure.services;

import org.springframework.beans.BeansException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ModelValidatorLocator implements ApplicationContextAware {
  private ApplicationContext ctx;

  public ModelValidator get(String model) {
    return ctx.getBean(model + "ModelValidator", ModelValidator.class);
  }

  @Override
  public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    this.ctx = ctx;
  }
}
