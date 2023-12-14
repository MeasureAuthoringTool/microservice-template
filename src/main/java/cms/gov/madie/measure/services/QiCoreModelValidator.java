package cms.gov.madie.measure.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import cms.gov.madie.measure.exceptions.InvalidGroupException;
import gov.cms.madie.models.measure.Group;

@Slf4j
@Service(ServiceConstants.QICORE_VALIDATOR)
public class QiCoreModelValidator implements ModelValidator {

  @Override
  public void validateGroupAssociations(Group group) {
    boolean isNotAssociated;

    isNotAssociated =
        group.getStratifications().stream().anyMatch(map -> map.getAssociation() == null);

    if (isNotAssociated) {
      throw new InvalidGroupException(
          "QI-Core group stratifications should be associated to a valid population type.");
    }
  }
}
