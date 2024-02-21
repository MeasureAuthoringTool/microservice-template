package cms.gov.madie.measure.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;

@ExtendWith(MockitoExtension.class)
public class GroupPopulationUtilTest {

  private Group group1;
  private Group group2;
  private Population population1;
  private Population population2;
  private Population population3;
  private Population population4;
  private Population population7;
  private MeasureObservation observation1;
  private MeasureObservation observation2;
  private Stratification stratification1;
  private Stratification stratification2;

  List<Group> groups;

  @BeforeEach
  public void setUp() {

    population1 =
        Population.builder()
            .id("id-1")
            .name(PopulationType.INITIAL_POPULATION)
            .definition(PopulationType.INITIAL_POPULATION.getDisplay())
            .build();
    population2 =
        Population.builder()
            .id("id-2")
            .name(PopulationType.DENOMINATOR)
            .definition(PopulationType.DENOMINATOR.getDisplay())
            .build();
    population3 =
        Population.builder().definition(PopulationType.DENOMINATOR_EXCLUSION.getDisplay()).build();
    population4 =
        Population.builder()
            .id("id-4")
            .name(PopulationType.NUMERATOR)
            .definition(PopulationType.NUMERATOR.getDisplay())
            .build();
    Population population5 =
        Population.builder().definition(PopulationType.NUMERATOR_EXCLUSION.getDisplay()).build();
    Population population6 =
        Population.builder()
            .id("id-6")
            .name(PopulationType.DENOMINATOR_EXCEPTION)
            .definition(PopulationType.DENOMINATOR_EXCEPTION.getDisplay())
            .build();
    population7 =
        Population.builder()
            .id("id-7")
            .name(PopulationType.MEASURE_POPULATION)
            .definition(PopulationType.MEASURE_POPULATION.getDisplay())
            .build();

    observation1 =
        MeasureObservation.builder()
            .definition(PopulationType.DENOMINATOR_OBSERVATION.getDisplay())
            .build();
    observation2 =
        MeasureObservation.builder()
            .definition(PopulationType.NUMERATOR_OBSERVATION.getDisplay())
            .build();

    stratification1 =
        Stratification.builder()
            .cqlDefinition(PopulationType.INITIAL_POPULATION.getDisplay())
            .build();
    stratification2 =
        Stratification.builder().cqlDefinition(PopulationType.DENOMINATOR.getDisplay()).build();

    group1 =
        Group.builder()
            .id("testGroupId1")
            .scoring("Proportion")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population1, population2, population3, population4))
            .measureObservations(List.of(observation1))
            .stratifications(List.of(stratification1))
            .build();
    group2 =
        Group.builder()
            .id("testGroupId1")
            .scoring("Proportion")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population1, population2, population5, population6))
            .measureObservations(List.of(observation2))
            .stratifications(List.of(stratification2))
            .build();

    groups =
        List.of(
            Group.builder()
                .scoring(MeasureScoring.RATIO.toString())
                .populations(List.of(population1, population2, population6, population4))
                .build());
  }

  @Test
  public void testAreGroupsAndPopulationsMatchingEmptyGroups() {
    boolean result = GroupPopulationUtil.areGroupsAndPopulationsMatching(null, null);
    assertFalse(result);
  }

  @Test
  public void testAreGroupsAndPopulationsMatchingOneGroupEmpty() {
    boolean result = GroupPopulationUtil.areGroupsAndPopulationsMatching(null, List.of(group1));
    assertFalse(result);
    result = GroupPopulationUtil.areGroupsAndPopulationsMatching(List.of(), List.of(group1));
    assertFalse(result);
    result = GroupPopulationUtil.areGroupsAndPopulationsMatching(List.of(group1), null);
    assertFalse(result);
    result = GroupPopulationUtil.areGroupsAndPopulationsMatching(List.of(group1), List.of());
    assertFalse(result);
  }

  @Test
  public void testAreGroupsAndPopulationsMatchingFindMatching() {
    boolean result =
        GroupPopulationUtil.areGroupsAndPopulationsMatching(
            List.of(group1), List.of(group1, group2));
    assertTrue(result);
  }

  @Test
  public void testAreGroupsAndPopulationsMatchingNoMatching() {
    boolean result =
        GroupPopulationUtil.areGroupsAndPopulationsMatching(List.of(group1), List.of(group2));
    assertFalse(result);
  }

  @Test
  public void testIsGroupPopulationsMatchingPopulationsNotMatch() {
    boolean result = GroupPopulationUtil.isGroupPopulationsMatching(group1, group2);
    assertFalse(result);
  }

  @Test
  public void testIsGroupPopulationsMatchingObservationsNotMatch() {
    group2.setPopulations(List.of(population1, population2, population3, population4));
    boolean result = GroupPopulationUtil.isGroupPopulationsMatching(group1, group2);
    assertFalse(result);
  }

  @Test
  public void testIsGroupPopulationsMatchingStratificationsNotMatch() {
    group2.setPopulations(List.of(population1, population2, population3, population4));
    group2.setMeasureObservations(List.of(observation1));
    boolean result = GroupPopulationUtil.isGroupPopulationsMatching(group1, group2);
    assertFalse(result);
  }

  @Test
  public void testIsGroupPopulationsMatchingAllMatch() {
    group2.setPopulations(List.of(population1, population2, population3, population4));
    group2.setMeasureObservations(List.of(observation1));
    group2.setStratifications(List.of(stratification1));
    boolean result = GroupPopulationUtil.isGroupPopulationsMatching(group1, group2);
    assertTrue(result);
  }

  @Test
  public void testGetValidPopulationsGroupNull() {
    List<Population> result = GroupPopulationUtil.getValidPopulations(null);
    assertNull(result);
  }

  @Test
  public void testGetValidPopulationsPopulationsNull() {
    List<Population> result = GroupPopulationUtil.getValidPopulations(Group.builder().build());
    assertNull(result);
  }

  @Test
  public void testGetValidPopulationsDefinitionNull() {
    List<Population> result =
        GroupPopulationUtil.getValidPopulations(
            Group.builder().populations(List.of(Population.builder().build())).build());
    assertTrue(result.size() == 0);
  }

  @Test
  public void testIsPopulationMatchEmptyPopulations() {
    boolean result = GroupPopulationUtil.isPopulationMatch(List.of(), List.of());
    assertTrue(result);
  }

  @Test
  public void testIsPopulationMatchOneEmptyPopulation() {
    boolean result = GroupPopulationUtil.isPopulationMatch(List.of(population1), List.of());
    assertFalse(result);
    result = GroupPopulationUtil.isPopulationMatch(List.of(), List.of(population1));
    assertFalse(result);
  }

  @Test
  public void testIsPopulationMatchSizeNotMatch() {
    boolean result =
        GroupPopulationUtil.isPopulationMatch(
            List.of(population1, population2, population3, population4), List.of(population1));
    assertFalse(result);
  }

  @Test
  public void testGetValidObservationsGroupNull() {
    List<MeasureObservation> result = GroupPopulationUtil.getValidObservations(null);
    assertNull(result);
  }

  @Test
  public void testGetValidObservationsObservationsNull() {
    List<MeasureObservation> result =
        GroupPopulationUtil.getValidObservations(Group.builder().build());
    assertNull(result);
  }

  @Test
  public void testGetValidObservationsDefinitionNull() {
    List<MeasureObservation> result =
        GroupPopulationUtil.getValidObservations(
            Group.builder()
                .measureObservations(List.of(MeasureObservation.builder().build()))
                .build());
    assertTrue(result.size() == 0);
  }

  @Test
  public void testIsMeasureObservationMatchEmptyObservations() {
    boolean result = GroupPopulationUtil.isMeasureObservationMatch(List.of(), List.of());
    assertTrue(result);
  }

  @Test
  public void testIsMeasureObservationMatchOneEmptyObservation() {
    boolean result =
        GroupPopulationUtil.isMeasureObservationMatch(List.of(observation1), List.of());
    assertFalse(result);
    result = GroupPopulationUtil.isMeasureObservationMatch(List.of(), List.of(observation1));
    assertFalse(result);
  }

  @Test
  public void testIsMeasureObservationMatchSizeNotMatch() {
    boolean result =
        GroupPopulationUtil.isMeasureObservationMatch(
            List.of(observation1, observation2), List.of(observation1));
    assertFalse(result);
  }

  @Test
  public void testGetValidStratificationsGroupNull() {
    List<Stratification> result = GroupPopulationUtil.getValidStratifications(null);
    assertNull(result);
  }

  @Test
  public void testGetValidStratificationsStratificationsNull() {
    List<Stratification> result =
        GroupPopulationUtil.getValidStratifications(Group.builder().build());
    assertNull(result);
  }

  @Test
  public void testGetValidStratificationsDefinitionNull() {
    List<Stratification> result =
        GroupPopulationUtil.getValidStratifications(
            Group.builder().stratifications(List.of(Stratification.builder().build())).build());
    assertTrue(result.size() == 0);
  }

  @Test
  public void testIsStratificationMatchEmptyStratifications() {
    boolean result = GroupPopulationUtil.isStratificationMatch(List.of(), List.of());
    assertTrue(result);
  }

  @Test
  public void testIsStratificationMatchOneEmptyPopulation() {
    boolean result = GroupPopulationUtil.isStratificationMatch(List.of(stratification1), List.of());
    assertFalse(result);
    result = GroupPopulationUtil.isStratificationMatch(List.of(), List.of(stratification1));
    assertFalse(result);
  }

  @Test
  public void testIsStratificationMatchSizeNotMatch() {
    boolean result =
        GroupPopulationUtil.isStratificationMatch(
            List.of(stratification1, stratification2), List.of(stratification1));
    assertFalse(result);
  }

  @Test
  public void testReorderGroupPopulationsRatio() {
    Group copiedGroup = Group.builder().populations(groups.get(0).getPopulations()).build();

    GroupPopulationUtil.reorderGroupPopulations(groups);

    assertEquals(1, groups.size());
    assertEquals(4, copiedGroup.getPopulations().size());
    assertEquals(5, groups.get(0).getPopulations().size());
    assertEquals(
        copiedGroup.getPopulations().get(0).getId(), groups.get(0).getPopulations().get(0).getId());
    assertEquals("Initial Population", groups.get(0).getPopulations().get(0).getDefinition());
    assertEquals(
        copiedGroup.getPopulations().get(1).getId(), groups.get(0).getPopulations().get(1).getId());
    assertEquals("Denominator", groups.get(0).getPopulations().get(1).getDefinition());
    // DENOMINATOR_EXCEPTION is not in the reordered group population
    assertNotEquals(
        copiedGroup.getPopulations().get(2).getId(), groups.get(0).getPopulations().get(2).getId());
    assertEquals(
        "DENOMINATOR_EXCLUSION", groups.get(0).getPopulations().get(2).getName().toString());
    assertEquals(
        copiedGroup.getPopulations().get(3).getId(), groups.get(0).getPopulations().get(3).getId());
    assertEquals("Numerator", groups.get(0).getPopulations().get(3).getDefinition());
    assertEquals(
        PopulationType.NUMERATOR_EXCLUSION, groups.get(0).getPopulations().get(4).getName());
  }

  @Test
  public void testReorderGroupPopulationsProportion() {
    groups.get(0).setScoring(MeasureScoring.PROPORTION.toString());
    Group copiedGroup = Group.builder().populations(groups.get(0).getPopulations()).build();

    GroupPopulationUtil.reorderGroupPopulations(groups);

    assertEquals(1, groups.size());
    assertEquals(4, copiedGroup.getPopulations().size());
    assertEquals(6, groups.get(0).getPopulations().size());
    assertEquals(
        copiedGroup.getPopulations().get(0).getId(), groups.get(0).getPopulations().get(0).getId());
    assertEquals("Initial Population", groups.get(0).getPopulations().get(0).getDefinition());
    assertEquals(
        copiedGroup.getPopulations().get(1).getId(), groups.get(0).getPopulations().get(1).getId());
    assertEquals("Denominator", groups.get(0).getPopulations().get(1).getDefinition());
    assertNotEquals(
        copiedGroup.getPopulations().get(2).getId(), groups.get(0).getPopulations().get(2).getId());
    assertEquals(
        PopulationType.DENOMINATOR_EXCLUSION, groups.get(0).getPopulations().get(2).getName());
    assertEquals("", groups.get(0).getPopulations().get(2).getDefinition());
    assertEquals(
        copiedGroup.getPopulations().get(3).getId(), groups.get(0).getPopulations().get(3).getId());
    assertEquals("Numerator", groups.get(0).getPopulations().get(3).getDefinition());
    assertEquals(
        PopulationType.NUMERATOR_EXCLUSION, groups.get(0).getPopulations().get(4).getName());
    assertEquals("", groups.get(0).getPopulations().get(4).getDefinition());
    // DENOMINATOR_EXCEPTION is in the reordered group population but reordered
    assertEquals(
        PopulationType.DENOMINATOR_EXCEPTION, groups.get(0).getPopulations().get(5).getName());
  }

  @Test
  public void testReorderGroupPopulationsCohort() {
    groups.get(0).setScoring("Cohort");

    GroupPopulationUtil.reorderGroupPopulations(groups);

    assertEquals(1, groups.size());
    assertEquals(1, groups.get(0).getPopulations().size());
  }

  @Test
  public void testReorderGroupPopulationsEmptyGroups() {
    List<Group> reorderedGroups = List.of();
    GroupPopulationUtil.reorderGroupPopulations(reorderedGroups);
    assertTrue(CollectionUtils.isEmpty(reorderedGroups));
  }

  @Test
  public void testReorderGroupPopulationsEmptyPopulations() {
    List<Group> reorderGroups = List.of(Group.builder().build());
    GroupPopulationUtil.reorderGroupPopulations(reorderGroups);
    assertFalse(CollectionUtils.isEmpty(reorderGroups));
  }

  @Test
  public void testReorderGroupPopulationsForCV() {
    Group copiedGroup =
        Group.builder()
            .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
            .populations(List.of(groups.get(0).getPopulations().get(0), population7))
            .measureObservations(List.of(observation1))
            .stratifications(List.of(stratification1, stratification2))
            .build();
    List<Group> reorderedGroups = List.of(copiedGroup);

    GroupPopulationUtil.reorderGroupPopulations(reorderedGroups);

    System.out.println("copiedGroup = " + copiedGroup.getPopulations().toString());

    assertEquals(1, reorderedGroups.size());
    assertEquals(3, reorderedGroups.get(0).getPopulations().size());
    assertEquals(
        PopulationType.INITIAL_POPULATION,
        reorderedGroups.get(0).getPopulations().get(0).getName());
    assertEquals(
        copiedGroup.getPopulations().get(0).getId(),
        reorderedGroups.get(0).getPopulations().get(0).getId());
    assertEquals(
        copiedGroup.getPopulations().get(0).getDefinition(),
        reorderedGroups.get(0).getPopulations().get(0).getDefinition());
    assertEquals(
        PopulationType.MEASURE_POPULATION,
        reorderedGroups.get(0).getPopulations().get(1).getName());
    assertEquals(population7.getId(), reorderedGroups.get(0).getPopulations().get(1).getId());
    assertEquals(
        population7.getDefinition(),
        reorderedGroups.get(0).getPopulations().get(1).getDefinition());
    assertEquals(
        PopulationType.MEASURE_POPULATION_EXCLUSION,
        reorderedGroups.get(0).getPopulations().get(2).getName());
    assertNotEquals("", reorderedGroups.get(0).getPopulations().get(2).getId());
    assertEquals("", reorderedGroups.get(0).getPopulations().get(2).getDefinition());
    assertEquals(1, reorderedGroups.get(0).getMeasureObservations().size());
    assertEquals(2, reorderedGroups.get(0).getStratifications().size());
  }
}
