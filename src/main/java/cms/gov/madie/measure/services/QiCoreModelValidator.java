package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.InvalidGroupException;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import org.apache.commons.lang3.StringUtils;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Stratification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import gov.cms.madie.models.measure.Group;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service(ServiceConstants.QICORE_VALIDATOR)
public class QiCoreModelValidator extends ModelValidator {

  // Groups no longer get validated based on association. Can have 0 to many
  @Override
  public void validateGroupAssociations(Group group) {
    if (group.getStratifications() != null) {
      List<Stratification> list =
          group.getStratifications().stream()
              .filter(
                  test ->
                      (StringUtils.isBlank(test.getCqlDefinition()))
                          && (!StringUtils.isBlank(test.getDescription())
                              || !CollectionUtils.isEmpty(test.getAssociations())))
              .toList();
      if (!CollectionUtils.isEmpty(list)) {
        throw new InvalidGroupException("QDM group stratifications cannot be associated.");
      }
    }
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

    if (measure.getMeasureMetaData() != null && measure.getMeasureMetaData().isDraft()) {
      measure
          .getGroups()
          .forEach(
              group -> {
                if (StringUtils.isBlank(group.getImprovementNotation())) {
                  throw new InvalidResourceStateException(
                      "Measure",
                      measure.getId(),
                      "since there is at least one Population Criteria "
                          + "with no improvement notation.");
                }
              });
    }
  }
}
