package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.InvalidGroupException;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.QdmMeasure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service(ServiceConstants.QDM_VALIDATOR)
public class QdmModelValidator extends ModelValidator {

  @Override
  public void validateGroupAssociations(Group group) {
    boolean isAssociated;

    isAssociated =
        group.getStratifications().stream().anyMatch(map -> map.getAssociation() != null);

    if (isAssociated) {
      throw new InvalidGroupException("QDM group stratifications cannot be associated.");
    }
  }

  @Override
  public void validateGroups(Measure measure) {
    // TODO: this is just to start with. implement all the required validations on QDM groups
    if (CollectionUtils.isEmpty(measure.getGroups())) {
      throw new InvalidResourceStateException(
          "Measure", measure.getId(), "since there is no population criteria on the measure.");
    }
    QdmMeasure qdmMeasure = (QdmMeasure) measure;
    if (CollectionUtils.isEmpty(qdmMeasure.getBaseConfigurationTypes())) {
      throw new InvalidResourceStateException(
          "Measure", measure.getId(), "since there are no measure types for the measure.");
    }
  }
}
