package cms.gov.madie.measure.services;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;

public abstract class ModelValidator {
  abstract void validateGroupAssociations(Group group);

  abstract void validateGroups(Measure measure);

  public void validateCqlErrors(Measure measure) {
    if (!CollectionUtils.isEmpty(measure.getErrors())) {
      String errors =
          measure.getErrors().stream()
              .map((error) -> error.name())
              .collect(Collectors.joining("\n"));
      throw new InvalidResourceStateException("Measure", measure.getId(), errors);
    }
    if (measure.isCqlErrors()) {
      throw new InvalidResourceStateException(
          "Measure", measure.getId(), "since there is CQL error");
    }
  }

  public void validateMetadata(Measure measure) {
    if (measure.getMeasureMetaData() != null) {
      if (CollectionUtils.isEmpty(measure.getMeasureMetaData().getDevelopers())) {
        throw new InvalidResourceStateException(
            "Measure", measure.getId(), "since there are no associated developers in metadata.");
      } else if (measure.getMeasureMetaData().getSteward() == null) {
        throw new InvalidResourceStateException(
            "Measure", measure.getId(), "since there is no associated steward in metadata.");
      } else if (StringUtils.isBlank(measure.getMeasureMetaData().getDescription())) {
        throw new InvalidResourceStateException(
            "Measure", measure.getId(), "since there is no description in metadata.");
      }
    }
  }
}
