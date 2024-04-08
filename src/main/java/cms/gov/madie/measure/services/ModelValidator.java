package cms.gov.madie.measure.services;

import java.util.stream.Collectors;

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
}
