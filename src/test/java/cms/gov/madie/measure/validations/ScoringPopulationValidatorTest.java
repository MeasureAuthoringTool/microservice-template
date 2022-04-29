package cms.gov.madie.measure.validations;

import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.MeasurePopulation;
import cms.gov.madie.measure.models.MeasureScoring;
import cms.gov.madie.measure.models.TestCaseGroupPopulation;
import cms.gov.madie.measure.models.TestCasePopulationValue;
import cms.gov.madie.measure.repositories.MeasureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ScoringPopulationValidatorTest {

  @Mock private ConstraintValidatorContext validatorContext;
  @Mock private MeasureRepository measureRepository;
  private final ScoringPopulationValidator validator = new ScoringPopulationValidator();

  private Measure measure;

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(validator, "measureRepository", measureRepository);

    Group group1 =
        Group.builder()
            .id("GroupId")
            .scoring("Cohort")
            .population(Map.of(MeasurePopulation.INITIAL_POPULATION, "Initial Population"))
            .groupDescription("Description")
            .build();
    List<Group> groups = new ArrayList<>();
    groups.add(group1);
    measure =
        Measure.builder()
            .active(true)
            .id("xyz-p13r-13ert")
            .cql("test cql")
            .measureScoring("Cohort")
            .measureSetId("IDIDID")
            .measureName("MSR01")
            .version("0.001")
            .groups(groups)
            .createdAt(Instant.now())
            .createdBy("test user")
            .lastModifiedAt(Instant.now())
            .lastModifiedBy("test user")
            .build();
  }

  @Test
  public void testValidatorReturnsTrueForNull() {
    boolean output = validator.isValid(null, validatorContext);
    assertTrue(output);
  }

  @Test
  public void testValidatorReturnsFalseForMissingScoring() {
    var testCaseGroupPopulation =
        TestCaseGroupPopulation.builder()
            .scoring(null)
            .populationValues(
                List.of(
                    TestCasePopulationValue.builder()
                        .name(MeasurePopulation.INITIAL_POPULATION)
                        .build()))
            .build();
    boolean output = validator.isValid(testCaseGroupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForNullPopulationsList() {
    var testCaseGroupPopulation =
        TestCaseGroupPopulation.builder()
            .populationValues(null)
            .scoring(MeasureScoring.COHORT.toString())
            .build();
    boolean output = validator.isValid(testCaseGroupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForEmptyPopulationsList() {
    var testCaseGroupPopulation =
        TestCaseGroupPopulation.builder()
            .populationValues(List.of())
            .scoring(MeasureScoring.COHORT.toString())
            .build();
    boolean output = validator.isValid(testCaseGroupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForMissingPopulation() {
    when(measureRepository.findGroupById(anyString())).thenReturn(Optional.of(measure));
    var testCaseGroupPopulation =
        TestCaseGroupPopulation.builder()
            .groupId("GroupId")
            .populationValues(List.of(TestCasePopulationValue.builder().build()))
            .scoring(MeasureScoring.COHORT.toString())
            .build();
    boolean output = validator.isValid(testCaseGroupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsFalseForIncorrectPopulation() {
    when(measureRepository.findGroupById(anyString())).thenReturn(Optional.of(measure));
    var testCaseGroupPopulation =
        TestCaseGroupPopulation.builder()
            .groupId("GroupId")
            .populationValues(
                List.of(
                    TestCasePopulationValue.builder().name(MeasurePopulation.DENOMINATOR).build()))
            .scoring(MeasureScoring.COHORT.toString())
            .build();
    boolean output = validator.isValid(testCaseGroupPopulation, validatorContext);
    assertFalse(output);
  }

  @Test
  public void testValidatorReturnsTrueForCorrectPopulation() {
    when(measureRepository.findGroupById(anyString())).thenReturn(Optional.of(measure));
    var testCaseGroupPopulation =
        TestCaseGroupPopulation.builder()
            .groupId("GroupId")
            .populationValues(
                List.of(
                    TestCasePopulationValue.builder()
                        .name(MeasurePopulation.INITIAL_POPULATION)
                        .build()))
            .scoring(MeasureScoring.COHORT.toString())
            .build();
    boolean output = validator.isValid(testCaseGroupPopulation, validatorContext);
    assertTrue(output);
  }
}
