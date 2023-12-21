package cms.gov.madie.measure.utils;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import gov.cms.madie.models.measure.TestCaseStratificationValue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class TestCaseServiceUtil {

  private static final List<PopulationType> EXPECTED_VALUE_ORDER =
      List.of(
          PopulationType.INITIAL_POPULATION,
          PopulationType.MEASURE_POPULATION,
          PopulationType.MEASURE_POPULATION_OBSERVATION,
          PopulationType.MEASURE_OBSERVATION,
          PopulationType.MEASURE_POPULATION_EXCLUSION,
          PopulationType.DENOMINATOR,
          PopulationType.DENOMINATOR_OBSERVATION,
          PopulationType.DENOMINATOR_EXCLUSION,
          PopulationType.NUMERATOR,
          PopulationType.NUMERATOR_OBSERVATION,
          PopulationType.NUMERATOR_EXCLUSION,
          PopulationType.DENOMINATOR_EXCEPTION);

  /**
   * Filter out populations that are not associated with a definition.
   *
   * @param originalGroups Target measure's Population Criteria.
   * @return New List with populations associated with definitions.
   */
  public List<Group> getGroupsWithValidPopulations(List<Group> originalGroups) {
    if (isEmpty(originalGroups)) {
      return null;
    }
    List<Group> groups = new ArrayList<>(originalGroups);

    for (Group group : groups) {
      if (isNotEmpty(group.getPopulations())) {
        List<Population> pops = new ArrayList<>(group.getPopulations());
        pops.removeIf(pop -> StringUtils.isBlank(pop.getDefinition()));
        group.setPopulations(pops);
      }
    }
    return groups;
  }

  // match criteria groups from MeasureReport in imported json file
  public boolean matchCriteriaGroups(
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      List<Group> groups,
      TestCase newTestCase) {
    boolean isValid = true;
    List<TestCaseGroupPopulation> finalGroupPopulations;
    List<TestCaseGroupPopulation> nonObservationGroupPopulations =
        getNonObservationGroupPopulations(testCaseGroupPopulations);

    // group size has to match
    if (!isEmpty(groups)
        && !isEmpty(nonObservationGroupPopulations)
        && groups.size() == nonObservationGroupPopulations.size()) {
      finalGroupPopulations = new ArrayList<>();
      for (int i = 0; i < groups.size(); i++) {
        Group group = groups.get(i);
        // group population size has to match
        if (!isEmpty(group.getPopulations())
            && !isEmpty(nonObservationGroupPopulations.get(i).getPopulationValues())
            && group.getPopulations().size()
                == nonObservationGroupPopulations.get(i).getPopulationValues().size()) {
          isValid =
              mapPopulationValues(
                  group,
                  nonObservationGroupPopulations,
                  i,
                  finalGroupPopulations,
                  newTestCase,
                  isValid,
                  testCaseGroupPopulations);

        } else {
          isValid = false;
        }
      }
    } else {
      isValid = false;
    }

    return isValid;
  }

  private TestCaseGroupPopulation assignTestCaseGroupPopulation(Group group) {
    return TestCaseGroupPopulation.builder()
        .groupId(group.getId())
        .scoring(group.getScoring())
        .populationBasis(group.getPopulationBasis())
        .build();
  }

  /**
   * This function modifies input parameters!
   *
   * @param group Existing Measure Population Criteria
   * @param nonObsPopulations Population Criteria members excluding observations.
   * @param measureGroupNumber
   * @param finalGroupPopulations Finalized list of Population Criteria populations.
   * @param newTestCase WIP Test Case from imported data
   * @param isValid Whether, excluding Observations, if the Measure Population Criteria matches the
   *     imported Population Criteria. True = matches, False, does not match.
   * @return isValid
   */
  private boolean mapPopulationValues(
      Group group,
      List<TestCaseGroupPopulation> nonObsPopulations,
      int measureGroupNumber,
      List<TestCaseGroupPopulation> finalGroupPopulations,
      TestCase newTestCase,
      boolean isValid,
      List<TestCaseGroupPopulation> allImportedPopulations) {
    TestCaseGroupPopulation groupPopulation = assignTestCaseGroupPopulation(group);
    List<TestCasePopulationValue> populationValues = new ArrayList<>();
    int matchedNumber = 0;
    final int groupPopulationCount = group.getPopulations().size();
    // map the non-observation population results based on type
    for (int groupPopulationIndex = 0;
        groupPopulationIndex < groupPopulationCount;
        groupPopulationIndex++) {
      Population population = group.getPopulations().get(groupPopulationIndex);
      matchedNumber =
          assignPopulationValues(
              population,
              nonObsPopulations,
              measureGroupNumber,
              groupPopulationIndex,
              matchedNumber,
              group,
              populationValues,
              groupPopulation);
    }
    if (matchedNumber == group.getPopulations().size()) {
      // if group has observations and some existed on test case, add them back in
      List<TestCasePopulationValue> observationPopVals =
          mapObservations(allImportedPopulations.get(measureGroupNumber), group);
      if (!isEmpty(observationPopVals)) {
        groupPopulation.getPopulationValues().addAll(observationPopVals);
        groupPopulation
            .getPopulationValues()
            .sort(
                Comparator.comparing(
                    TestCasePopulationValue::getName,
                    Comparator.comparingInt(EXPECTED_VALUE_ORDER::indexOf)));
      }
      finalGroupPopulations.add(groupPopulation);
      groupPopulation.setGroupId(group.getId());
      newTestCase.setGroupPopulations(finalGroupPopulations);
    } else {
      isValid = false;
    }

    return isValid;
  }

  private Set<PopulationType> getObservationTypesForGroup(Group group) {
    Set<PopulationType> types = new HashSet<>();
    if (group != null
        && !isEmpty(group.getPopulations())
        && !isEmpty(group.getMeasureObservations())) {
      for (MeasureObservation observation : group.getMeasureObservations()) {
        Optional<Population> refPopOpt =
            group.getPopulations().stream()
                .filter(p -> StringUtils.equals(p.getId(), observation.getCriteriaReference()))
                .findFirst();
        if (refPopOpt.isPresent()) {
          switch (refPopOpt.get().getName()) {
            case DENOMINATOR -> types.add(PopulationType.DENOMINATOR_OBSERVATION);
            case NUMERATOR -> types.add(PopulationType.NUMERATOR_OBSERVATION);
            case MEASURE_POPULATION -> types.add(PopulationType.MEASURE_POPULATION_OBSERVATION);
            default -> {}
          }
        }
      }
    }
    return types;
  }

  private List<TestCasePopulationValue> mapObservations(
      TestCaseGroupPopulation importedGroup, Group measrueGroup) {
    List<TestCasePopulationValue> observationPopVals = new ArrayList<>();
    Set<PopulationType> observationPopulationTypes = getObservationTypesForGroup(measrueGroup);
    if (!isEmpty(observationPopulationTypes) && !isEmpty(importedGroup.getPopulationValues())) {
      for (TestCasePopulationValue tcPopVal : importedGroup.getPopulationValues()) {
        if (observationPopulationTypes.contains(tcPopVal.getName())) {
          observationPopVals.add(tcPopVal);
        }
      }
    }
    return observationPopVals;
  }

  private int assignPopulationValues(
      Population population,
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      int measureGroupNumber,
      int groupPopulationIndex,
      int matchedNumber,
      Group group,
      List<TestCasePopulationValue> populationValues,
      TestCaseGroupPopulation groupPopulation) {
    if (population
        .getName()
        .toCode()
        .equalsIgnoreCase(
            testCaseGroupPopulations
                .get(measureGroupNumber)
                .getPopulationValues()
                .get(groupPopulationIndex)
                .getName()
                .toCode())) {
      matchedNumber++;

      TestCasePopulationValue populationValue =
          testCaseGroupPopulations
              .get(measureGroupNumber)
              .getPopulationValues()
              .get(groupPopulationIndex);
      populationValue.setId(population.getId());

      if (group.getPopulationBasis() != null
          && group.getPopulationBasis().equalsIgnoreCase("boolean")) {
        String originalValue = (String) populationValue.getExpected();
        if (originalValue.equalsIgnoreCase("1")) {
          populationValue.setExpected(Boolean.TRUE);
        } else {
          populationValue.setExpected(Boolean.FALSE);
        }
      }
      populationValues.add(populationValue);
      groupPopulation.setPopulationValues(populationValues);
      groupPopulation.setStratificationValues(
          testCaseGroupPopulations.get(measureGroupNumber).getStratificationValues());
    }
    return matchedNumber;
  }

  public List<TestCaseGroupPopulation> assignStratificationValuesQdm(
      List<TestCaseGroupPopulation> testCaseGroupPopulations, List<Group> measureGroups) {

    // Break up single list of pop values and strats into separate lists
    List<TestCaseGroupPopulation> populationCriteria =
        testCaseGroupPopulations.stream()
            .filter(
                group ->
                    isNotEmpty(group.getPopulationValues())
                        && isEmpty(group.getStratificationValues()))
            .toList();

    List<TestCaseStratificationValue> stratification =
        testCaseGroupPopulations.stream()
            .filter(group -> isNotEmpty(group.getStratificationValues()))
            // Assumes there cannot be more than 1 strat in each incoming expected value obj
            .map(group -> group.getStratificationValues().get(0))
            .toList();

    // Mismatch between target and import Stratification, don't set Strat expected values
    boolean measureHasStrats =
        measureGroups.stream().allMatch(group -> isNotEmpty(group.getStratifications()));
    if ((measureHasStrats && isEmpty(stratification))
        || (!measureHasStrats && isNotEmpty(stratification))) {
      return new ArrayList<>(populationCriteria);
    }

    if (measureGroups.size() > 1 && isNotEmpty(stratification)) {
      Deque<TestCaseStratificationValue> stratificationQueue = new ArrayDeque<>(stratification);
      try {
        do {
          // Assumes MADiE's Measure Group order matches incoming Group order
          // i.e. MADiE's PopCriteria 1 aligns with incoming TestCaseGroupPopulation 1
          for (int i = 0; i < measureGroups.size(); i++) {
            for (int j = 0; j < measureGroups.get(i).getStratifications().size(); j++) {
              addStrat(populationCriteria.get(i), stratificationQueue.pop());
            }
          }
        } while (!stratificationQueue.isEmpty());
      } catch (NoSuchElementException e) {
        // Import Strat count doesn't align with measure group Strat count, don't set expected
        // values.
        populationCriteria.forEach(popCrit -> popCrit.setStratificationValues(new ArrayList<>()));
      }
    } else {
      // Single group, go ahead and assign all strats.
      populationCriteria.get(0).setStratificationValues(stratification);
    }
    return new ArrayList<>(populationCriteria);
  }

  private void addStrat(
      TestCaseGroupPopulation populationCriteria, TestCaseStratificationValue strat) {
    if (populationCriteria.getStratificationValues() == null) {
      List<TestCaseStratificationValue> strats = new ArrayList<>();
      strats.add(strat);
      populationCriteria.setStratificationValues(strats);
    } else {
      populationCriteria.getStratificationValues().add(strat);
    }
  }

  private Object getPopulationExpected(
      String populationBasis, TestCasePopulationValue populationValue) {
    Object expected;
    if (populationBasis != null && populationBasis.equalsIgnoreCase("boolean")) {
      String originalValue = populationValue.getExpected().toString();
      if (originalValue.equalsIgnoreCase("1")) {
        expected = Boolean.TRUE;
      } else {
        expected = Boolean.FALSE;
      }
    } else {
      expected = populationValue.getExpected();
    }
    return expected;
  }

  // testCaseGroupPopulations may contain observations that are not in group
  protected List<TestCaseGroupPopulation> getNonObservationGroupPopulations(
      List<TestCaseGroupPopulation> testCaseGroupPopulations) {
    List<TestCaseGroupPopulation> revisedGroupPopulations = new ArrayList<>();
    if (!isEmpty(testCaseGroupPopulations)) {
      for (TestCaseGroupPopulation groupPopulation : testCaseGroupPopulations) {
        List<TestCasePopulationValue> revisedPopulationValues = null;
        if (isNotEmpty(groupPopulation.getPopulationValues())) {
          revisedPopulationValues =
              groupPopulation.getPopulationValues().stream()
                  .filter(
                      populationValue ->
                          !populationValue.getName().toCode().contains("observation"))
                  .toList();
        }
        revisedGroupPopulations.add(
            groupPopulation.toBuilder().populationValues(revisedPopulationValues).build());
      }
    }
    return revisedGroupPopulations;
  }

  public void assignObservationValues(
      TestCase newTestCase,
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      String populationBasis) {
    List<TestCasePopulationValue> observationPopulations =
        getObservationPopulations(testCaseGroupPopulations);

    TestCaseGroupPopulation groupPopulation = newTestCase.getGroupPopulations().get(0);
    List<TestCasePopulationValue> currentPopulationValues = groupPopulation.getPopulationValues();

    List<TestCasePopulationValue> combinedPopulationValues = new ArrayList<>();
    combinedPopulationValues.addAll(currentPopulationValues);
    if (!isEmpty(observationPopulations)) {
      combinedPopulationValues.addAll(
          convertPopulationValues(observationPopulations, populationBasis));
    }
    groupPopulation.setPopulationValues(combinedPopulationValues);
    List<TestCaseGroupPopulation> newGroupPopulations = new ArrayList<>();
    newGroupPopulations.add(groupPopulation);
    newTestCase.setGroupPopulations(newGroupPopulations);
  }

  private List<TestCasePopulationValue> convertPopulationValues(
      List<TestCasePopulationValue> observationValues, String populationBasis) {
    List<TestCasePopulationValue> observationPopulationValues = new ArrayList<>();
    if (!isEmpty(observationValues)) {
      for (TestCasePopulationValue observationvalue : observationValues) {
        TestCasePopulationValue populationValue =
            TestCasePopulationValue.builder()
                .id(UUID.randomUUID().toString())
                .name(observationvalue.getName())
                .expected(getPopulationExpected(populationBasis, observationvalue))
                .build();
        observationPopulationValues.add(populationValue);
      }
    }
    return observationPopulationValues;
  }

  protected List<TestCasePopulationValue> getObservationPopulations(
      List<TestCaseGroupPopulation> testCaseGroupPopulations) {
    if (!isEmpty(testCaseGroupPopulations)
        && !isEmpty(testCaseGroupPopulations.get(0).getPopulationValues())) {
      return testCaseGroupPopulations.get(0).getPopulationValues().stream()
          .filter(populationValue -> populationValue.getName().toCode().contains("observation"))
          .toList();
    }
    return null;
  }
}
