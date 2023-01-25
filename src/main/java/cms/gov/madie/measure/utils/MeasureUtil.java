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
    Measure.MeasureBuilder measureBuilder = measure.toBuilder();
    if (elmJson == null && groupsExistWithPopulations) {
      // Measure has groups with populations with definitions, but ELM JSON is missing
      measureBuilder =
          measureBuilder
              .cqlErrors(true)
              .error(MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES)
              .error(MeasureErrorType.MISSING_ELM);
    } else if (elmJson != null && groupsExistWithPopulations) {
      if (measure.getGroups().stream()
          .anyMatch(group -> !isGroupReturnTypesValid(group, elmJson))) {
        measureBuilder =
            measureBuilder.error(MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES);
      } else {
        measureBuilder =
            measureBuilder
                .clearErrors()
                .errors(
                    removeError(
                        measure.getErrors(),
                        MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES));
      }
    } else {
      measureBuilder =
          measureBuilder
              .clearErrors()
              .errors(
                  removeError(
                      measure.getErrors(), MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES));
    }
    return measureBuilder.build();
  }

  /**
   * Functional method to remove an error from a set. If the errors set is not null and not empty,
   * and if the error is not null a new set will be returned with the error removed (if found).
   *
   * @param errors
   * @param error
   * @return
   */
  public Set<MeasureErrorType> removeError(Set<MeasureErrorType> errors, MeasureErrorType error) {
    if (errors == null || errors.isEmpty() || error == null) {
      return errors;
    }
    return errors.stream().filter(e -> !error.equals(e)).collect(Collectors.toSet());
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
    if (original == null && updated == null) {
      return false;
    } else if (original == null || updated == null) {
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
