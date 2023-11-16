package cms.gov.madie.measure.utils;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

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

    String mappedGroupPopulationBasis = null;
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
          mappedGroupPopulationBasis = group.getPopulationBasis();
        } else {
          isValid = false;
        }
      }
    } else {
      isValid = false;
    }

    if (!isEmpty(testCaseGroupPopulations)
        && !isEmpty(revisedGroupPopulations)
        && !isEmpty(testCaseGroupPopulations.get(0).getPopulationValues())
        && !isEmpty(revisedGroupPopulations.get(0).getPopulationValues())
        && testCaseGroupPopulations.get(0).getPopulationValues().size()
            > revisedGroupPopulations.get(0).getPopulationValues().size()) {
      assignObservationValues(newTestCase, testCaseGroupPopulations, mappedGroupPopulationBasis);
    }
    assignStratificationValuesQdm(
        testCaseGroupPopulations, newTestCase, mappedGroupPopulationBasis);

    return isValid;
  }

  private TestCaseGroupPopulation assignTestCaseGroupPopulation(Group group) {
    return TestCaseGroupPopulation.builder()
        .groupId(group.getId())
        .scoring(group.getScoring())
        .populationBasis(group.getPopulationBasis())
        .build();
  }

  private boolean mapPopulationValues(
      Group group,
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      int i,
      List<TestCaseGroupPopulation> groupPopulations,
      TestCase newTestCase,
      boolean isValid) {
    TestCaseGroupPopulation groupPopulation = assignTestCaseGroupPopulation(group);
    List<TestCasePopulationValue> populationValues = new ArrayList<>();
    int matchedNumber = 0;
    for (int j = 0; j < group.getPopulations().size(); j++) {
      Population population = group.getPopulations().get(j);
      matchedNumber =
          assignPopulationValues(
              population,
              testCaseGroupPopulations,
              i,
              j,
              matchedNumber,
              group,
              populationValues,
              groupPopulation);
      // check if matchedNumber is correct
      if (j == group.getPopulations().size() - 1) {
        if (matchedNumber == group.getPopulations().size()) {
          groupPopulations.add(groupPopulation);
          newTestCase.setGroupPopulations(groupPopulations);
        } else {
          isValid = false;
        }
      }
    }
    return isValid;
  }

  private int assignPopulationValues(
      Population population,
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      int i,
      int j,
      int matchedNumber,
      Group group,
      List<TestCasePopulationValue> populationValues,
      TestCaseGroupPopulation groupPopulation) {
    if (population
        .getName()
        .toCode()
        .equalsIgnoreCase(
            testCaseGroupPopulations.get(i).getPopulationValues().get(j).getName().toCode())) {
      matchedNumber++;

      TestCasePopulationValue populationValue =
          testCaseGroupPopulations.get(i).getPopulationValues().get(j);
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

  protected void assignStratificationValuesQdm(
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      TestCase newTestCase,
      String mappedGroupPopulationBasis) {
    List<TestCaseGroupPopulation> groupPopulations = newTestCase.getGroupPopulations();

    List<TestCaseGroupPopulation> newGroupPopulations = null;
    if (!CollectionUtils.isEmpty(groupPopulations)) {
      newGroupPopulations = new ArrayList<>();
      for (int i = 0; i < groupPopulations.size(); i++) {
        TestCaseGroupPopulation groupPopulationFromTestCase = groupPopulations.get(i);
        TestCaseGroupPopulation groupPopulationFromImportedRequest =
            testCaseGroupPopulations.get(i);
        groupPopulationFromTestCase.setStratificationValues(
            convertStratificationValues(
                groupPopulationFromImportedRequest.getStratificationValues(),
                mappedGroupPopulationBasis));
        groupPopulationFromTestCase.setPopulationValues(
            groupPopulationFromTestCase.getPopulationValues());
        newGroupPopulations.add(groupPopulationFromTestCase);
      }
    }
    newTestCase.setGroupPopulations(newGroupPopulations);
  }

  private List<TestCaseStratificationValue> convertStratificationValues(
      List<TestCaseStratificationValue> stratValuesFromRequest, String populationBasis) {
    List<TestCaseStratificationValue> converted = null;
    if (!CollectionUtils.isEmpty(stratValuesFromRequest)) {
      converted = new ArrayList<>();
      for (TestCaseStratificationValue stratValue : stratValuesFromRequest) {
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
        TestCaseStratificationValue newStratValue =
            TestCaseStratificationValue.builder()
                .name(stratValue.getName())
                .expected(expected)
                .build();
        converted.add(newStratValue);
      }
    }
    return converted;
  }

  // testCaseGroupPopulations may contain observations that are not in group
  protected List<TestCaseGroupPopulation> getRevisedGroupPopulation(
      List<TestCaseGroupPopulation> testCaseGroupPopulations) {
    List<TestCaseGroupPopulation> revisedGroupPopulations = new ArrayList<>();
    List<TestCasePopulationValue> revisedPopulationValues = new ArrayList<>();
    if (!CollectionUtils.isEmpty(testCaseGroupPopulations)
        && CollectionUtils.isNotEmpty(testCaseGroupPopulations.get(0).getPopulationValues())) {
      revisedPopulationValues =
          testCaseGroupPopulations.get(0).getPopulationValues().stream()
              .filter(
                  populationValue -> !populationValue.getName().toCode().contains("observation"))
              .toList();
    }

    TestCaseGroupPopulation revisedGroupPopulation =
        TestCaseGroupPopulation.builder().populationValues(revisedPopulationValues).build();
    revisedGroupPopulations.add(revisedGroupPopulation);
    return revisedGroupPopulations;
  }

  protected void assignObservationValues(
      TestCase newTestCase,
      List<TestCaseGroupPopulation> testCaseGroupPopulations,
      String populationBasis) {
    List<TestCaseGroupPopulation> observationPopulations =
        getObservationGroupPopulation(testCaseGroupPopulations);
    TestCaseGroupPopulation observationPopulation = observationPopulations.get(0);
    List<TestCasePopulationValue> observationValues = observationPopulation.getPopulationValues();

    TestCaseGroupPopulation groupPopulation = newTestCase.getGroupPopulations().get(0);
    List<TestCasePopulationValue> currentPopulationValues = groupPopulation.getPopulationValues();

    List<TestCasePopulationValue> combinedPopulationValues = new ArrayList<>();
    combinedPopulationValues.addAll(currentPopulationValues);
    combinedPopulationValues.addAll(convertObservationValues(observationValues, populationBasis));
    groupPopulation.setPopulationValues(combinedPopulationValues);
    List<TestCaseGroupPopulation> newGroupPopulations = new ArrayList<>();
    newGroupPopulations.add(groupPopulation);
    newTestCase.setGroupPopulations(newGroupPopulations);
  }

  private List<TestCasePopulationValue> convertObservationValues(
      List<TestCasePopulationValue> observationValues, String populationBasis) {
    List<TestCasePopulationValue> observationPopulationValues = new ArrayList<>();
    if (!CollectionUtils.isEmpty(observationValues)) {
      for (TestCasePopulationValue observationvalue : observationValues) {
        Object expected = null;
        if (populationBasis != null && populationBasis.equalsIgnoreCase("boolean")) {
          String originalValue = observationvalue.getExpected().toString();
          if (originalValue.equalsIgnoreCase("1")) {
            expected = Boolean.TRUE;
          } else {
            expected = Boolean.FALSE;
          }
        } else {
          expected = observationvalue.getExpected();
        }
        TestCasePopulationValue populationValue =
            TestCasePopulationValue.builder()
                .name(observationvalue.getName())
                .expected(expected)
                .build();
        observationPopulationValues.add(populationValue);
      }
    }
    return observationPopulationValues;
  }

  protected List<TestCaseGroupPopulation> getObservationGroupPopulation(
      List<TestCaseGroupPopulation> testCaseGroupPopulations) {
    List<TestCaseGroupPopulation> revisedGroupPopulations = new ArrayList<>();
    List<TestCasePopulationValue> revisedPopulationValues = new ArrayList<>();

    revisedPopulationValues =
        testCaseGroupPopulations.get(0).getPopulationValues().stream()
            .filter(populationValue -> populationValue.getName().toCode().contains("observation"))
            .toList();
    TestCaseGroupPopulation revisedGroupPopulation =
        TestCaseGroupPopulation.builder().populationValues(revisedPopulationValues).build();
    revisedGroupPopulations.add(revisedGroupPopulation);
    return revisedGroupPopulations;
  }
}
