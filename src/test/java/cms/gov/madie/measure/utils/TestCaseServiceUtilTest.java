package cms.gov.madie.measure.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import gov.cms.madie.models.measure.TestCaseStratificationValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

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
            .id("Population1Id")
            .name(PopulationType.INITIAL_POPULATION)
            .definition("Initial Population")
            .build();
    population2 =
        Population.builder()
            .id("Population2Id")
            .name(PopulationType.DENOMINATOR)
            .definition("Denominator")
            .build();
    population3 =
        Population.builder()
            .id("Population3Id")
            .name(PopulationType.DENOMINATOR_EXCLUSION)
            .definition("Denominator Exclusion")
            .build();
    population4 =
        Population.builder()
            .id("Population4Id")
            .name(PopulationType.NUMERATOR)
            .definition("Numerator")
            .build();
    population5 =
        Population.builder()
            .id("Population5Id")
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
    assertThat(testCase, is(notNullValue()));
    assertThat(testCase.getGroupPopulations().size(), is(equalTo(1)));
    assertThat(testCase.getGroupPopulations().get(0).getGroupId(), is(equalTo("testGroupId")));
    assertThat(testCase.getGroupPopulations().get(0).getPopulationValues(), is(notNullValue()));
    assertThat(testCase.getGroupPopulations().get(0).getPopulationValues().size(), is(equalTo(5)));
    assertThat(
        testCase.getGroupPopulations().get(0).getPopulationValues().get(0).getId(),
        is(equalTo("Population1Id")));
    assertThat(
        testCase.getGroupPopulations().get(0).getPopulationValues().get(1).getId(),
        is(equalTo("Population2Id")));
    assertThat(
        testCase.getGroupPopulations().get(0).getPopulationValues().get(2).getId(),
        is(equalTo("Population3Id")));
    assertThat(
        testCase.getGroupPopulations().get(0).getPopulationValues().get(3).getId(),
        is(equalTo("Population4Id")));
    assertThat(
        testCase.getGroupPopulations().get(0).getPopulationValues().get(4).getId(),
        is(equalTo("Population5Id")));
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
    // Measure Group
    Group group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.PROPORTION.name())
            .populationBasis("Boolean")
            .populations(List.of(population1, population2, population3, population4))
            .stratifications(List.of(Stratification.builder().cqlDefinition("def1").build()))
            .build();
    List<Group> measureGroups = new ArrayList<>();
    measureGroups.add(group);

    TestCasePopulationValue populationValue = TestCasePopulationValue.builder().expected(1).build();

    // Imported Strat Population
    TestCaseStratificationValue stratValue =
        TestCaseStratificationValue.builder().name("Strata-1").expected(1).build();
    stratValue.setPopulationValues(List.of(populationValue));

    TestCaseGroupPopulation stratPopultion =
        TestCaseGroupPopulation.builder().stratificationValues(List.of(stratValue)).build();

    // Imported Populations
    List<TestCaseGroupPopulation> importedGroups = new ArrayList<>();
    importedGroups.add(
        TestCaseGroupPopulation.builder().populationValues(List.of(populationValue)).build());
    importedGroups.add(stratPopultion);

    List<TestCaseGroupPopulation> testCaseGroups =
        testCaseServiceUtil.assignStratificationValuesQdm(importedGroups, measureGroups);

    assertEquals(1, testCaseGroups.size());
    assertThat(testCaseGroups.get(0).getStratificationValues(), not(empty()));

    List<TestCaseStratificationValue> finalStratValues = testCaseGroups.get(0).getStratificationValues();
    assertThat(finalStratValues.size(), is(1));
    assertThat(finalStratValues.get(0).getExpected(), is(1));
  }

  @Test
  public void testAssignStratificationValuesQdmEmpty() {
    // Measure Group
    Group group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.PROPORTION.name())
            .populationBasis("Boolean")
            .populations(List.of(population1, population2, population3, population4))
            .stratifications(List.of(Stratification.builder().cqlDefinition("def1").build()))
            .build();
    List<Group> measureGroups = new ArrayList<>();
    measureGroups.add(group);

    // Imported Populations
    List<TestCaseGroupPopulation> importedGroups = new ArrayList<>();
    importedGroups.add(
        TestCaseGroupPopulation.builder()
            .populationValues(List.of(TestCasePopulationValue.builder().expected(1).build()))
            .build());

    List<TestCaseGroupPopulation> testCaseGroups =
        testCaseServiceUtil.assignStratificationValuesQdm(importedGroups, measureGroups);
    assertThat(testCaseGroups.get(0).getStratificationValues(), empty());
  }

  @Test
  public void testAssignStratificationValuesQdmBoolean() {
    // Measure Group
    Group group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.PROPORTION.name())
            .populationBasis("Boolean")
            .populations(List.of(population1, population2, population3, population4))
            .stratifications(List.of(Stratification.builder().cqlDefinition("def1").build()))
            .build();
    List<Group> measureGroups = new ArrayList<>();
    measureGroups.add(group);

    // Imported Populations
    TestCaseStratificationValue stratValue =
        TestCaseStratificationValue.builder().name("Strata-1").expected(true).build();
    stratValue.setPopulationValues(List.of(TestCasePopulationValue.builder().expected(1).build()));

    TestCaseGroupPopulation importedPopulationGroup =
        TestCaseGroupPopulation.builder()
            .populationValues(List.of(TestCasePopulationValue.builder().expected(true).build()))
            .build();
    TestCaseGroupPopulation importedStratGroup =
        TestCaseGroupPopulation.builder().stratificationValues(List.of(stratValue)).build();
    List<TestCaseGroupPopulation> importedGroups = new ArrayList<>();
    importedGroups.add(importedPopulationGroup);
    importedGroups.add(importedStratGroup);

    List<TestCaseGroupPopulation> testCaseGroups =
        testCaseServiceUtil.assignStratificationValuesQdm(importedGroups, measureGroups);

    assertEquals(1, testCaseGroups.get(0).getStratificationValues().size());
    assertEquals(
        Boolean.TRUE, testCaseGroups.get(0).getStratificationValues().get(0).getExpected());
  }

  @Test
  public void testAssignStratificationValuesQdmBooleanFalse() {
    // Measure Group
    Group group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.PROPORTION.name())
            .populationBasis("Boolean")
            .populations(List.of(population1, population2, population3, population4))
            .stratifications(List.of(Stratification.builder().cqlDefinition("def1").build()))
            .build();
    List<Group> measureGroups = new ArrayList<>();
    measureGroups.add(group);

    // Imported Populations
    TestCaseStratificationValue stratValue =
        TestCaseStratificationValue.builder().name("Strata-1").expected(false).build();
    stratValue.setPopulationValues(List.of(TestCasePopulationValue.builder().expected(1).build()));

    TestCaseGroupPopulation importedPopulationGroup =
        TestCaseGroupPopulation.builder()
            .populationValues(List.of(TestCasePopulationValue.builder().expected(true).build()))
            .build();
    TestCaseGroupPopulation importedStratGroup =
        TestCaseGroupPopulation.builder().stratificationValues(List.of(stratValue)).build();
    List<TestCaseGroupPopulation> importedGroups = new ArrayList<>();
    importedGroups.add(importedPopulationGroup);
    importedGroups.add(importedStratGroup);

    List<TestCaseGroupPopulation> testCaseGroups =
        testCaseServiceUtil.assignStratificationValuesQdm(importedGroups, measureGroups);

    assertEquals(1, testCaseGroups.get(0).getStratificationValues().size());
    assertEquals(
        Boolean.FALSE, testCaseGroups.get(0).getStratificationValues().get(0).getExpected());
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

  @Test
  public void testGetRevisedGroupPopulationReturnsEmptyListForNull() {
    List<TestCaseGroupPopulation> output =
        testCaseServiceUtil.getNonObservationGroupPopulations(null);
    assertThat(output, is(notNullValue()));
    assertThat(output.isEmpty(), is(true));
  }

  @Test
  public void testGetRevisedGroupPopulationReturnsEmptyListForEmptyInput() {
    final List<TestCaseGroupPopulation> originalTestCaseGroupPopulations = List.of();
    List<TestCaseGroupPopulation> output =
        testCaseServiceUtil.getNonObservationGroupPopulations(originalTestCaseGroupPopulations);
    assertThat(output, is(notNullValue()));
    assertThat(output.isEmpty(), is(true));
  }

  @Test
  public void testGetRevisedGroupPopulationReturnsListWithEmptyPopValuesLists() {
    final List<TestCaseGroupPopulation> originalTestCaseGroupPopulations =
        List.of(
            TestCaseGroupPopulation.builder()
                .groupId("Group1Id")
                .populationBasis("boolean")
                .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
                .build(),
            TestCaseGroupPopulation.builder()
                .groupId("Group2Id")
                .populationBasis("boolean")
                .scoring(MeasureScoring.PROPORTION.toString())
                .build());
    List<TestCaseGroupPopulation> output =
        testCaseServiceUtil.getNonObservationGroupPopulations(originalTestCaseGroupPopulations);
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(2)));
    assertThat(output.get(0), is(notNullValue()));
    assertThat(output.get(0).getPopulationValues(), is(nullValue()));
  }

  @Test
  public void testGetRevisedGroupPopulationReturnsListWithoutObservations() {
    final List<TestCaseGroupPopulation> originalTestCaseGroupPopulations =
        List.of(
            TestCaseGroupPopulation.builder()
                .groupId("Group1Id")
                .populationBasis("boolean")
                .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
                .populationValues(
                    List.of(
                        TestCasePopulationValue.builder()
                            .name(PopulationType.INITIAL_POPULATION)
                            .id("G1P1")
                            .expected("true")
                            .build(),
                        TestCasePopulationValue.builder()
                            .name(PopulationType.MEASURE_POPULATION)
                            .id("G1P2")
                            .expected("true")
                            .build(),
                        TestCasePopulationValue.builder()
                            .name(PopulationType.MEASURE_POPULATION_OBSERVATION)
                            .id("G1P3")
                            .expected("2")
                            .build()))
                .build(),
            TestCaseGroupPopulation.builder()
                .groupId("Group2Id")
                .populationBasis("Encounter")
                .scoring(MeasureScoring.RATIO.toString())
                .populationValues(
                    List.of(
                        TestCasePopulationValue.builder()
                            .name(PopulationType.INITIAL_POPULATION)
                            .id("G2P1")
                            .expected("2")
                            .build(),
                        TestCasePopulationValue.builder()
                            .name(PopulationType.DENOMINATOR)
                            .id("G2P2")
                            .expected("2")
                            .build(),
                        TestCasePopulationValue.builder()
                            .name(PopulationType.DENOMINATOR_OBSERVATION)
                            .id("G2P3")
                            .expected("15")
                            .build(),
                        TestCasePopulationValue.builder()
                            .name(PopulationType.DENOMINATOR_OBSERVATION)
                            .id("G2P4")
                            .expected("120")
                            .build(),
                        TestCasePopulationValue.builder()
                            .name(PopulationType.NUMERATOR)
                            .id("G2P5")
                            .expected("1")
                            .build(),
                        TestCasePopulationValue.builder()
                            .name(PopulationType.NUMERATOR_OBSERVATION)
                            .id("G2P6")
                            .expected("2")
                            .build()))
                .build());
    List<TestCaseGroupPopulation> output =
        testCaseServiceUtil.getNonObservationGroupPopulations(originalTestCaseGroupPopulations);
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(2)));
    assertThat(output.get(0), is(notNullValue()));
    assertThat(output.get(0).getPopulationValues(), is(notNullValue()));
    assertThat(output.get(0).getPopulationValues().size(), is(equalTo(2)));
    assertThat(output.get(1), is(notNullValue()));
    assertThat(output.get(1).getPopulationValues(), is(notNullValue()));
    assertThat(output.get(1).getPopulationValues().size(), is(equalTo(3)));
  }
}
