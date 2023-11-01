package cms.gov.madie.measure.services;

import org.springframework.stereotype.Service;

import cms.gov.madie.measure.exceptions.InvalidGroupException;
import gov.cms.madie.models.measure.Group;

@Service
public class QicoreModelValidator implements ModelValidator {

  @Override
  public void validateGroupAssociations(String model, Group group) {

    boolean isNotAssociated;

    isNotAssociated =
        group.getStratifications().stream().anyMatch(map -> map.getAssociation() == null);

    if (isNotAssociated) {
      throw new InvalidGroupException(
          "QI-Core group stratifications should be associated to a valid population type.");
    }
  }
}
