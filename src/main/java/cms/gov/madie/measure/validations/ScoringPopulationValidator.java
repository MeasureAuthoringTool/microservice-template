package cms.gov.madie.measure.validations;

import cms.gov.madie.measure.models.MeasurePopulation;
import cms.gov.madie.measure.models.MeasureScoring;
import cms.gov.madie.measure.models.TestCaseGroupPopulation;
import cms.gov.madie.measure.models.TestCasePopulationValue;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cms.gov.madie.measure.utils.ScoringPopulationDefinition.SCORING_POPULATION_MAP;

public class ScoringPopulationValidator implements ConstraintValidator<ValidScoringPopulation, TestCaseGroupPopulation> {

  @Override
  public boolean isValid(TestCaseGroupPopulation testCaseGroupPopulation, ConstraintValidatorContext context) {
    if (testCaseGroupPopulation == null) {
      return true;
    }

    MeasureScoring scoring = testCaseGroupPopulation.getScoring();
    List<TestCasePopulationValue> populationValues = testCaseGroupPopulation.getPopulationValues();
    if (scoring == null || populationValues == null || populationValues.isEmpty()) {
      return false;
    }

    List<MeasurePopulation> requiredPopulations = SCORING_POPULATION_MAP.get(scoring);
    List<MeasurePopulation> receivedPopulations = populationValues.stream().map(TestCasePopulationValue::getName)
        .distinct().filter(Objects::nonNull).collect(Collectors.toList());
    return receivedPopulations.size() == requiredPopulations.size() && requiredPopulations.containsAll(receivedPopulations);
  }

}
