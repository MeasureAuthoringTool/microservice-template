package cms.gov.madie.measure.validations;

import cms.gov.madie.measure.models.MeasurePopulation;
import cms.gov.madie.measure.models.MeasureScoring;
import cms.gov.madie.measure.models.TestCaseGroupPopulation;
import cms.gov.madie.measure.models.TestCasePopulationValue;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.validation.ConstraintValidatorContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoringPopulationValidatorTest {

  @Mock private ConstraintValidatorContext validatorContext;

  private ScoringPopulationValidator validator = new ScoringPopulationValidator();

  @Test
  public void testValidatorReturnsTrueForNull() {
    boolean output = validator.isValid(null, validatorContext);
    assertTrue(output);
  }

  @Test
  public void testValidatorReturnsFalseForMissingScoring() {
    TestCaseGroupPopulation groupPopulation = new TestCaseGroupPopulation();
    groupPopulation.setScoring(null);
    groupPopulation.setPopulationValues(
        List.of(
            TestCasePopulationValue.builder().name(MeasurePopulation.INITIAL_POPULATION).build()));
    boolean output = validator.isValid(groupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForNullPopulationsList() {
    TestCaseGroupPopulation groupPopulation = new TestCaseGroupPopulation();
    groupPopulation.setScoring(MeasureScoring.COHORT.toString());
    groupPopulation.setPopulationValues(null);
    boolean output = validator.isValid(groupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForEmptyPopulationsList() {
    TestCaseGroupPopulation groupPopulation = new TestCaseGroupPopulation();
    groupPopulation.setScoring(MeasureScoring.COHORT.toString());
    groupPopulation.setPopulationValues(List.of());
    boolean output = validator.isValid(groupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForMissingPopulation() {
    TestCaseGroupPopulation groupPopulation = new TestCaseGroupPopulation();
    groupPopulation.setScoring(MeasureScoring.COHORT.toString());
    groupPopulation.setPopulationValues(List.of(TestCasePopulationValue.builder().build()));
    boolean output = validator.isValid(groupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForIncorrectPopulation() {
    TestCaseGroupPopulation groupPopulation = new TestCaseGroupPopulation();
    groupPopulation.setScoring(MeasureScoring.COHORT.toString());
    groupPopulation.setPopulationValues(
        List.of(TestCasePopulationValue.builder().name(MeasurePopulation.DENOMINATOR).build()));
    boolean output = validator.isValid(groupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsTrueForCorrectPopulation() {
    TestCaseGroupPopulation groupPopulation = new TestCaseGroupPopulation();
    groupPopulation.setScoring(MeasureScoring.COHORT.toString());
    groupPopulation.setPopulationValues(
        List.of(
            TestCasePopulationValue.builder().name(MeasurePopulation.INITIAL_POPULATION).build()));
    boolean output = validator.isValid(groupPopulation, validatorContext);
    assertTrue(output);
  }
}
