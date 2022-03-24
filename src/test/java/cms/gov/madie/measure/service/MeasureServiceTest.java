package cms.gov.madie.measure.service;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.*;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.MeasureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MeasureServiceTest {
  @Mock
  private MeasureRepository repository;

  @InjectMocks
  private MeasureService measureService;

  private Group group1;
  private Group group2;
  private Measure measure;

  @BeforeEach
  public void setUp() {
    // new group, not in DB, so no ID
    group1 =
        Group.builder()
            .scoring("Cohort")
            .population(Map.of(MeasurePopulation.INITIAL_POPULATION, "Initial Population"))
            .build();
    // Present in DB and has ID
    group2 =
        Group.builder()
            .id("xyz-p12r-12ert")
            .population(Map.of(MeasurePopulation.INITIAL_POPULATION, "FactorialOfFive"))
            .build();

    List<Group> groups = new ArrayList<>();
    groups.add(group2);
    measure =
        Measure.builder()
            .id("xyz-p13r-13ert")
            .cql("test cql")
            .measureScoring("Cohort")
            .measureSetId("IDIDID")
            .measureName("MSR01")
            .version("0.001")
            .groups(groups)
            .createdAt(Instant.now())
            .createdBy("test user")
            .lastModifiedAt(Instant.now())
            .lastModifiedBy("test user")
            .build();
  }

  @Test
  public void testCreateGroupWhenNoMeasureGroupsPresent() {
    // no measure group present
    measure.setGroups(null);
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(repository).findById(any(String.class));

    Mockito.doReturn(measure).when(repository).save(any(Measure.class));

    Group persistedGroup = measureService.createOrUpdateGroup(group1, measure.getId(), "test.user");

    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(group1.getId(), persistedGroup.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getGroups());
    assertEquals(1, savedMeasure.getGroups().size());
    Group capturedGroup = savedMeasure.getGroups().get(0);
    assertEquals("Cohort", capturedGroup.getScoring());
    assertEquals(
        "Initial Population",
        capturedGroup.getPopulation().get(MeasurePopulation.INITIAL_POPULATION));
  }

  @Test
  public void testCreateGroupWhenMeasureGroupsAreMultiple() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(repository).findById(any(String.class));

    Mockito.doReturn(measure).when(repository).save(any(Measure.class));

    Group persistedGroup = measureService.createOrUpdateGroup(group1, measure.getId(), "test.user");

    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(group1.getId(), persistedGroup.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getGroups());
    assertEquals(2, savedMeasure.getGroups().size());
    Group capturedGroup = savedMeasure.getGroups().get(1);
    assertEquals("Cohort", capturedGroup.getScoring());
    assertEquals(
        "Initial Population",
        capturedGroup.getPopulation().get(MeasurePopulation.INITIAL_POPULATION));
  }

  @Test
  public void testUpdateGroup() {
    // make both group IDs same, to simulate update to the group
    group1.setId(group2.getId());

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(repository).findById(any(String.class));

    Mockito.doReturn(measure).when(repository).save(any(Measure.class));

    // before update
    assertEquals(
        "FactorialOfFive",
        measure.getGroups().get(0).getPopulation().get(MeasurePopulation.INITIAL_POPULATION));

    Group persistedGroup = measureService.createOrUpdateGroup(group1, measure.getId(), "test.user");

    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(group1.getId(), persistedGroup.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getGroups());
    assertEquals(1, savedMeasure.getGroups().size());
    Group capturedGroup = savedMeasure.getGroups().get(0);
    // after update
    assertEquals(
        "Initial Population",
        capturedGroup.getPopulation().get(MeasurePopulation.INITIAL_POPULATION));
  }


  @Test
  public void testUpdateGroupChangingScoring() {
    // make both group IDs same, to simulate update to the group
    group1.setId(group2.getId());
    group2.setScoring(MeasureScoring.CONTINUOUS_VARIABLE.toString());

    // existing population referencing the group that exists in the DB
    final TestCaseGroupPopulation tcGroupPop = TestCaseGroupPopulation.builder()
        .groupId(group2.getId())
        .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
        .populationValues(
            List.of(
                TestCasePopulationValue.builder()
                    .name(MeasurePopulation.INITIAL_POPULATION)
                    .expected(true)
                    .build(),
                TestCasePopulationValue.builder()
                    .name(MeasurePopulation.MEASURE_POPULATION)
                    .expected(true)
                    .build()
            ))
        .build();

    final List<TestCase> testCases = List.of(
        TestCase.builder().groupPopulations(List.of(tcGroupPop)).build());
    measure.setTestCases(testCases);

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(repository).findById(any(String.class));

    Mockito.doReturn(measure).when(repository).save(any(Measure.class));

    // before update
    assertEquals(
        "FactorialOfFive",
        measure.getGroups().get(0).getPopulation().get(MeasurePopulation.INITIAL_POPULATION));

    Group persistedGroup = measureService.createOrUpdateGroup(group1, measure.getId(), "test.user");

    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(group1.getId(), persistedGroup.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getGroups());
    assertEquals(1, savedMeasure.getGroups().size());
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());
    assertNotNull(savedMeasure.getTestCases().get(0));
    assertNotNull(savedMeasure.getTestCases().get(0).getGroupPopulations());
    assertTrue(savedMeasure.getTestCases().get(0).getGroupPopulations().isEmpty());
    Group capturedGroup = savedMeasure.getGroups().get(0);
    // after update
    assertEquals(
        "Initial Population",
        capturedGroup.getPopulation().get(MeasurePopulation.INITIAL_POPULATION));
  }

  @Test
  public void testCreateOrUpdateGroupWhenMeasureDoesNotExist() {
    Optional<Measure> optional = Optional.empty();
    when(repository.findById(anyString())).thenReturn(optional);
    assertThrows(
        ResourceNotFoundException.class,
        () -> measureService.createOrUpdateGroup(group1, "test", "test.user"));
  }

  @Test
  public void testClearPopulationValuesForGroupHandlesNullGroupId() {
    final String groupId = null;
    final List<TestCase> testCases = List.of(
        TestCase.builder().groupPopulations(
            List.of(TestCaseGroupPopulation.builder().groupId("Group1_ID").build())
        ).build());

    List<TestCase> output = measureService.clearPopulationValuesForGroup(groupId, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testClearPopulationValuesForGroupHandlesEmptyGroupId() {
    final String groupId = "";
    final List<TestCase> testCases = List.of(
        TestCase.builder().groupPopulations(
            List.of(TestCaseGroupPopulation.builder().groupId("Group1_ID").build())
        ).build());

    List<TestCase> output = measureService.clearPopulationValuesForGroup(groupId, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testClearPopulationValuesForGroupHandlesNullTestCaseList() {
    final String groupId = "GroupID_1";
    final List<TestCase> testCases = null;

    List<TestCase> output = measureService.clearPopulationValuesForGroup(groupId, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testClearPopulationValuesForGroupHandlesEmptyTestCaseList() {
    final String groupId = "GroupID_1";
    final List<TestCase> testCases = List.of();

    List<TestCase> output = measureService.clearPopulationValuesForGroup(groupId, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testClearPopulationValuesForGroupHandlesNonMatchingID() {
    final String groupId = "Group2_ID";
    final List<TestCase> testCases = List.of(
        TestCase.builder()
            .groupPopulations(
                List.of(
                    TestCaseGroupPopulation.builder().groupId("Group1_ID")
                        .scoring(MeasureScoring.COHORT.toString())
                        .populationValues(
                            List.of(
                                TestCasePopulationValue.builder()
                                    .name(MeasurePopulation.INITIAL_POPULATION)
                                    .expected(true)
                                    .build()
                            ))
                        .build()
                ))
            .build());

    List<TestCase> output = measureService.clearPopulationValuesForGroup(groupId, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testClearPopulationValuesForGroupHandlesMatchingID() {
    final String groupId = "Group2_ID";
    TestCaseGroupPopulation group1 = TestCaseGroupPopulation.builder().groupId("Group1_ID")
        .scoring(MeasureScoring.COHORT.toString())
        .populationValues(
            List.of(
                TestCasePopulationValue.builder()
                    .name(MeasurePopulation.INITIAL_POPULATION)
                    .expected(true)
                    .build()
            ))
        .build();

    final TestCaseGroupPopulation group2 = TestCaseGroupPopulation.builder().groupId("Group2_ID")
        .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
        .populationValues(
            List.of(
                TestCasePopulationValue.builder()
                    .name(MeasurePopulation.INITIAL_POPULATION)
                    .expected(true)
                    .build(),
                TestCasePopulationValue.builder()
                    .name(MeasurePopulation.MEASURE_POPULATION)
                    .expected(true)
                    .build()
            ))
        .build();

    final List<TestCase> testCases = List.of(
        TestCase.builder().groupPopulations(List.of(group1, group2)).build());

    List<TestCase> output = measureService.clearPopulationValuesForGroup(groupId, testCases);
    assertNotNull(output);
    assertNotEquals(testCases, output);
    assertEquals(1, output.size());
    assertNotNull(output.get(0));
    assertNotNull(output.get(0).getGroupPopulations());
    assertEquals(1, output.get(0).getGroupPopulations().size());
    assertEquals(group1, output.get(0).getGroupPopulations().get(0));
  }
}
