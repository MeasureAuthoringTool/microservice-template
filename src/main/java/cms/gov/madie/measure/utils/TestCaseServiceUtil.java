package cms.gov.madie.measure.utils;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import gov.cms.madie.models.measure.TestCaseStratificationValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class TestCaseServiceUtil {

  public List<Group> getGroupsWithValidPopulations(List<Group> originalGroups) {
    List<Group> changedGroups = null;
    if (!isEmpty(originalGroups)) {
      changedGroups = new ArrayList<>();
      for (Group group : originalGroups) {
        if (!isEmpty(group.getPopulations())) {
          List<Population> changedPopulations = new ArrayList<>();
          for (Population population : group.getPopulations()) {
            if (!StringUtils.isBlank(population.getDefinition())) {
              changedPopulations.add(population);
            }
          }
          group.setPopulations(changedPopulations);
        }
        changedGroups.add(group);
      }
    }
    return changedGroups;
  }

  // match criteria groups from MeasureReport in imported json file
  public boolean matchCriteriaGroups(
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      List<Group> groups,
      TestCase newTestCase) {
    boolean isValid = true;
    List<TestCaseGroupPopulation> groupPopulations = null;
    List<TestCaseGroupPopulation> revisedGroupPopulations =
        getRevisedGroupPopulation(testCaseGroupPopulations);

    // group size has to match
    if (!isEmpty(groups)
        && !isEmpty(revisedGroupPopulations)
        && groups.size() == revisedGroupPopulations.size()) {
      groupPopulations = new ArrayList<>();
      for (int i = 0; i < groups.size(); i++) {
        Group group = groups.get(i);
        // group population size has to match
        if (!isEmpty(group.getPopulations())
            && !isEmpty(revisedGroupPopulations.get(i).getPopulationValues())
            && group.getPopulations().size()
                == revisedGroupPopulations.get(i).getPopulationValues().size()) {
          isValid =
              mapPopulationValues(
                  group, revisedGroupPopulations, i, groupPopulations, newTestCase, isValid);

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
   * @param group
   * @param testCaseGroupPopulations
   * @param measureGroupNumber
   * @param groupPopulations
   * @param newTestCase
   * @param isValid
   * @return
   */
  private boolean mapPopulationValues(
      Group group,
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      int measureGroupNumber,
      List<TestCaseGroupPopulation> groupPopulations,
      TestCase newTestCase,
      boolean isValid) {
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
              testCaseGroupPopulations,
              measureGroupNumber,
              groupPopulationIndex,
              matchedNumber,
              group,
              populationValues,
              groupPopulation);
    }

    if (matchedNumber == group.getPopulations().size()) {
      // if group has observations and some existed on test case, add them back in
      groupPopulations.add(groupPopulation);
      groupPopulation.setGroupId(group.getId());
      newTestCase.setGroupPopulations(groupPopulations);
    } else {
      isValid = false;
    }

    return isValid;
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
    }
    return matchedNumber;
  }

  public void assignStratificationValuesQdm(
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      TestCase newTestCase,
      String populationBasis) {
    List<TestCaseStratificationValue> stratValues =
        convertStratificationValues(
            testCaseGroupPopulations.get(0).getStratificationValues(), populationBasis);
    if (!CollectionUtils.isEmpty(stratValues)) {
      newTestCase.getGroupPopulations().get(0).setStratificationValues(stratValues);
    }
  }

  private List<TestCaseStratificationValue> convertStratificationValues(
      List<TestCaseStratificationValue> stratValuesFromRequest, String populationBasis) {
    List<TestCaseStratificationValue> converted = new ArrayList<>();
    if (!CollectionUtils.isEmpty(stratValuesFromRequest)) {
      for (TestCaseStratificationValue stratValue : stratValuesFromRequest) {
        TestCaseStratificationValue newStratValue =
            TestCaseStratificationValue.builder()
                .id(UUID.randomUUID().toString())
                .name(stratValue.getName())
                .expected(getStratificationExpected(populationBasis, stratValue))
                .build();
        List<TestCasePopulationValue> populationValues = stratValue.getPopulationValues();
        List<TestCasePopulationValue> convertgedPopulationValues = new ArrayList<>();
        for (TestCasePopulationValue populationValue : populationValues) {
          TestCasePopulationValue value =
              TestCasePopulationValue.builder()
                  .id(UUID.randomUUID().toString())
                  .name(populationValue.getName())
                  .expected(getPopulationExpected(populationBasis, populationValue))
                  .build();
          convertgedPopulationValues.add(value);
        }
        newStratValue.setPopulationValues(convertgedPopulationValues);
        converted.add(newStratValue);
      }
    }
    return converted;
  }

  private Object getStratificationExpected(
      String populationBasis, TestCaseStratificationValue stratValue) {
    Object expected = null;
    if (populationBasis != null && populationBasis.equalsIgnoreCase("boolean")) {
      String originalValue = stratValue.getExpected().toString();
      if (originalValue.equalsIgnoreCase("1")) {
        expected = Boolean.TRUE;
      } else {
        expected = Boolean.FALSE;
      }
    } else {
      expected = stratValue.getExpected();
    }
    return expected;
  }

  private Object getPopulationExpected(
      String populationBasis, TestCasePopulationValue populationValue) {
    Object expected = null;
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
  protected List<TestCaseGroupPopulation> getRevisedGroupPopulation(
      List<TestCaseGroupPopulation> testCaseGroupPopulations) {
    List<TestCaseGroupPopulation> revisedGroupPopulations = new ArrayList<>();
    if (!isEmpty(testCaseGroupPopulations)) {
      for (TestCaseGroupPopulation groupPopulation : testCaseGroupPopulations) {
        List<TestCasePopulationValue> revisedPopulationValues = null;
        if (CollectionUtils.isNotEmpty(groupPopulation.getPopulationValues())) {
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
    if (!CollectionUtils.isEmpty(observationPopulations)) {
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
    if (!CollectionUtils.isEmpty(observationValues)) {
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
    if (!CollectionUtils.isEmpty(testCaseGroupPopulations)
        && !CollectionUtils.isEmpty(testCaseGroupPopulations.get(0).getPopulationValues())) {
      return testCaseGroupPopulations.get(0).getPopulationValues().stream()
          .filter(populationValue -> populationValue.getName().toCode().contains("observation"))
          .toList();
    }
    return null;
  }
}
