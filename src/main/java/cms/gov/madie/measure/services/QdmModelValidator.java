package cms.gov.madie.measure.services;

import org.springframework.stereotype.Service;

import cms.gov.madie.measure.exceptions.InvalidGroupException;
import gov.cms.madie.models.measure.Group;

@Service
public class QdmModelValidator implements ModelValidator {

  @Override
  public void validateGroupAssociations(String model, Group group) {

    boolean isAssociated;

    isAssociated =
        group.getStratifications().stream().anyMatch(map -> map.getAssociation() != null);

    if (isAssociated) {
      throw new InvalidGroupException("QDM group stratifications cannot be associated.");
    }
  }
}
