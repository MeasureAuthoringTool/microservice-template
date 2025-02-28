package cms.gov.madie.measure.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import cms.gov.madie.measure.exceptions.GroupPopulationDisplayIdException;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;

@ExtendWith(MockitoExtension.class)
public class GroupPopulationUtilTest {

  private Population population11;
  private Population population2;
  private Population population3;

  private Measure measure;
  private Group group2IPs;
  private Group group1IP;
  private Population population12;

  List<Group> groups;

  @BeforeEach
  public void setUp() {
    population11 =
        Population.builder()
            .id("id-1")
            .name(PopulationType.INITIAL_POPULATION)
            .definition(PopulationType.INITIAL_POPULATION.getDisplay())
            .displayId("InitialPopulation_1_1")
            .build();

    population12 =
        Population.builder()
            .id("id-4")
            .name(PopulationType.INITIAL_POPULATION)
            .definition(PopulationType.INITIAL_POPULATION.getDisplay())
            .displayId("InitialPopulation_1_2")
            .build();

    population2 =
        Population.builder()
            .id("id-2")
            .name(PopulationType.DENOMINATOR)
            .definition(PopulationType.DENOMINATOR.getDisplay())
            .displayId("Denominator_1")
            .build();

    population3 =
        Population.builder()
            .id("id-3")
            .name(PopulationType.NUMERATOR)
            .definition(PopulationType.NUMERATOR.getDisplay())
            .displayId("Numerator_1")
            .build();

    group2IPs =
        Group.builder()
            .id("testGroupId1")
            .displayId("Group_1")
            .scoring("Ratio")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population11, population12, population2, population3))
            .build();

    group1IP =
        Group.builder()
            .id("testGroupId1")
            .displayId("Group_1")
            .scoring("Proportion")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population11, population2, population3))
            .build();

    measure = Measure.builder().id("testMeasureId").groups(List.of(group2IPs)).build();
  }

  @Test
  public void testGetGroupNumber() {
    int result =
        GroupPopulationUtil.getGroupNumber(
            group2IPs, List.of(Group.builder().id("testGroup1").build(), group2IPs));
    assertEquals(result, 2);
  }

  @Test
  public void testValidate2IPPopulationsSuccess() {
    measure.setGroups(List.of(group2IPs));

    assertDoesNotThrow(() -> GroupPopulationUtil.validatePopulations(measure, group2IPs));
  }

  @Test
  public void testValidate1IPPopulationsSuccess() {
    population11.setDisplayId("InitialPopulation_1");
    measure.setGroups(List.of(group1IP));

    assertDoesNotThrow(() -> GroupPopulationUtil.validatePopulations(measure, group1IP));
  }

  @Test
  public void testValidatePopulationsInvalidGroupDisplayId() {
    group1IP =
        Group.builder()
            .id("testGroupId1")
            .displayId("Invalid_Group_1")
            .scoring("Proportion")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population11, population2, population3))
            .build();
    measure.setGroups(List.of(group1IP));

    assertThrows(
        GroupPopulationDisplayIdException.class,
        () -> GroupPopulationUtil.validatePopulations(measure, group1IP));
  }

  @Test
  public void testValidatePopulationsNoGroupDisplayId() {
    population11.setDisplayId("InitialPopulation_1");
    group1IP =
        Group.builder()
            .id("testGroupId1")
            .scoring("Proportion")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population11, population2, population3))
            .build();
    measure.setGroups(List.of(group1IP));

    assertDoesNotThrow(() -> GroupPopulationUtil.validatePopulations(measure, group1IP));
  }

  @Test
  public void testValidatePopulationsInvalidGroupPopulationDisplayId() {
    group1IP =
        Group.builder()
            .id("testGroupId1")
            .displayId("Group_1")
            .scoring("Proportion")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population11, population2, population3))
            .build();
    measure.setGroups(List.of(group1IP));

    assertThrows(
        GroupPopulationDisplayIdException.class,
        () -> GroupPopulationUtil.validatePopulations(measure, group1IP));
  }

  @Test
  public void testValidatePopulationsNoPopulationDisplayId() {
    population11.setDisplayId(null);
    group1IP =
        Group.builder()
            .id("testGroupId1")
            .displayId("Group_1")
            .scoring("Proportion")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population11, population2, population3))
            .build();
    measure.setGroups(List.of(group1IP));

    assertDoesNotThrow(() -> GroupPopulationUtil.validatePopulations(measure, group1IP));
  }

  @Test
  public void testValidatePopulationsNoIP() {
    Group group =
        Group.builder()
            .id("testGroupId1")
            .displayId("Group_1")
            .scoring("Proportion")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population2, population3))
            .build();
    measure.setGroups(List.of(group));

    assertDoesNotThrow(() -> GroupPopulationUtil.validatePopulations(measure, group));
  }

  @Test
  public void testSetGroupAndPopulationsDisplayIds() {
    population11.setDisplayId(null);
    population12.setDisplayId(null);
    population2.setDisplayId(null);
    group2IPs.setDisplayId(null);
    group2IPs.setPopulations(List.of(population11, population12, population2, population3));
    measure.setGroups(List.of(group2IPs));

    GroupPopulationUtil.setGroupAndPopulationsDisplayIds(measure, group2IPs);

    assertEquals("Group_1", measure.getGroups().get(0).getDisplayId());
    assertEquals(
        "InitialPopulation_1_1", measure.getGroups().get(0).getPopulations().get(0).getDisplayId());
    assertEquals(
        "InitialPopulation_1_2", measure.getGroups().get(0).getPopulations().get(1).getDisplayId());
    assertEquals(
        "Denominator_1", measure.getGroups().get(0).getPopulations().get(2).getDisplayId());
    assertEquals("Numerator_1", measure.getGroups().get(0).getPopulations().get(3).getDisplayId());
  }

  @Test
  public void testSetGroupAndPopulationsDisplayIdsNoPopulations() {
    Group group = Group.builder().id("testGroupId1").populations(Collections.emptyList()).build();
    measure.setGroups(List.of(group));

    GroupPopulationUtil.setGroupAndPopulationsDisplayIds(measure, group);

    assertEquals("Group_1", measure.getGroups().get(0).getDisplayId());
    assertEquals(0, measure.getGroups().get(0).getPopulations().size());
  }

  @Test
  public void testSetDisplayIdsNoIP() {
    Group group =
        Group.builder()
            .id("testGroupId1")
            .displayId("Group_1")
            .scoring("Proportion")
            .populations(List.of(population2, population3))
            .build();
    measure.setGroups(List.of(group));

    GroupPopulationUtil.setGroupAndPopulationsDisplayIds(measure, group);

    assertEquals("Group_1", measure.getGroups().get(0).getDisplayId());
    assertEquals(
        "Denominator_1", measure.getGroups().get(0).getPopulations().get(0).getDisplayId());
    assertEquals("Numerator_1", measure.getGroups().get(0).getPopulations().get(1).getDisplayId());
  }

  @Test
  public void testSetDisplayIdsOneIP() {
    population11.setDisplayId(null);
    population2.setDisplayId(null);
    group1IP.setDisplayId(null);
    group1IP.setPopulations(List.of(population11, population2, population3));
    measure.setGroups(List.of(group1IP));

    GroupPopulationUtil.setGroupAndPopulationsDisplayIds(measure, group1IP);

    assertEquals("Group_1", measure.getGroups().get(0).getDisplayId());
    assertEquals(
        "InitialPopulation_1", measure.getGroups().get(0).getPopulations().get(0).getDisplayId());
    assertEquals(
        "Denominator_1", measure.getGroups().get(0).getPopulations().get(1).getDisplayId());
    assertEquals("Numerator_1", measure.getGroups().get(0).getPopulations().get(2).getDisplayId());
  }
}
