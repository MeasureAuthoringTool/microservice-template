package cms.gov.madie.measure.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import gov.cms.madie.models.measure.TestCaseStratificationValue;

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
    assertFalse(result);
  }

  @Test
  public void testMatchCriteriaGroupsNullTestCaseGroupPopulations() {
    testCaseGroupPopulations.clear();
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertFalse(result);
  }

  @Test
  public void testMatchCriteriaGroupsGroupSizeNotMatch() {
    List<TestCaseGroupPopulation> testCaseGroupPopulations = new ArrayList<>();
    populationValues = new ArrayList<>();
    populationValues.add(testCasePopulationValue1);
    testCaseGroupPopulation1 =
        TestCaseGroupPopulation.builder()
            .populationBasis("Boolean")
            .populationValues(populationValues)
            .build();
    testCaseGroupPopulations.add(testCaseGroupPopulation1);
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertFalse(result);
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
    assertFalse(result);
  }

  @Test
  public void testMatchCriteriaGroupsNoTestCaseGroupPopulationValues() {
    List<TestCaseGroupPopulation> testCaseGroupPopulations = new ArrayList<>();
    testCaseGroupPopulation1 =
        TestCaseGroupPopulation.builder().populationBasis("Encounter").build();
    testCaseGroupPopulations.add(testCaseGroupPopulation1);
    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, List.of(group), testCase);
    assertFalse(result);
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
    assertFalse(result);
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
    assertFalse(result);
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
    assertTrue(result);
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
    assertTrue(result);
  }

  @Test
  public void testMatchCriteriaGroupsWithObservations() {
    List<TestCasePopulationValue> populationValues =
        testCaseGroupPopulations.get(0).getPopulationValues();
    TestCasePopulationValue denomPopulationValue =
        TestCasePopulationValue.builder()
            .name(PopulationType.DENOMINATOR_OBSERVATION)
            .expected(4)
            .build();
    populationValues.add(denomPopulationValue);
    TestCasePopulationValue numerPopulationValue =
        TestCasePopulationValue.builder()
            .name(PopulationType.NUMERATOR_OBSERVATION)
            .expected(4)
            .build();
    populationValues.add(numerPopulationValue);
    TestCaseGroupPopulation groupPopulation = TestCaseGroupPopulation.builder().build();
    groupPopulation.setPopulationValues(populationValues);

    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populationBasis("Encounter")
            .populations(List.of(population1, population2, population3, population4, population5))
            .build();

    boolean result =
        testCaseServiceUtil.matchCriteriaGroups(List.of(groupPopulation), List.of(group), testCase);
    assertTrue(result);
  }

  @Test
  public void testAssignStratificationValuesQdm() {
    TestCaseStratificationValue stratValue =
        TestCaseStratificationValue.builder().name("Strata-1").expected(1).build();
    TestCasePopulationValue populationValue = TestCasePopulationValue.builder().expected(1).build();
    stratValue.setPopulationValues(List.of(populationValue));
    TestCaseGroupPopulation groupPopulation =
        TestCaseGroupPopulation.builder()
            .stratificationValues(List.of(stratValue))
            .populationValues(List.of(populationValue))
            .build();

    TestCaseGroupPopulation groupPopulationFromTestCase = TestCaseGroupPopulation.builder().build();
    TestCase newTestCase =
        TestCase.builder().groupPopulations(List.of(groupPopulationFromTestCase)).build();

    testCaseServiceUtil.assignStratificationValuesQdm(
        List.of(groupPopulation), newTestCase, "Encounter");

    assertEquals(newTestCase.getGroupPopulations().get(0).getStratificationValues().size(), 1);
    assertEquals(
        newTestCase.getGroupPopulations().get(0).getStratificationValues().get(0).getExpected(), 1);
  }

  @Test
  public void testAssignStratificationValuesQdmEmpty() {

    TestCaseGroupPopulation groupPopulation =
        TestCaseGroupPopulation.builder().stratificationValues(null).build();

    TestCase newTestCase = TestCase.builder().build();

    testCaseServiceUtil.assignStratificationValuesQdm(
        List.of(groupPopulation), newTestCase, "Encounter");

    assertNull(newTestCase.getGroupPopulations());
  }

  @Test
  public void testAssignStratificationValuesQdmBoolean() {
    TestCaseStratificationValue stratValue =
        TestCaseStratificationValue.builder().name("Strata-1").expected(1).build();
    TestCasePopulationValue populationValue = TestCasePopulationValue.builder().expected(1).build();
    stratValue.setPopulationValues(List.of(populationValue));
    TestCaseGroupPopulation groupPopulation =
        TestCaseGroupPopulation.builder().stratificationValues(List.of(stratValue)).build();

    TestCaseGroupPopulation groupPopulationFromTestCase = TestCaseGroupPopulation.builder().build();
    TestCase newTestCase =
        TestCase.builder().groupPopulations(List.of(groupPopulationFromTestCase)).build();

    testCaseServiceUtil.assignStratificationValuesQdm(
        List.of(groupPopulation), newTestCase, "Boolean");

    assertEquals(newTestCase.getGroupPopulations().get(0).getStratificationValues().size(), 1);
    assertEquals(
        newTestCase.getGroupPopulations().get(0).getStratificationValues().get(0).getExpected(),
        Boolean.TRUE);
  }

  @Test
  public void testAssignStratificationValuesQdmBooleanFalse() {
    TestCaseStratificationValue stratValue =
        TestCaseStratificationValue.builder().name("Strata-1").expected(0).build();
    TestCasePopulationValue populationValue = TestCasePopulationValue.builder().expected(1).build();
    stratValue.setPopulationValues(List.of(populationValue));
    TestCaseGroupPopulation groupPopulation =
        TestCaseGroupPopulation.builder().stratificationValues(List.of(stratValue)).build();

    TestCaseGroupPopulation groupPopulationFromTestCase = TestCaseGroupPopulation.builder().build();
    TestCase newTestCase =
        TestCase.builder().groupPopulations(List.of(groupPopulationFromTestCase)).build();

    testCaseServiceUtil.assignStratificationValuesQdm(
        List.of(groupPopulation), newTestCase, "Boolean");

    assertEquals(newTestCase.getGroupPopulations().get(0).getStratificationValues().size(), 1);
    assertEquals(
        newTestCase.getGroupPopulations().get(0).getStratificationValues().get(0).getExpected(),
        Boolean.FALSE);
  }

  @Test
  public void testAssignObservationValuesBoolean() {
    TestCaseGroupPopulation groupPopulationFromTestCase =
        TestCaseGroupPopulation.builder().populationValues(populationValues).build();
    TestCase newTestCase =
        TestCase.builder().groupPopulations(List.of(groupPopulationFromTestCase)).build();

    TestCasePopulationValue populationValue1 =
        TestCasePopulationValue.builder()
            .name(PopulationType.DENOMINATOR_OBSERVATION)
            .expected(1)
            .build();
    TestCaseGroupPopulation groupPopulation =
        TestCaseGroupPopulation.builder().populationValues(List.of(populationValue1)).build();

    testCaseServiceUtil.assignObservationValues(newTestCase, List.of(groupPopulation), "Boolean");

    assertEquals(newTestCase.getGroupPopulations().get(0).getPopulationValues().size(), 6);
    assertEquals(
        newTestCase.getGroupPopulations().get(0).getPopulationValues().get(5).getExpected(),
        Boolean.TRUE);
  }

  @Test
  public void testAssignObservationValuesBooleanFalse() {
    TestCaseGroupPopulation groupPopulationFromTestCase =
        TestCaseGroupPopulation.builder().populationValues(populationValues).build();
    TestCase newTestCase =
        TestCase.builder().groupPopulations(List.of(groupPopulationFromTestCase)).build();

    TestCasePopulationValue populationValue1 =
        TestCasePopulationValue.builder()
            .name(PopulationType.DENOMINATOR_OBSERVATION)
            .expected(0)
            .build();
    TestCaseGroupPopulation groupPopulation =
        TestCaseGroupPopulation.builder().populationValues(List.of(populationValue1)).build();

    testCaseServiceUtil.assignObservationValues(newTestCase, List.of(groupPopulation), "Boolean");

    assertEquals(newTestCase.getGroupPopulations().get(0).getPopulationValues().size(), 6);
    assertEquals(
        newTestCase.getGroupPopulations().get(0).getPopulationValues().get(5).getExpected(),
        Boolean.FALSE);
  }

  @Test
  public void testGetObservationPopulationsReturnsNull() {
    List<TestCasePopulationValue> populationValues =
        testCaseServiceUtil.getObservationPopulations(null);
    assertNull(populationValues);
  }
}
