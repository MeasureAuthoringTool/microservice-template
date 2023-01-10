package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import gov.cms.madie.models.measure.TestCaseStratificationValue;
import gov.cms.madie.models.common.Version;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.InvalidReturnTypeException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.measure.AggregateMethodType;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import gov.cms.madie.models.common.Version;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest implements ResourceUtil {
  @Mock private MeasureRepository measureRepository;

  @Mock private FhirServicesClient fhirServicesClient;

  @Mock private ElmTranslatorClient elmTranslatorClient;

  @InjectMocks private GroupService groupService;

  private Group group1;
  private Group group2;
  private Group ratioGroup;
  private Measure measure;
  private Stratification strata1;

  @BeforeEach
  public void setUp() {
    strata1 = new Stratification();
    strata1.setId("strat-1");
    strata1.setCqlDefinition("Initial Population");
    strata1.setAssociation(PopulationType.INITIAL_POPULATION);
    Stratification strata2 = new Stratification();
    strata2.setId("strat-2");
    strata2.setCqlDefinition("Denominator");
    strata2.setAssociation(PopulationType.DENOMINATOR);

    Stratification emptyStrat = new Stratification();
    // new group, not in DB, so no ID
    group1 =
        Group.builder()
            .scoring("Cohort")
            .populationBasis("Encounter")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();
    // Present in DB and has ID
    group2 =
        Group.builder()
            .id("xyz-p12r-12ert")
            .populationBasis("Encounter")
            .scoring("Continuous Variable")
            .populations(
                List.of(
                    new Population(
                        "id-1", PopulationType.INITIAL_POPULATION, "FactorialOfFive", null, null),
                    new Population(
                        "id-2",
                        PopulationType.MEASURE_POPULATION,
                        "Measure Population",
                        null,
                        null)))
            .measureObservations(
                List.of(
                    new MeasureObservation(
                        "id-1", "fun", "id-2", AggregateMethodType.MAXIMUM.getValue())))
            .stratifications(List.of(strata1, emptyStrat))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();
    // Ratio group
    ratioGroup =
        Group.builder()
            .scoring("Ratio")
            .populationBasis("Encounter")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null),
                    new Population("id-2", PopulationType.DENOMINATOR, "Denominator", null, null),
                    new Population(
                        "id-3", PopulationType.DENOMINATOR_EXCLUSION, "Denominator", null, null),
                    new Population("id-4", PopulationType.NUMERATOR, "Numerator", null, null),
                    new Population("id-4", PopulationType.NUMERATOR_EXCLUSION, "", null, null)))
            .measureObservations(
                new ArrayList<>(
                    List.of(
                        new MeasureObservation(
                            "mo-id-1", "fun", "id-2", AggregateMethodType.MAXIMUM.getValue()),
                        new MeasureObservation(
                            "mo-id-2", "fun", "id-4", AggregateMethodType.MAXIMUM.getValue()))))
            .stratifications(List.of(strata1, strata2))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();

    List<Group> groups = new ArrayList<>();
    groups.add(group2);
    String elmJson = getData("/test_elm.json");
    measure =
        Measure.builder()
            .active(true)
            .id("xyz-p13r-13ert")
            .cql("test cql")
            .elmJson(elmJson)
            .measureSetId("IDIDID")
            .measureName("MSR01")
            .version(new Version(0, 0, 1))
            .groups(groups)
            .createdAt(Instant.now())
            .createdBy("test user")
            .lastModifiedAt(Instant.now())
            .lastModifiedBy("test user")
            .build();
  }

  @Test
  public void testFindAllByActiveOmitsAndRetrievesCorrectly() {
    Measure m1 =
        Measure.builder()
            .active(true)
            .id("xyz-p13r-459b")
            .measureName("Measure1")
            .cqlLibraryName("TestLib1")
            .createdBy("test-okta-user-id-123")
            .model("QI-Core")
            .build();
    Measure m2 =
        Measure.builder()
            .id("xyz-p13r-459a")
            .active(false)
            .measureName("Measure2")
            .cqlLibraryName("TestLib2")
            .createdBy("test-okta-user-id-123")
            .model("QI-Core")
            .active(true)
            .build();
    Page<Measure> activeMeasures = new PageImpl<>(List.of(measure, m1));
    Page<Measure> inactiveMeasures = new PageImpl<>(List.of(m2));
    PageRequest initialPage = PageRequest.of(0, 10);

    when(measureRepository.findAllByActive(eq(true), any(PageRequest.class)))
        .thenReturn(activeMeasures);
    when(measureRepository.findAllByActive(eq(false), any(PageRequest.class)))
        .thenReturn(inactiveMeasures);

    assertEquals(measureRepository.findAllByActive(true, initialPage), activeMeasures);
    assertEquals(measureRepository.findAllByActive(false, initialPage), inactiveMeasures);
    // Inactive measure id is not present in active measures
    assertFalse(activeMeasures.stream().anyMatch(item -> "xyz-p13r-459a".equals(item.getId())));
    // but is in inactive measures
    assertTrue(inactiveMeasures.stream().anyMatch(item -> "xyz-p13r-459a".equals(item.getId())));
  }

  @Test
  public void testCreateGroupWhenNoMeasureGroupsPresent() {
    // no measure group present
    measure.setGroups(null);
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    doReturn(measure).when(measureRepository).save(any(Measure.class));

    Group persistedGroup = groupService.createOrUpdateGroup(group1, measure.getId(), "test.user");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    assertEquals(group1.getId(), persistedGroup.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getGroups());
    assertEquals(1, savedMeasure.getGroups().size());
    Group capturedGroup = savedMeasure.getGroups().get(0);
    assertEquals("Cohort", capturedGroup.getScoring());
    assertEquals(
        PopulationType.INITIAL_POPULATION, capturedGroup.getPopulations().get(0).getName());
    assertEquals("Initial Population", capturedGroup.getPopulations().get(0).getDefinition());
    assertEquals("Description", capturedGroup.getGroupDescription());
    assertEquals("test-scoring-unit", capturedGroup.getScoringUnit());
  }

  @Test
  public void testCreateGroupWhenMeasureGroupsAreMultiple() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    doReturn(measure).when(measureRepository).save(any(Measure.class));

    Group persistedGroup = groupService.createOrUpdateGroup(group1, measure.getId(), "test.user");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    assertEquals(group1.getId(), persistedGroup.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getGroups());
    assertEquals(2, savedMeasure.getGroups().size());
    Group capturedGroup = savedMeasure.getGroups().get(1);
    assertEquals("Cohort", capturedGroup.getScoring());
    assertEquals("Initial Population", capturedGroup.getPopulations().get(0).getDefinition());
    assertEquals(
        PopulationType.INITIAL_POPULATION, capturedGroup.getPopulations().get(0).getName());
    assertEquals("Description", capturedGroup.getGroupDescription());
    assertEquals("test-scoring-unit", capturedGroup.getScoringUnit());
  }

  @Test
  public void testCreateGroupWithObservationWhenMeasureGroupsAreMultiple() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    doReturn(measure).when(measureRepository).save(any(Measure.class));

    Group persistedGroup =
        groupService.createOrUpdateGroup(ratioGroup, measure.getId(), "test.user");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    assertEquals(ratioGroup.getId(), persistedGroup.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getGroups());
    assertEquals(2, savedMeasure.getGroups().size());
    Group capturedGroup = savedMeasure.getGroups().get(1);
    assertEquals("Ratio", capturedGroup.getScoring());
    assertEquals("Initial Population", capturedGroup.getPopulations().get(0).getDefinition());
    assertEquals(
        PopulationType.INITIAL_POPULATION, capturedGroup.getPopulations().get(0).getName());
    assertEquals("Description", capturedGroup.getGroupDescription());
    assertEquals("test-scoring-unit", capturedGroup.getScoringUnit());
    assertNotNull(capturedGroup.getMeasureObservations());
    assertEquals(2, capturedGroup.getMeasureObservations().size());
  }

  @Test
  public void testUpdateGroup() {
    // make both group IDs same, to simulate update to the group
    group1.setId(group2.getId());

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    doReturn(measure).when(measureRepository).save(any(Measure.class));

    // before update
    assertEquals(
        "FactorialOfFive", measure.getGroups().get(0).getPopulations().get(0).getDefinition());

    Group persistedGroup = groupService.createOrUpdateGroup(group1, measure.getId(), "test.user");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    assertEquals(group1.getId(), persistedGroup.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getGroups());
    assertEquals(1, savedMeasure.getGroups().size());
    Group capturedGroup = savedMeasure.getGroups().get(0);
    // after update
    assertEquals("Initial Population", capturedGroup.getPopulations().get(0).getDefinition());
    assertEquals("Description", capturedGroup.getGroupDescription());
    assertEquals("test-scoring-unit", capturedGroup.getScoringUnit());
  }

  @Test
  void testDeleteGroup() {
    Group group =
        Group.builder()
            .id("testgroupid")
            .scoring("Cohort")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .build();

    Measure existingMeasure =
        Measure.builder().id("measure-id").createdBy("test.user").groups(List.of(group)).build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    doReturn(existingMeasure).when(measureRepository).save(any(Measure.class));

    Measure output = groupService.deleteMeasureGroup("measure-id", "testgroupid", "test.user");

    assertEquals(0, output.getGroups().size());
  }

  @Test
  void testDeleteGroupWhenTestCaseHasNoGroups() {
    Group group =
        Group.builder()
            .id("testgroupid")
            .scoring("Cohort")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .build();
    List<TestCase> testCases = List.of(TestCase.builder().groupPopulations(null).build());

    Measure existingMeasure =
        Measure.builder()
            .id("measure-id")
            .createdBy("test.user")
            .groups(List.of(group))
            .testCases(testCases)
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    doReturn(existingMeasure).when(measureRepository).save(any(Measure.class));
    // before deletion
    assertEquals(1, existingMeasure.getGroups().size());
    groupService.deleteMeasureGroup("measure-id", "testgroupid", "test.user");
    // after deletion
    assertEquals(0, existingMeasure.getGroups().size());
  }

  @Test
  void testDeleteMeasureGroupReturnsExceptionForNullMeasureId() {
    assertThrows(
        InvalidIdException.class,
        () -> groupService.deleteMeasureGroup("", "grouptestid", "OtherUser"));
  }

  @Test
  void testDeleteMeasureGroupReturnsExceptionThrowsAccessException() {
    String groupId = "testgroupid";
    final Measure measure = Measure.builder().id("measure-id").createdBy("OtherUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        UnauthorizedException.class,
        () -> groupService.deleteMeasureGroup("measure-id", groupId, "user2"));
  }

  @Test
  void testDeleteMeasureGroupReturnsExceptionForResourceNotFound() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> groupService.deleteMeasureGroup("testid", "testgroupid", "user2"));
  }

  @Test
  void testDeleteMeasureGroupReturnsExceptionForNullId() {
    final Measure measure = Measure.builder().id("measure-id").createdBy("OtherUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));

    assertThrows(
        InvalidIdException.class,
        () -> groupService.deleteMeasureGroup("measure-id", "", "OtherUser"));
  }

  @Test
  void testDeleteMeasureGroupReturnsExceptionForGroupNotFoundInMeasure() {
    Group group =
        Group.builder()
            .id("testgroupid")
            .scoring("Cohort")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .build();

    Measure existingMeasure =
        Measure.builder().id("measure-id").createdBy("test.user").groups(List.of(group)).build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        ResourceNotFoundException.class,
        () -> groupService.deleteMeasureGroup("measure-id", "grouptestid1", "test.user"));
  }

  @Test
  public void testUpdateGroupChangingScoring() {
    // make both group IDs same, to simulate update to the group
    group1.setId(group2.getId());
    group2.setScoring(MeasureScoring.CONTINUOUS_VARIABLE.toString());

    // existing population referencing the group that exists in the DB
    final TestCaseGroupPopulation tcGroupPop =
        TestCaseGroupPopulation.builder()
            .groupId(group2.getId())
            .scoring(MeasureScoring.CONTINUOUS_VARIABLE.toString())
            .populationValues(
                List.of(
                    TestCasePopulationValue.builder()
                        .name(PopulationType.INITIAL_POPULATION)
                        .expected(true)
                        .build(),
                    TestCasePopulationValue.builder()
                        .name(PopulationType.MEASURE_POPULATION)
                        .expected(true)
                        .build()))
            .build();

    final List<TestCase> testCases =
        List.of(TestCase.builder().groupPopulations(List.of(tcGroupPop)).build());
    measure.setTestCases(testCases);

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    doReturn(measure).when(measureRepository).save(any(Measure.class));

    // before update
    assertEquals(
        "FactorialOfFive", measure.getGroups().get(0).getPopulations().get(0).getDefinition());

    Group persistedGroup = groupService.createOrUpdateGroup(group1, measure.getId(), "test.user");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
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
    assertEquals(0, savedMeasure.getTestCases().get(0).getGroupPopulations().size());
    Group capturedGroup = savedMeasure.getGroups().get(0);
    // after update
    assertEquals("Initial Population", capturedGroup.getPopulations().get(0).getDefinition());
  }

  @Test
  public void testCreateOrUpdateGroupWhenMeasureDoesNotExist() {
    Optional<Measure> optional = Optional.empty();
    when(measureRepository.findById(anyString())).thenReturn(optional);
    assertThrows(
        ResourceNotFoundException.class,
        () -> groupService.createOrUpdateGroup(group1, "test", "test.user"));
  }

  @Test
  public void testResetPopulationValuesForGroupHandlesNullGroup() {
    TestCaseGroupPopulation testPopulation =
        TestCaseGroupPopulation.builder().groupId("Group1_ID").build();
    final List<TestCase> testCases =
        List.of(TestCase.builder().groupPopulations(List.of(testPopulation)).build());

    groupService.updateGroupForTestCases(null, testCases);
    // unchanged test case populations
    assertEquals(testCases.get(0).getGroupPopulations(), List.of(testPopulation));
  }

  @Test
  public void testUpdateTestCaseGroupGroupScoringChanged() {
    final Group group =
        Group.builder().id("Group1_ID").scoring("Cohort").populationBasis("Encounter").build();
    final List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .groupPopulations(
                    List.of(
                        TestCaseGroupPopulation.builder()
                            .groupId("Group1_ID")
                            .scoring("Proportion")
                            .populationBasis("Encounter")
                            .build()))
                .build());
    // before updates
    assertEquals(1, testCases.get(0).getGroupPopulations().size());
    groupService.updateGroupForTestCases(group, testCases);
    // group should be removed from test case as  measure group scoring was changed
    assertEquals(0, testCases.get(0).getGroupPopulations().size());
  }

  @Test
  public void testUpdateTestCaseGroupGroupPopulationBasisChanged() {
    final Group group =
        Group.builder().id("Group1_ID").scoring("Cohort").populationBasis("Encounter").build();
    final List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .groupPopulations(
                    List.of(
                        TestCaseGroupPopulation.builder()
                            .groupId("Group1_ID")
                            .scoring("Cohort")
                            .populationBasis("boolean")
                            .build()))
                .build());
    // before updates
    assertEquals(1, testCases.get(0).getGroupPopulations().size());
    groupService.updateGroupForTestCases(group, testCases);
    // group should be removed from test case as populationBasis for measure group was changed
    assertEquals(0, testCases.get(0).getGroupPopulations().size());
  }

  @Test
  public void testResetPopulationValuesForGroupHandlesEmptyTestCaseList() {
    final Group group = Group.builder().id("Group1_ID").scoring("Cohort").build();
    final List<TestCase> testCases = List.of();
    // before updates
    assertEquals(0, testCases.size());
    groupService.updateGroupForTestCases(group, testCases);
    // after updates, no change to the test case list
    assertEquals(0, testCases.size());
  }

  @Test
  public void testResetPopulationValuesForGroupHandlesNullGroupPopulationList() {
    final Group group = Group.builder().id("Group2_ID").scoring("Cohort").build();
    final List<TestCase> testCases = List.of(TestCase.builder().groupPopulations(null).build());
    // before updates
    assertNull(testCases.get(0).getGroupPopulations());
    groupService.updateGroupForTestCases(group, testCases);
    // after updates, no population added to the group
    assertNull(testCases.get(0).getGroupPopulations());
  }

  @Test
  public void testResetPopulationValuesForNewGroup() {
    // group with id that does not exist in test case
    final Group group = Group.builder().id("Group2_ID").scoring("Cohort").build();
    final List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .groupPopulations(
                    List.of(
                        TestCaseGroupPopulation.builder()
                            .groupId("Group1_ID")
                            .scoring(MeasureScoring.COHORT.toString())
                            .populationValues(
                                List.of(
                                    TestCasePopulationValue.builder()
                                        .name(PopulationType.INITIAL_POPULATION)
                                        .expected(true)
                                        .build()))
                            .build()))
                .build());
    // before updates
    assertEquals(1, testCases.get(0).getGroupPopulations().size());
    groupService.updateGroupForTestCases(group, testCases);
    // after update call, do nothing for new group
    assertEquals(1, testCases.get(0).getGroupPopulations().size());
  }

  @Test
  public void testUpdateGroupChangingScoringUnit() {
    // make both group IDs same, to simulate update to the group
    group1.setId(group2.getId());
    group1.setScoringUnit("new scoring unit");

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    doReturn(measure).when(measureRepository).save(any(Measure.class));

    // before update
    assertEquals(
        "FactorialOfFive", measure.getGroups().get(0).getPopulations().get(0).getDefinition());

    Group persistedGroup = groupService.createOrUpdateGroup(group1, measure.getId(), "test.user");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    assertEquals(group1.getId(), persistedGroup.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getGroups());
    assertEquals(1, savedMeasure.getGroups().size());
    Group capturedGroup = savedMeasure.getGroups().get(0);
    // after update
    assertEquals("Initial Population", capturedGroup.getPopulations().get(0).getDefinition());
    assertEquals("Description", capturedGroup.getGroupDescription());
    assertNotEquals("test-scoring-unit", capturedGroup.getScoringUnit());
    assertEquals("new scoring unit", capturedGroup.getScoringUnit());
  }

  @Test
  public void testUpdateGroupWhenPopulationDefinitionReturnTypeNotMatchingWithPopulationBasis() {
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    assertThrows(
        InvalidReturnTypeException.class,
        () -> groupService.createOrUpdateGroup(group2, measure.getId(), "test.user"));
  }

  @Test
  public void testUpdateGroupWhenPopulationFunctionReturnTypeNotMatchingWithPopulationBasis() {
    group2.setPopulations(null);
    group2.setPopulationBasis("Boolean");
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    assertThrows(
        InvalidReturnTypeException.class,
        () -> groupService.createOrUpdateGroup(group2, measure.getId(), "test.user"));
  }

  @Test
  public void testUpdateGroupWhenElmJsonIsInvalid() {
    measure.setElmJson("UnpardonableElmJson");
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    assertThrows(
        IllegalArgumentException.class,
        () -> groupService.createOrUpdateGroup(group2, measure.getId(), "test.user"));
  }

  @Test
  public void testUpdateGroupWhenElmJsonIsNull() {
    measure.setElmJson(null);
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    assertThrows(
        IllegalArgumentException.class,
        () -> groupService.createOrUpdateGroup(group2, measure.getId(), "test.user"));
  }

  @Test
  public void testUpdateGroupWithValidStratification() {
    group2.setPopulations(null);
    Optional<Measure> optional = Optional.of(measure);
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    doReturn(optional).when(measureRepository).findById(any(String.class));
    doReturn(measure).when(measureRepository).save(any(Measure.class));

    Group group = groupService.createOrUpdateGroup(group2, measure.getId(), "test.user");
    assertEquals(group.getStratifications().size(), group2.getStratifications().size());
    verify(measureRepository, times(1)).save(measureCaptor.capture());
  }

  @Test
  public void testUpdateGroupWhenPopulationFunctionReturnTypeMatchingWithPopulationBasis() {
    group2.setPopulations(null);
    Optional<Measure> optional = Optional.of(measure);
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    doReturn(optional).when(measureRepository).findById(any(String.class));
    doReturn(measure).when(measureRepository).save(any(Measure.class));

    Group group = groupService.createOrUpdateGroup(group2, measure.getId(), "test.user");
    assertEquals(group.getMeasureObservations().size(), group2.getMeasureObservations().size());
    verify(measureRepository, times(1)).save(measureCaptor.capture());
  }

  @Test
  void testUpdateGroupReturnsExceptionForResourceNotFound() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> groupService.createOrUpdateGroup(group2, "testid", "test.user"));
  }

  @Test
  public void testCreateGroupWithEmptyElm() {
    group2.setPopulations(null);
    group2.setPopulationBasis("Boolean");
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    measure.setElmJson("");
    assertThrows(
        IllegalArgumentException.class,
        () -> groupService.createOrUpdateGroup(group2, measure.getId(), "test.user"));
  }

  @Test
  public void testUpdateGroupWithNoFunctions() {
    measure.setElmJson(getData("/test_elm_no_functions.json"));
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    Group group = groupService.createOrUpdateGroup(group1, measure.getId(), "test.user");
    assertNotNull(group);
  }

  @Test
  public void testUpdateGroupWithBooleanAsOperandTypeSpecifierName() {
    group2.setMeasureObservations(
        List.of(
            new MeasureObservation(
                "id-1", "fun23", "id-2", AggregateMethodType.MAXIMUM.getValue())));
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));
    assertThrows(
        InvalidReturnTypeException.class,
        () -> groupService.createOrUpdateGroup(group2, measure.getId(), "test.user"));
  }

  @Test
  public void testUpdateGroupWithStratificationWhenReturnTypeNotEqualToPopulationBasis() {
    group2.setPopulations(null);
    // non-boolean define for strat cql definition
    group2.getStratifications().get(1).setCqlDefinition("SDE Race");
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));
    assertThrows(
        InvalidReturnTypeException.class,
        () -> groupService.createOrUpdateGroup(group2, measure.getId(), "test.user"));
  }

  @Test
  public void updateTestCaseGroupToAddMeasurePopulationsAndStratification() {
    // measure group with 4 populations and 2 stratification
    Group measureGroup = ratioGroup;
    TestCaseGroupPopulation testCaseGroup = buildTestCaseRatioGroup();
    // no testcase stratification
    testCaseGroup.setStratificationValues(null);

    // before updates
    assertEquals(5, testCaseGroup.getPopulationValues().size());
    assertNull(testCaseGroup.getStratificationValues());
    groupService.updateTestCaseGroupWithMeasureGroup(testCaseGroup, measureGroup);
    // after updates
    assertEquals(6, testCaseGroup.getPopulationValues().size());
    assertEquals(2, testCaseGroup.getStratificationValues().size());
  }

  @Test
  public void updateTestCaseGroupToRemoveMeasurePopulationsAndStratification() {
    // measure group with 4 populations and 2 stratification
    Group measureGroup = ratioGroup;

    // test case group with 3 populations, 2 observations and 2 stratification
    TestCaseGroupPopulation testCaseGroup = buildTestCaseRatioGroup();
    // add one more optional population so that it can be removed
    testCaseGroup
        .getPopulationValues()
        .add(buildTestCasePopulation("id-3", PopulationType.DENOMINATOR_EXCLUSION));

    // before updates
    assertEquals(6, testCaseGroup.getPopulationValues().size());
    assertEquals(2, testCaseGroup.getStratificationValues().size());

    // unselect 1 population and 1 stratification from measure group
    measureGroup.getPopulations().get(2).setDefinition(null);
    measureGroup.getStratifications().get(1).setCqlDefinition(null);
    groupService.updateTestCaseGroupWithMeasureGroup(testCaseGroup, measureGroup);
    // after updates
    assertEquals(5, testCaseGroup.getPopulationValues().size());
    assertEquals(1, testCaseGroup.getStratificationValues().size());
    // removed population DENOMINATOR_EXCLUSION is no longer in testCaseGroup
    TestCasePopulationValue testPopulation =
        testCaseGroup.getPopulationValues().stream()
            .filter(
                p -> StringUtils.equals(p.getId(), measureGroup.getPopulations().get(2).getId()))
            .findAny()
            .orElse(null);
    assertNull(testPopulation);
    // removed stratification is no longer in testCaseGroup
    TestCaseStratificationValue testStrata =
        testCaseGroup.getStratificationValues().stream()
            .filter(
                s ->
                    StringUtils.equals(s.getId(), measureGroup.getStratifications().get(1).getId()))
            .findAny()
            .orElse(null);
    assertNull(testStrata);
  }

  @Test
  public void updateTestCaseGroupToModifyObservations() {
    // measure group with 4 populations and 2 stratification
    Group measureGroup =
        Group.builder()
            .scoring("Ratio")
            .populationBasis("Encounter")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null),
                    new Population("id-2", PopulationType.DENOMINATOR, "Denominator", null, null),
                    new Population(
                        "id-3", PopulationType.DENOMINATOR_EXCLUSION, "Denominator", null, null),
                    new Population("id-4", PopulationType.NUMERATOR, "Numerator", null, null),
                    new Population("id-4", PopulationType.NUMERATOR_EXCLUSION, "", null, null)))
            .measureObservations(
                new ArrayList<>(
                    List.of(
                        new MeasureObservation(
                            "mo-id-1",
                            "Denominator MO",
                            "id-2",
                            AggregateMethodType.MAXIMUM.getValue()),
                        new MeasureObservation(
                            "mo-id-2",
                            "Numerator MO",
                            "id-4",
                            AggregateMethodType.MAXIMUM.getValue()))))
            .stratifications(List.of(strata1))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();

    // test case group with 3 populations, 2 observations and 2 stratification
    TestCaseGroupPopulation testCaseGroup = buildTestCaseRatioGroup();
    // add one more optional population so that it can be removed
    testCaseGroup
        .getPopulationValues()
        .add(buildTestCasePopulation("id-3", PopulationType.DENOMINATOR_EXCLUSION));

    // before updates
    assertEquals(6, testCaseGroup.getPopulationValues().size());
    assertEquals(2, testCaseGroup.getStratificationValues().size());

    // unselect 1 population and 1 stratification from measure group
    measureGroup.getPopulations().get(2).setDefinition(null);
    measureGroup.getStratifications().get(0).setCqlDefinition(null);
    groupService.updateTestCaseGroupWithMeasureGroup(testCaseGroup, measureGroup);
    // after updates
    assertEquals(5, testCaseGroup.getPopulationValues().size());
    assertEquals(0, testCaseGroup.getStratificationValues().size());
    // removed population DENOMINATOR_EXCLUSION is no longer in testCaseGroup
    TestCasePopulationValue testPopulation =
        testCaseGroup.getPopulationValues().stream()
            .filter(
                p -> StringUtils.equals(p.getId(), measureGroup.getPopulations().get(2).getId()))
            .findAny()
            .orElse(null);
    assertNull(testPopulation);
    // removed stratification is no longer in testCaseGroup
    TestCaseStratificationValue testStrata =
        testCaseGroup.getStratificationValues().stream()
            .filter(
                s ->
                    StringUtils.equals(s.getId(), measureGroup.getStratifications().get(1).getId()))
            .findAny()
            .orElse(null);
    assertNull(testStrata);
  }

  @Test
  public void updateTestCaseGroupToRemoveAllStratificationAndAnObservations() {
    // measure group with 4 populations, 2 observations and 2 stratification
    Group measureGroup = ratioGroup;
    TestCaseGroupPopulation testCaseGroup = buildTestCaseRatioGroup();

    // before updates
    assertEquals(5, testCaseGroup.getPopulationValues().size());
    assertEquals(2, testCaseGroup.getStratificationValues().size());

    // update measure group to remove stratification and observations
    measureGroup.setStratifications(null);
    measureGroup.setMeasureObservations(null);
    groupService.updateTestCaseGroupWithMeasureGroup(testCaseGroup, measureGroup);
    // after updates
    assertEquals(4, testCaseGroup.getPopulationValues().size());
    assertEquals(0, testCaseGroup.getStratificationValues().size());
  }

  @Test
  public void updateTestCaseGroupToRemoveDenominatorObservation() {
    // measure group with 4 populations and 2 stratification
    Group measureGroup = ratioGroup;
    TestCaseGroupPopulation testCaseGroup = buildTestCaseRatioGroup();
    // before updates
    assertEquals(5, testCaseGroup.getPopulationValues().size());
    // remove denominator observation from measure group
    measureGroup.getMeasureObservations().remove(0);
    groupService.updateTestCaseGroupWithMeasureGroup(testCaseGroup, measureGroup);
    // after updates
    assertEquals(5, testCaseGroup.getPopulationValues().size());
  }

  @Test
  public void updateTestCaseGroupToRemoveNumeratorObservation() {
    // measure group with 4 populations and 2 stratification
    Group measureGroup = ratioGroup;
    TestCaseGroupPopulation testCaseGroup = buildTestCaseRatioGroup();
    // before updates
    assertEquals(5, testCaseGroup.getPopulationValues().size());
    // remove denominator observation from measure group
    measureGroup.getMeasureObservations().remove(1);
    groupService.updateTestCaseGroupWithMeasureGroup(testCaseGroup, measureGroup);
    // after updates
    assertEquals(5, testCaseGroup.getPopulationValues().size());
  }

  @Test
  public void testCVObservationsAreUnchangedOnPopulationChange() {
    group2.setStratifications(null);
    TestCaseGroupPopulation testCaseGroup = buildTestCaseCVGroup();
    // before updates
    assertEquals(4, testCaseGroup.getPopulationValues().size());
    groupService.updateTestCaseGroupWithMeasureGroup(testCaseGroup, group2);
    // after updates
    assertEquals(4, testCaseGroup.getPopulationValues().size());
  }

  @Test
  public void testRemoveGroupFromTestCases() {
    TestCaseGroupPopulation cvGroup = buildTestCaseCVGroup();
    TestCaseGroupPopulation ratioGroup = buildTestCaseRatioGroup();
    ratioGroup.setGroupId("id-2");
    List<TestCase> testCases =
        List.of(TestCase.builder().groupPopulations(List.of(cvGroup, ratioGroup)).build());
    measure.setTestCases(testCases);
    // before removal
    assertEquals(2, testCases.get(0).getGroupPopulations().size());
    groupService.removeGroupFromTestCases(ratioGroup.getGroupId(), testCases);
    // after removal
    assertEquals(1, testCases.get(0).getGroupPopulations().size());
    assertEquals(cvGroup.getGroupId(), testCases.get(0).getGroupPopulations().get(0).getGroupId());
  }

  @Test
  public void testRemoveGroupFromTestCasesNoGroupIdOrTestCaseProvided() {
    TestCaseGroupPopulation cvGroup = buildTestCaseCVGroup();
    TestCaseGroupPopulation ratioGroup = buildTestCaseRatioGroup();
    ratioGroup.setGroupId("id-2");
    List<TestCase> testCases =
        List.of(TestCase.builder().groupPopulations(List.of(cvGroup, ratioGroup)).build());
    measure.setTestCases(testCases);
    // before removal
    assertEquals(2, testCases.get(0).getGroupPopulations().size());
    groupService.removeGroupFromTestCases(null, List.of());
    // after removal
    assertEquals(2, testCases.get(0).getGroupPopulations().size());
  }

  private TestCaseGroupPopulation buildTestCaseRatioGroup() {
    List<TestCasePopulationValue> populations =
        List.of(
            buildTestCasePopulation("id-1", PopulationType.INITIAL_POPULATION),
            buildTestCasePopulation("id-2", PopulationType.DENOMINATOR),
            buildTestCasePopulation("id-4", PopulationType.NUMERATOR),
            buildTestCasePopulation(
                "denominatorObservation1", PopulationType.DENOMINATOR_OBSERVATION),
            buildTestCasePopulation("numeratorObservation1", PopulationType.NUMERATOR_OBSERVATION));
    TestCaseStratificationValue testStrata1 = buildTestCaseStrata();
    testStrata1.setId(strata1.getId());
    TestCaseStratificationValue testStrata2 = buildTestCaseStrata();
    testStrata2.setId(strata1.getId());
    List<TestCaseStratificationValue> stratification =
        new ArrayList<>(List.of(testStrata1, testStrata2));
    // test case group with 3 populations and 2 stratification
    return buildTestCaseGroup(populations, stratification);
  }

  private TestCaseGroupPopulation buildTestCaseCVGroup() {
    List<TestCasePopulationValue> populations =
        List.of(
            buildTestCasePopulation("id-1", PopulationType.INITIAL_POPULATION),
            buildTestCasePopulation("id-2", PopulationType.MEASURE_POPULATION),
            buildTestCasePopulation("id-4", PopulationType.MEASURE_OBSERVATION),
            buildTestCasePopulation("id-5", PopulationType.MEASURE_OBSERVATION));
    // test case group with 3 populations and no stratification
    return buildTestCaseGroup(populations, null);
  }

  private TestCaseGroupPopulation buildTestCaseGroup(
      List<TestCasePopulationValue> populations, List<TestCaseStratificationValue> stratification) {
    // test case group with 3 populations and 2 stratification
    return TestCaseGroupPopulation.builder()
        .groupId("group-1")
        .scoring(MeasureScoring.RATIO.toString())
        .populationBasis("boolean")
        .populationValues(new ArrayList<>(populations))
        .stratificationValues(stratification)
        .build();
  }

  private TestCaseStratificationValue buildTestCaseStrata() {
    return TestCaseStratificationValue.builder().expected(null).actual(null).build();
  }

  private TestCasePopulationValue buildTestCasePopulation(
      String id, PopulationType populationType) {
    return TestCasePopulationValue.builder().id(id).name(populationType).expected(true).build();
  }
}
