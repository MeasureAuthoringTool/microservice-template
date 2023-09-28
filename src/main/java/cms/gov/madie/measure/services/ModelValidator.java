package cms.gov.madie.measure.services;

import gov.cms.madie.models.measure.Group;

public interface ModelValidator {
  public void validateGroupAssociations(String model, Group group);
}
