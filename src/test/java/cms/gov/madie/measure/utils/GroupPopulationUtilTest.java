package cms.gov.madie.measure.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.MeasureObservation;
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
  private Population population5;
  private Population population6;
  private MeasureObservation observation1;
  private MeasureObservation observation2;
  private Stratification stratification1;
  private Stratification stratification2;

  @BeforeEach
  public void setUp() {

    population1 =
        Population.builder().definition(PopulationType.INITIAL_POPULATION.getDisplay()).build();
    population2 = Population.builder().definition(PopulationType.DENOMINATOR.getDisplay()).build();
    population3 =
        Population.builder().definition(PopulationType.DENOMINATOR_EXCLUSION.getDisplay()).build();
    population4 = Population.builder().definition(PopulationType.NUMERATOR.getDisplay()).build();
    population5 =
        Population.builder().definition(PopulationType.NUMERATOR_EXCLUSION.getDisplay()).build();
    population6 =
        Population.builder().definition(PopulationType.DENOMINATOR_EXCEPTION.getDisplay()).build();

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
  }

  @Test
  public void testisAllGroupsAndPopulationsMatchingEmptyGroups() {
    boolean result = GroupPopulationUtil.isAllGroupsAndPopulationsMatching(null, null);
    assertTrue(result);
  }

  @Test
  public void testisAllGroupsAndPopulationsMatchingOneGroupEmpty() {
    boolean result = GroupPopulationUtil.isAllGroupsAndPopulationsMatching(null, List.of(group1));
    assertFalse(result);
    result = GroupPopulationUtil.isAllGroupsAndPopulationsMatching(List.of(), List.of(group1));
    assertFalse(result);
    result = GroupPopulationUtil.isAllGroupsAndPopulationsMatching(List.of(group1), null);
    assertFalse(result);
    result = GroupPopulationUtil.isAllGroupsAndPopulationsMatching(List.of(group1), List.of());
    assertFalse(result);
  }

  @Test
  public void testisAllGroupsAndPopulationsMatchingFindMatching() {
    boolean result =
        GroupPopulationUtil.isAllGroupsAndPopulationsMatching(
            List.of(group1), List.of(group1, group2));
    assertTrue(result);
  }

  @Test
  public void testisAllGroupsAndPopulationsMatchingNoMatching() {
    boolean result =
        GroupPopulationUtil.isAllGroupsAndPopulationsMatching(List.of(group1), List.of(group2));
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
}
