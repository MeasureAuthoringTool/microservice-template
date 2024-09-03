package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import gov.cms.madie.models.measure.Measure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import cms.gov.madie.measure.exceptions.InvalidGroupException;
import gov.cms.madie.models.measure.Group;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service(ServiceConstants.QICORE_VALIDATOR)
public class QiCoreModelValidator extends ModelValidator {

  // Groups no longer get validated based on association. Can have 0 to many
  @Override
  public void validateGroupAssociations(Group group) {
  }

  @Override
  public void validateGroups(Measure measure) {
    if (CollectionUtils.isEmpty(measure.getGroups())) {
      throw new InvalidResourceStateException(
          "Measure", measure.getId(), "since there is no population criteria on the measure.");
    }
    if (measure.getGroups().stream()
        .anyMatch(g -> CollectionUtils.isEmpty(g.getMeasureGroupTypes()))) {
      throw new InvalidResourceStateException(
          "Measure",
          measure.getId(),
          "since there is at least one Population Criteria with no type.");
    }
  }
}
