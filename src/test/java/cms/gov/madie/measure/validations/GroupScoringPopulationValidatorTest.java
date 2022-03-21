package cms.gov.madie.measure.validations;

import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.models.MeasurePopulation;
import cms.gov.madie.measure.models.MeasureScoring;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import javax.validation.ConstraintValidatorContext;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GroupScoringPopulationValidatorTest {

  @Mock private ConstraintValidatorContext validatorContext;

  private GroupScoringPopulationValidator validator = new GroupScoringPopulationValidator();

  @Test
  public void testValidatorReturnsTrueForNullGroup() {
    boolean output = validator.isValid(null, validatorContext);
    assertTrue(output);
  }

  @Test
  public void testValidatorReturnsFalseForNullScoring() {
    Group group = Group.builder().scoring(null).build();
    boolean output = validator.isValid(group, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForEmptyScoring() {
    Group group = Group.builder().scoring("").build();
    boolean output = validator.isValid(group, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForWhitespaceScoring() {
    Group group = Group.builder().scoring("   ").build();
    boolean output = validator.isValid(group, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForNullPopulation() {
    Group group =
        Group.builder().scoring(MeasureScoring.COHORT.toString()).population(null).build();
    boolean output = validator.isValid(group, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForEmptyPopulation() {
    Group group =
        Group.builder().scoring(MeasureScoring.COHORT.toString()).population(Map.of()).build();
    boolean output = validator.isValid(group, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForInvalidScoring() {
    Map<MeasurePopulation, String> populations =
        Map.of(MeasurePopulation.INITIAL_POPULATION, "Advanced Illness");
    Group group = Group.builder().scoring("Invalid").population(populations).build();
    boolean output = validator.isValid(group, validatorContext);
    assertFalse(output);
  }

  @ParameterizedTest
  @MethodSource("providePopulationCombinationsForGroupScoring")
  void isGroupScoringPopulationCombinationValid(Group group, boolean expected) {
    assertEquals(expected, validator.isValid(group, validatorContext));
  }

  // Note: cannot pass null arguments using a generator like this
  private static Stream<Arguments> providePopulationCombinationsForGroupScoring() {
    return Stream.of(
        // correct Cohort population combination
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.COHORT.toString())
                .population(Map.of(MeasurePopulation.INITIAL_POPULATION, "Advanced Illness"))
                .build(),
            true),
        // correct Cohort initial population but with an extra, invalid population
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.COHORT.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "",
                        MeasurePopulation.MEASURE_POPULATION,
                        "Something"))
                .build(),
            false),
        // invalid, Cohort population
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.COHORT.toString())
                .population(Map.of(MeasurePopulation.INITIAL_POPULATION, ""))
                .build(),
            false),
        // valid Proportion definitions with no optional populations
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.PROPORTION.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.NUMERATOR,
                        "pop2",
                        MeasurePopulation.DENOMINATOR,
                        "pop3"))
                .build(),
            true),
        // valid Proportion definitions with all optional populations
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.PROPORTION.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.NUMERATOR,
                        "pop2",
                        MeasurePopulation.NUMERATOR_EXCLUSION,
                        "pop3",
                        MeasurePopulation.DENOMINATOR,
                        "pop4",
                        MeasurePopulation.DENOMINATOR_EXCLUSION,
                        "pop5",
                        MeasurePopulation.DENOMINATOR_EXCEPTION,
                        "pop6"))
                .build(),
            true),
        // invalid Proportion definitions with missing value for optional population
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.PROPORTION.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.NUMERATOR,
                        "pop2",
                        MeasurePopulation.NUMERATOR_EXCLUSION,
                        "pop3",
                        MeasurePopulation.DENOMINATOR,
                        "pop4",
                        MeasurePopulation.DENOMINATOR_EXCLUSION,
                        "",
                        MeasurePopulation.DENOMINATOR_EXCEPTION,
                        "pop6"))
                .build(),
            false),
        // invalid Proportion definitions with missing value for required population
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.PROPORTION.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.NUMERATOR,
                        "",
                        MeasurePopulation.NUMERATOR_EXCLUSION,
                        "pop3",
                        MeasurePopulation.DENOMINATOR,
                        "pop4",
                        MeasurePopulation.DENOMINATOR_EXCLUSION,
                        "pop5",
                        MeasurePopulation.DENOMINATOR_EXCEPTION,
                        "pop6"))
                .build(),
            false),
        // invalid Proportion definitions with all required populations but extra population
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.PROPORTION.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.NUMERATOR,
                        "pop2",
                        MeasurePopulation.DENOMINATOR,
                        "pop4",
                        MeasurePopulation.DENOMINATOR_EXCLUSION,
                        "pop5",
                        MeasurePopulation.DENOMINATOR_EXCEPTION,
                        "pop6",
                        MeasurePopulation.MEASURE_POPULATION,
                        "pop9"))
                .build(),
            false),
        // invalid CV definitions with mismatched definitions
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.NUMERATOR,
                        "pop2",
                        MeasurePopulation.DENOMINATOR,
                        "pop4"))
                .build(),
            false),
        // invalid CV definitions missing required populations
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
                .population(Map.of(MeasurePopulation.INITIAL_POPULATION, "pop1"))
                .build(),
            false),
        // invalid CV definitions with mismatched definitions
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.NUMERATOR,
                        "pop2",
                        MeasurePopulation.DENOMINATOR,
                        "pop4"))
                .build(),
            false),
        // valid CV definitions with required populations
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.MEASURE_POPULATION,
                        "pop2"))
                .build(),
            true),
        // valid CV definitions with required and optional populations
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.MEASURE_POPULATION,
                        "pop2",
                        MeasurePopulation.MEASURE_POPULATION_EXCLUSION,
                        "pop3"))
                .build(),
            true),
        // valid Cohort definitions with required population
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.COHORT.toString())
                .population(Map.of(MeasurePopulation.INITIAL_POPULATION, "pop1"))
                .build(),
            true),
        // invalid Cohort definitions with required population plus extra population
        Arguments.of(
            Group.builder()
                .scoring(MeasureScoring.COHORT.toString())
                .population(
                    Map.of(
                        MeasurePopulation.INITIAL_POPULATION,
                        "pop1",
                        MeasurePopulation.NUMERATOR,
                        "pop2"))
                .build(),
            false));
  }
}
