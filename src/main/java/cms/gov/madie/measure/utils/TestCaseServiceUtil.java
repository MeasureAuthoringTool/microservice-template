package cms.gov.madie.measure.utils;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;
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
    // group size has to match
    if (!CollectionUtils.isEmpty(groups)
        && !CollectionUtils.isEmpty(testCaseGroupPopulations)
        && groups.size() == testCaseGroupPopulations.size()) {
      groupPopulations = new ArrayList<>();
      for (int i = 0; i < groups.size(); i++) {
        Group group = groups.get(i);
        // group population size has to match
        if (!CollectionUtils.isEmpty(group.getPopulations())
            && !CollectionUtils.isEmpty(testCaseGroupPopulations.get(i).getPopulationValues())
            && group.getPopulations().size()
                == testCaseGroupPopulations.get(i).getPopulationValues().size()) {
          isValid =
              mapPopulationValues(
                  group, testCaseGroupPopulations, i, groupPopulations, newTestCase, isValid);
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
}
