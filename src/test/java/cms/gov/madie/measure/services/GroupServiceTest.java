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

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest implements ResourceUtil {
  @Mock private MeasureRepository measureRepository;

  @Mock private FhirServicesClient fhirServicesClient;

  @Mock private ElmTranslatorClient elmTranslatorClient;

  @InjectMocks private GroupService groupService;

  private Group group1;
  private Group group2;
  private Group group3;
  private Measure measure;

  @BeforeEach
  public void setUp() {
    Stratification strat1 = new Stratification();
    strat1.setId("strat-1");
    strat1.setCqlDefinition("Initial Population");
    strat1.setAssociation("Initial Population");
    Stratification strat2 = new Stratification();
    strat2.setCqlDefinition("denominator_define");
    strat2.setAssociation("Denom");

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
            .populations(
                List.of(
                    new Population(
                        "id-1", PopulationType.INITIAL_POPULATION, "FactorialOfFive", null, null)))
            .stratifications(List.of(strat1, emptyStrat))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();

    group3 =
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
                    new Population("id-12", PopulationType.DENOMINATOR_EXCLUSION, "", null, null)))
            .measureObservations(
                List.of(
                    new MeasureObservation(
                        "mo-id-1", "ipp", "id-1", AggregateMethodType.MAXIMUM.getValue())))
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
            .version("0.001")
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

    Group persistedGroup = groupService.createOrUpdateGroup(group3, measure.getId(), "test.user");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    assertEquals(group3.getId(), persistedGroup.getId());
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
    assertEquals(1, capturedGroup.getMeasureObservations().size());
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
    assertFalse(savedMeasure.getTestCases().get(0).getGroupPopulations().isEmpty());
    assertEquals(1, savedMeasure.getTestCases().get(0).getGroupPopulations().size());
    TestCaseGroupPopulation outputGroupPopulation =
        savedMeasure.getTestCases().get(0).getGroupPopulations().get(0);
    assertEquals("Cohort", outputGroupPopulation.getScoring());
    assertNotNull(outputGroupPopulation.getPopulationValues());
    assertEquals(1, outputGroupPopulation.getPopulationValues().size());
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
    final List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .groupPopulations(
                    List.of(TestCaseGroupPopulation.builder().groupId("Group1_ID").build()))
                .build());

    List<TestCase> output = groupService.setPopulationValuesForGroup(null, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testResetGroupPopulationValuesForGroupHandlesNullGroupId() {
    final Group group = Group.builder().id(null).scoring("Cohort").build();
    final List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .groupPopulations(
                    List.of(TestCaseGroupPopulation.builder().groupId("Group1_ID").build()))
                .build());

    List<TestCase> output = groupService.setPopulationValuesForGroup(group, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testResetGroupPopulationValuesForGroupHandlesEmptyGroupId() {
    final Group group = Group.builder().id("").scoring("Cohort").build();
    final List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .groupPopulations(
                    List.of(TestCaseGroupPopulation.builder().groupId("Group1_ID").build()))
                .build());

    List<TestCase> output = groupService.setPopulationValuesForGroup(group, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testResetGroupPopulationValuesForGroupHandlesNullGroupScoring() {
    final Group group = Group.builder().id("Group1_ID").scoring(null).build();
    final List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .groupPopulations(
                    List.of(TestCaseGroupPopulation.builder().groupId("Group1_ID").build()))
                .build());

    List<TestCase> output = groupService.setPopulationValuesForGroup(group, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testResetGroupPopulationValuesForGroupHandlesEmptyGroupScoring() {
    final Group group = Group.builder().id("Group1_ID").scoring("").build();
    final List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .groupPopulations(
                    List.of(TestCaseGroupPopulation.builder().groupId("Group1_ID").build()))
                .build());

    List<TestCase> output = groupService.setPopulationValuesForGroup(group, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testResetPopulationValuesForGroupHandlesNullTestCaseList() {
    final Group group = Group.builder().id("Group1_ID").scoring("Cohort").build();

    List<TestCase> output = groupService.setPopulationValuesForGroup(group, null);
    assertNull(output);
  }

  @Test
  public void testResetPopulationValuesForGroupHandlesEmptyTestCaseList() {
    final Group group = Group.builder().id("Group1_ID").scoring("Cohort").build();
    final List<TestCase> testCases = List.of();

    List<TestCase> output = groupService.setPopulationValuesForGroup(group, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testResetPopulationValuesForGroupHandlesNullGroupPopulationList() {
    final Group group = Group.builder().id("Group2_ID").scoring("Cohort").build();
    final List<TestCase> testCases = List.of(TestCase.builder().groupPopulations(null).build());

    List<TestCase> output = groupService.setPopulationValuesForGroup(group, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testResetPopulationValuesForGroupHandlesNonMatchingID() {
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

    List<TestCase> output = groupService.setPopulationValuesForGroup(group, testCases);
    assertEquals(testCases, output);
  }

  @Test
  public void testResetPopulationValuesForGroupHandlesMatchingID() {
    final Group group = Group.builder().id("Group2_ID").scoring("Cohort").build();
    TestCaseGroupPopulation testCaseGroupPopulation1 =
        TestCaseGroupPopulation.builder()
            .groupId("Group1_ID")
            .scoring(MeasureScoring.COHORT.toString())
            .populationValues(
                List.of(
                    TestCasePopulationValue.builder()
                        .name(PopulationType.INITIAL_POPULATION)
                        .expected(true)
                        .build()))
            .build();

    final TestCaseGroupPopulation testCaseGroupPopulation2 =
        TestCaseGroupPopulation.builder()
            .groupId("Group2_ID")
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
        List.of(
            TestCase.builder()
                .groupPopulations(List.of(testCaseGroupPopulation1, testCaseGroupPopulation2))
                .build());

    List<TestCase> output = groupService.setPopulationValuesForGroup(group, testCases);
    assertNotNull(output);
    assertNotEquals(testCases, output);
    assertEquals(1, output.size());
    assertNotNull(output.get(0));
    assertNotNull(output.get(0).getGroupPopulations());
    assertEquals(2, output.get(0).getGroupPopulations().size());
    assertEquals(testCaseGroupPopulation1, output.get(0).getGroupPopulations().get(0));
    assertNotNull(output.get(0).getGroupPopulations().get(1).getPopulationValues());
    assertEquals("Cohort", output.get(0).getGroupPopulations().get(1).getScoring());
    assertEquals(0, output.get(0).getGroupPopulations().get(1).getPopulationValues().size());
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
  public void testUpdateGroupWhenElmJsonIsInvalid() {
    measure.setElmJson("UnpardonableElmJson");
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    assertThrows(
        InvalidIdException.class,
        () -> groupService.createOrUpdateGroup(group2, measure.getId(), "test.user"));
  }

  @Test
  public void testUpdateGroupWhenElmJsonIsNull() {
    measure.setElmJson(null);
    Optional<Measure> optional = Optional.of(measure);
    doReturn(optional).when(measureRepository).findById(any(String.class));

    assertThrows(
        InvalidIdException.class,
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
}
