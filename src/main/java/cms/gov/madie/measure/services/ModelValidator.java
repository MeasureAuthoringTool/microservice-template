package cms.gov.madie.measure.services;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;

public interface ModelValidator {
  void validateGroupAssociations(Group group);

  void validateGroups(Measure measure);
}
