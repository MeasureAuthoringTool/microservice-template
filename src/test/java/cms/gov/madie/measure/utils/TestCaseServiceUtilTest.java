package cms.gov.madie.measure.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;

@ExtendWith(MockitoExtension.class)
public class TestCaseServiceUtilTest {

  @InjectMocks private TestCaseServiceUtil testCaseServiceUtil;
  private Population population1;
  private Population population2;
  private Population population3;
  private Population population4;
  private Population population5;
  private Group group;

  List<TestCasePopulationValue> populationValues = new ArrayList<>();
  TestCasePopulationValue testCasePopulationValue1 =
      TestCasePopulationValue.builder()
          .name(PopulationType.INITIAL_POPULATION)
          .expected("1")
          .build();
  TestCasePopulationValue testCasePopulationValue2 =
      TestCasePopulationValue.builder().name(PopulationType.DENOMINATOR).expected("2").build();
  TestCasePopulationValue testCasePopulationValue3 =
      TestCasePopulationValue.builder()
          .name(PopulationType.DENOMINATOR_EXCLUSION)
          .expected("3")
          .build();
  TestCasePopulationValue testCasePopulationValue4 =
      TestCasePopulationValue.builder().name(PopulationType.NUMERATOR).expected("4").build();
  TestCasePopulationValue testCasePopulationValue5 =
      TestCasePopulationValue.builder()
          .name(PopulationType.NUMERATOR_EXCLUSION)
          .expected("5")
          .build();

  private List<TestCaseGroupPopulation> testCaseGroupPopulations = new ArrayList<>();
  TestCaseGroupPopulation testCaseGroupPopulation1 = null;
  TestCaseGroupPopulation testCaseGroupPopulation2 = null;

  private TestCase testCase;

  @BeforeEach
  public void setup() {
    populationValues.add(testCasePopulationValue1);
    populationValues.add(testCasePopulationValue2);
    populationValues.add(testCasePopulationValue3);
    populationValues.add(testCasePopulationValue4);
    populationValues.add(testCasePopulationValue5);

    testCaseGroupPopulation1 =
        TestCaseGroupPopulation.builder()
            .populationBasis("Encounter")
            .populationValues(populationValues)
            .build();
    testCaseGroupPopulation2 =
        TestCaseGroupPopulation.builder()
            .populationBasis("Encounter")
            .populationValues(populationValues)
            .build();
    testCaseGroupPopulations.add(testCaseGroupPopulation1);

    population1 =
        Population.builder()
            .name(PopulationType.INITIAL_POPULATION)
            .definition("Initial Population")
            .build();
    population2 =
        Population.builder().name(PopulationType.DENOMINATOR).definition("Denominator").build();
    population3 =
        Population.builder()
            .name(PopulationType.DENOMINATOR_EXCLUSION)
            .definition("Denominator Exclusion")
            .build();
    population4 =
        Population.builder().name(PopulationType.NUMERATOR).definition("Numerator").build();
    population5 =
        Population.builder()
            .name(PopulationType.NUMERATOR_EXCLUSION)
            .definition("Numerator Exclusion")
            .build();
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populationBasis("Boolean")
            .populations(List.of(population1, population2, population3, population4, population5))
            .build();

    testCase =
        TestCase.builder()
            .id("TESTID")
            .title("IPPPass")
            .series("BloodPressure>124")
            .json("{\"resourceType\":\"Patient\"}")
            .patientId(UUID.randomUUID())
            .build();
  }

  @Test
  public void testGetGroupsWithValidPopulationsReturnsNullWithNullInput() {
    List<Group> changedGroups = testCaseServiceUtil.getGroupsWithValidPopulations(null);
    assertThat(changedGroups, is(nullValue()));
  }

  @Test
  public void testGetGroupsWithValidPopulationsReturnsNullPopulations() {
    group = Group.builder().id("testGroupId").build();
    List<Group> changedGroups = testCaseServiceUtil.getGroupsWithValidPopulations(List.of(group));
    assertNotNull(changedGroups);
    assertThat(changedGroups.size(), is(1));
    assertNull(changedGroups.get(0).getPopulations());
  }

  @Test
  public void testGetGroupsWithValidPopulationsRemovePopulationsWithoutDefinition() {
    population1 =
        Population.builder()
            .name(PopulationType.INITIAL_POPULATION)
            .definition("Initial Population")
            .build();
    population2 = Population.builder().name(PopulationType.DENOMINATOR).build();
    population3 = Population.builder().name(PopulationType.DENOMINATOR_EXCLUSION).build();
    population4 = Population.builder().name(PopulationType.NUMERATOR).build();
    population5 = Population.builder().name(PopulationType.NUMERATOR_EXCLUSION).build();
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populationBasis("Boolean")
            .populations(List.of(population1, population2, population3, population4, population5))
            .build();
    List<Group> changedGroups = testCaseServiceUtil.getGroupsWithValidPopulations(List.of(group));
    assertNotNull(changedGroups);
    assertThat(changedGroups.size(), is(1));
    assertNotNull(changedGroups.get(0));
    assertNotNull(changedGroups.get(0).getPopulations());
    assertThat(changedGroups.get(0).getPopulations().size(), is(1));
  }

  @Test
  public void testGetGroupsWithValidPopulationsNoRemoval() {
    List<Group> changedGroups = testCaseServiceUtil.getGroupsWithValidPopulations(List.of(group));
    assertNotNull(changedGroups);
    assertThat(changedGroups.size(), is(1));
    assertNotNull(changedGroups.get(0));
    assertNotNull(changedGroups.get(0).getPopulations());
    assertThat(changedGroups.get(0).getPopulations().size(), is(5));
  }

  @Test
  public void testMatchCriteriaGroupsSuccessAllMatch() {
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertTrue(result);
  }

  @Test
  public void testMatchCriteriaGroupsNullGroup() {
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, null, testCase);
    assertEquals(false, result);
  }

  @Test
  public void testMatchCriteriaGroupsNullTestCaseGroupPopulations() {
    testCaseGroupPopulations.clear();
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertEquals(false, result);
  }

  @Test
  public void testMatchCriteriaGroupsGroupSizeNotMatch() {
    List<TestCaseGroupPopulation> testCaseGroupPopulations = new ArrayList<>();
    testCaseGroupPopulations.add(testCaseGroupPopulation1);
    testCaseGroupPopulations.add(testCaseGroupPopulation2);
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertEquals(false, result);
  }

  @Test
  public void testMatchCriteriaGroupsNoGroupPopulation() {
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populationBasis("Boolean")
            .build();
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertEquals(false, result);
  }

  @Test
  public void testMatchCriteriaGroupsNoTestCaseGroupPopulationValues() {
    List<TestCaseGroupPopulation> testCaseGroupPopulations = new ArrayList<>();
    testCaseGroupPopulation1 =
        TestCaseGroupPopulation.builder().populationBasis("Encounter").build();
    testCaseGroupPopulations.add(testCaseGroupPopulation1);
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertEquals(false, result);
  }

  @Test
  public void testMatchCriteriaGroupsPopulationSizeNotMatch() {
    List<TestCaseGroupPopulation> testCaseGroupPopulations = new ArrayList<>();
    List<TestCasePopulationValue> populationValues = new ArrayList<>();
    populationValues.add(testCasePopulationValue1);
    testCaseGroupPopulation1 =
        TestCaseGroupPopulation.builder()
            .populationBasis("Encounter")
            .populationValues(populationValues)
            .build();
    testCaseGroupPopulations.add(testCaseGroupPopulation1);
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertEquals(false, result);
  }

  @Test
  public void testMatchCriteriaGroupsNotAllMatched() {
    List<TestCaseGroupPopulation> testCaseGroupPopulations = new ArrayList<>();
    List<TestCasePopulationValue> populationValues = new ArrayList<>();
    populationValues.add(testCasePopulationValue1);
    populationValues.add(testCasePopulationValue2);
    populationValues.add(testCasePopulationValue3);
    populationValues.add(testCasePopulationValue4);
    populationValues.add(testCasePopulationValue4);
    testCaseGroupPopulation1 =
        TestCaseGroupPopulation.builder()
            .populationBasis("Encounter")
            .populationValues(populationValues)
            .build();
    testCaseGroupPopulations.add(testCaseGroupPopulation1);
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertEquals(false, result);
  }

  @Test
  public void testMatchCriteriaGroupsNoPopulationBasis() {
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populations(List.of(population1, population2, population3, population4, population5))
            .build();
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertEquals(true, result);
  }

  @Test
  public void testMatchCriteriaGroupsPopulationBasisNotBoolean() {
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populationBasis("Encounter")
            .populations(List.of(population1, population2, population3, population4, population5))
            .build();
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertEquals(true, result);
  }
}
