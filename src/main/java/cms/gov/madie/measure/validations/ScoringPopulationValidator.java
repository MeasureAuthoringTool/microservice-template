package cms.gov.madie.measure.validations;

import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madiejavamodels.measure.Group;
import gov.cms.madiejavamodels.measure.Measure;
import gov.cms.madiejavamodels.measure.MeasurePopulation;
import gov.cms.madiejavamodels.measure.MeasureScoring;
import gov.cms.madiejavamodels.measure.TestCaseGroupPopulation;
import gov.cms.madiejavamodels.measure.TestCasePopulationValue;
import gov.cms.madiejavamodels.validators.ValidScoringPopulation;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ScoringPopulationValidator
    implements ConstraintValidator<ValidScoringPopulation, TestCaseGroupPopulation> {

  @Autowired MeasureRepository measureRepository;

  /**
   * Validates based on measureGroupPopulations If testCaseGroupPopulations contains all
   * measureGroupPopulations return true
   *
   * @param testCaseGroupPopulation
   * @param context
   * @return
   */
  @Override
  public boolean isValid(
      TestCaseGroupPopulation testCaseGroupPopulation, ConstraintValidatorContext context) {
    if (testCaseGroupPopulation == null) {
      return true;
    }
    if (testCaseGroupPopulation.getScoring() == null
        || testCaseGroupPopulation.getScoring().trim().isEmpty()) {
      return false;
    }
    MeasureScoring scoring = MeasureScoring.valueOfText(testCaseGroupPopulation.getScoring());
    List<TestCasePopulationValue> populationValues = testCaseGroupPopulation.getPopulationValues();

    if (scoring == null || populationValues == null || populationValues.isEmpty()) {
      return false;
    }

    List<MeasurePopulation> receivedPopulations =
        populationValues.stream()
            .map(TestCasePopulationValue::getName)
            .distinct()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    Optional<Measure> measure =
        measureRepository.findGroupById(testCaseGroupPopulation.getGroupId());

    if (measure.isPresent()) {
      Optional<Group> measureGroup =
          measure.get().getGroups().stream()
              .filter(group -> group.getId().equals(testCaseGroupPopulation.getGroupId()))
              .findFirst();
      if (measureGroup.isPresent()) {
        Set<MeasurePopulation> measurePopulationSet = measureGroup.get().getPopulation().keySet();
        return receivedPopulations.size() == measurePopulationSet.size()
            && measurePopulationSet.containsAll(receivedPopulations);
      }
    }
    return false;
  }
}
