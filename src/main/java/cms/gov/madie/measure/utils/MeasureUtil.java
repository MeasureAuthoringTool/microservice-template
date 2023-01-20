package cms.gov.madie.measure.utils;

import cms.gov.madie.measure.validations.CqlDefinitionReturnTypeValidator;
import cms.gov.madie.measure.validations.CqlObservationFunctionValidator;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureErrorType;
import gov.cms.madie.models.measure.Population;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class MeasureUtil {
  private final CqlDefinitionReturnTypeValidator cqlDefinitionReturnTypeValidator;
  private final CqlObservationFunctionValidator cqlObservationFunctionValidator;

  /**
   * Validates measure group population define return types and observation function return types
   * compared to the population basis of the group. If any mismatches are found, an error is added
   * to the list of errors on the measure object
   *
   * @param measure
   * @return
   */
  public Measure validateAllMeasureGroupReturnTypes(Measure measure) {
    if (measure == null) {
      return null;
    }

    final String elmJson = measure.getElmJson();
    boolean groupsExistWithPopulations = isGroupsExistWithPopulations(measure);
    Measure outputMeasure = measure;
    if (elmJson == null && groupsExistWithPopulations) {
      // Measure has groups with populations with definitions, but ELM JSON is missing
      measure.setCqlErrors(true);
      outputMeasure =
          measure
              .toBuilder()
              .error(MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES)
              .error(MeasureErrorType.MISSING_ELM)
              .build();
    } else if (elmJson != null && groupsExistWithPopulations) {
      if (measure.getGroups().stream()
          .anyMatch(group -> !isGroupReturnTypesValid(group, elmJson))) {
        log.info(
            "Mismatch exists between CQL return types and Population Criteria definition types!");
        outputMeasure =
            measure
                .toBuilder()
                .error(MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES)
                .build();
      } else if (measure.getErrors() != null
          && measure.getErrors().contains(MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES)) {
        log.info("No CQL return type mismatch! Woo!");
        Set<MeasureErrorType> updatedErrors =
            measure.getErrors().stream()
                .filter(e -> !MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES.equals(e))
                .collect(Collectors.toSet());
        log.info("updated errors; {}", updatedErrors);
        outputMeasure = measure.toBuilder().clearErrors().errors(updatedErrors).build();
      }
    }
    return outputMeasure;
  }

  public boolean isGroupsExistWithPopulations(Measure measure) {
    if (measure == null || measure.getGroups() == null || measure.getGroups().isEmpty()) {
      return false;
    }
    return measure.getGroups().stream()
        .anyMatch(
            (group) -> {
              final List<Population> populations = group.getPopulations();
              if (populations == null) {
                return false;
              }
              return populations.stream()
                  .anyMatch(population -> StringUtils.isNotBlank(population.getDefinition()));
            });
  }

  public boolean isGroupReturnTypesValid(final Group group, final String elmJson) {
    try {
      cqlDefinitionReturnTypeValidator.validateCqlDefinitionReturnTypes(group, elmJson);
    } catch (Exception ex) {
      // Either no return types were found in ELM, or return type mismatch exists
      log.error("An error occurred while validating population return types", ex);
      return false;
    }
    try {
      cqlObservationFunctionValidator.validateObservationFunctions(group, elmJson);
    } catch (Exception ex) {
      // Either no return types were found in ELM, or return type mismatch exists
      log.error("An error occurred while validating observation return types", ex);
      return false;
    }
    return true;
  }

  public boolean isMeasureCqlChanged(final Measure original, final Measure updated) {
    if ((original == null || updated == null) && original != updated) {
      return true;
    }
    return !StringUtils.equals(original.getCql(), updated.getCql());
  }

  public boolean isCqlLibraryNameChanged(Measure measure, Measure persistedMeasure) {
    return !Objects.equals(persistedMeasure.getCqlLibraryName(), measure.getCqlLibraryName());
  }

  public boolean isMeasurementPeriodChanged(Measure measure, Measure persistedMeasure) {
    return !Objects.equals(
            persistedMeasure.getMeasurementPeriodStart(), measure.getMeasurementPeriodStart())
        || !Objects.equals(
            persistedMeasure.getMeasurementPeriodEnd(), measure.getMeasurementPeriodEnd());
  }
}
