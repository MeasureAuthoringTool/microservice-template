package cms.gov.madie.measure.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import cms.gov.madie.measure.exceptions.InvalidGroupException;
import gov.cms.madie.models.measure.Group;

@Slf4j
@Service(ServiceConstants.QDM_VALIDATOR)
public class QdmModelValidator implements ModelValidator {

  @Override
  public void validateGroupAssociations(Group group) {
    boolean isAssociated;

    isAssociated =
        group.getStratifications().stream().anyMatch(map -> map.getAssociation() != null);

    if (isAssociated) {
      throw new InvalidGroupException("QDM group stratifications cannot be associated.");
    }
  }
}
