package cms.gov.madie.measure.validations;

import cms.gov.madie.measure.exceptions.InvalidMeasureObservationException;
import cms.gov.madie.measure.exceptions.InvalidReturnTypeException;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class CqlObservationFunctionValidatorTest implements ResourceUtil {

  private CqlObservationFunctionValidator validator = new CqlObservationFunctionValidator();

  @Test
  public void testValidateObservationFunctionsThrowsInvalidMeasureObservationException() {
    String elmJson = getData("/test_elm_no_observation.json");
    MeasureObservation observation =
        MeasureObservation.builder().definition("boolFunc").aggregateMethod("Count").build();
    Group group =
        Group.builder()
            .scoring("Continuous Variable")
            .populationBasis("boolean")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .groupDescription("Description")
            .measureObservations(List.of(observation))
            .build();

    assertThrows(
        InvalidMeasureObservationException.class,
        () -> validator.validateObservationFunctions(group, elmJson),
        "Measure CQL does not have observation definition");
  }

  @Test
  public void testValidateObservationFunctionsSuccess() {
    String elmJson = getData("/test_elm_with_observation.json");
    MeasureObservation observation = MeasureObservation.builder().aggregateMethod("Count").build();
    Group group =
        Group.builder()
            .scoring("Continuous Variable")
            .populationBasis("boolean")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .groupDescription("Description")
            .measureObservations(List.of(observation))
            .build();

    assertDoesNotThrow(() -> validator.validateObservationFunctions(group, elmJson));
  }

  @Test
  public void testValidateObservationFunctionsSuccessWithNoCQLNoObservation() {
    Group group =
        Group.builder()
            .scoring("Continuous Variable")
            .populationBasis("boolean")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .groupDescription("Description")
            .measureObservations(null)
            .build();
    assertDoesNotThrow(() -> validator.validateObservationFunctions(group, ""));
  }

  @Test
  public void
      testValidateObservationFunctionsThrowsInvalidReturnTypeExceptionWithBooleanGroupPopulationBasis()
          throws JsonProcessingException {
    String elmJson = getData("/test_elm.json");
    MeasureObservation observation =
        MeasureObservation.builder().definition("boolFunc").aggregateMethod("Count").build();
    Group group =
        Group.builder()
            .scoring("Continuous Variable")
            .populationBasis("boolean")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .groupDescription("Description")
            .measureObservations(List.of(observation))
            .build();

    assertThrows(
        InvalidReturnTypeException.class,
        () -> validator.validateObservationFunctions(group, elmJson),
        "Selected observation function boolFunc can not have parameters");
  }

  @Test
  public void testValidateObservationFunctionsThrowsInvalidReturnTypeException()
      throws JsonProcessingException {
    String elmJson = getData("/test_elm.json");
    MeasureObservation observation =
        MeasureObservation.builder().definition("boolFunc").aggregateMethod("Count").build();
    Group group =
        Group.builder()
            .scoring("Continuous Variable")
            .populationBasis("non-boolean")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .groupDescription("Description")
            .measureObservations(List.of(observation))
            .build();

    assertThrows(
        InvalidReturnTypeException.class,
        () -> validator.validateObservationFunctions(group, elmJson),
        "Selected observation function boolFunc can not have parameters");
  }
}
