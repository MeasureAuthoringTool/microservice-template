package cms.gov.madie.measure.service;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.models.TestCaseWrapper;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.TestCaseService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TestCaseServiceTest {
  @Mock private MeasureRepository repository;

  @InjectMocks private TestCaseService testCaseService;

  private TestCase testCase;
  private Measure measure;

  @BeforeEach
  public void setUp() {
    testCase = new TestCase();
    testCase.setId("TESTID");
    testCase.setName("IPPPass");
    testCase.setSeries("BloodPressure>124");
    testCase.setCreatedBy("TestUser");
    testCase.setLastModifiedBy("TestUser2");

    measure = new Measure();
    measure.setId(ObjectId.get().toString());
    measure.setMeasureSetId("IDIDID");
    measure.setMeasureName("MSR01");
    measure.setVersion("0.001");
  }

  @Test
  public void testPersistTestCase() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(repository).findById(any(String.class));

    Mockito.doReturn(measure).when(repository).save(any(Measure.class));

//    TestCase persistTestCase =
//        testCaseService.persistTestCase(testCase, measure.getId(), "test.user");
    TestCaseWrapper testCaseWrapper = testCaseService.persistTestCase(testCase, measure.getId(), "test.user");
    assertNotNull(testCaseWrapper);
    TestCase persistTestCase = testCaseWrapper.getTestCase();
    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(testCase.getId(), persistTestCase.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());
    TestCase capturedTestCase = savedMeasure.getTestCases().get(0);
    int lastModCompareTo =
        capturedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user", capturedTestCase.getLastModifiedBy());
    assertEquals("test.user", capturedTestCase.getCreatedBy());
    assertEquals(1, lastModCompareTo);
    assertEquals(capturedTestCase.getLastModifiedAt(), capturedTestCase.getCreatedAt());
  }

  @Test
  public void testFindTestCasesByMeasureId() {
    measure.setTestCases(List.of(testCase));
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(repository).findById(any(String.class));
    List<TestCase> persistTestCase = testCaseService.findTestCasesByMeasureId(measure.getId());
    assertEquals(1, persistTestCase.size());
    assertEquals(testCase.getId(), persistTestCase.get(0).getId());
  }

  @Test
  public void testFindTestCasesByMeasureIdWhenMeasureDoesNotExist() {
    Optional<Measure> optional = Optional.empty();
    Mockito.doReturn(optional).when(repository).findById(any(String.class));
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.findTestCasesByMeasureId(measure.getId()));
  }

  @Test
  public void testFindTestCaseSeriesByMeasureIdThrowsExceptionWhenMeasureDoesNotExist() {
    Optional<Measure> optional = Optional.empty();
    when(repository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.findTestCaseSeriesByMeasureId(measure.getId()));
  }

  @Test
  public void testFindTestCaseSeriesByMeasureIdReturnsEmptyListWhenTestCasesNull() {
    Measure noTestCases = measure.toBuilder().build();
    measure.setTestCases(null);
    Optional<Measure> optional = Optional.of(noTestCases);
    when(repository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
    List<String> output = testCaseService.findTestCaseSeriesByMeasureId(measure.getId());
    assertEquals(List.of(), output);
  }

  @Test
  public void testFindTestCaseSeriesByMeasureIdReturnsEmptyListWhenTestCasesEmpty() {
    Measure noTestCases = measure.toBuilder().build();
    measure.setTestCases(new ArrayList<>());
    Optional<Measure> optional = Optional.of(noTestCases);
    when(repository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
    List<String> output = testCaseService.findTestCaseSeriesByMeasureId(measure.getId());
    assertEquals(List.of(), output);
  }

  @Test
  public void testFindTestCaseSeriesByMeasureIdReturnsDistinctList() {
    Measure withTestCases = measure.toBuilder().build();
    withTestCases.setTestCases(
        List.of(
            TestCase.builder().id(ObjectId.get().toString()).series("SeriesAAA").build(),
            TestCase.builder().id(ObjectId.get().toString()).series("SeriesAAA").build(),
            TestCase.builder().id(ObjectId.get().toString()).series("SeriesBBB").build()));
    Optional<Measure> optional = Optional.of(withTestCases);
    when(repository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
    List<String> output = testCaseService.findTestCaseSeriesByMeasureId(measure.getId());
    assertEquals(List.of("SeriesAAA", "SeriesBBB"), output);
  }

  @Test
  public void testFindTestCaseSeriesByMeasureIdReturnsListWithoutNullsAndEmptyStrings() {
    Measure withTestCases = measure.toBuilder().build();
    withTestCases.setTestCases(
        List.of(
            TestCase.builder().id(ObjectId.get().toString()).series("SeriesAAA").build(),
            TestCase.builder().id(ObjectId.get().toString()).series("").build(),
            TestCase.builder().id(ObjectId.get().toString()).series(null).build(),
            TestCase.builder().id(ObjectId.get().toString()).series("SeriesBBB").build()));
    Optional<Measure> optional = Optional.of(withTestCases);
    when(repository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
    List<String> output = testCaseService.findTestCaseSeriesByMeasureId(measure.getId());
    assertEquals(List.of("SeriesAAA", "SeriesBBB"), output);
  }

  @Test
  public void testUpdateTestCaseUpdatesLastModifiedFields() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Instant createdAt = Instant.now().minus(300, ChronoUnit.SECONDS);
    TestCase originalTestCase =
        testCase
            .toBuilder()
            .createdAt(createdAt)
            .createdBy("test.user5")
            .lastModifiedAt(createdAt)
            .lastModifiedBy("test.user5")
            .build();
    List<TestCase> testCases = new ArrayList<>();
    testCases.add(originalTestCase);
    Measure originalMeasure = measure.toBuilder().testCases(testCases).build();
    Optional<Measure> optional = Optional.of(originalMeasure);
    Mockito.doReturn(optional).when(repository).findById(any(String.class));

    TestCase updatingTestCase =
        testCase.toBuilder().title("UpdatedTitle").series("UpdatedSeries").build();
    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));

    TestCaseWrapper updatedTestCaseWrapper =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user");
    assertNotNull(updatedTestCaseWrapper);
    TestCase updatedTestCase = updatedTestCaseWrapper.getTestCase();
    assertNotNull(updatedTestCase);
    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(updatingTestCase.getId(), updatedTestCase.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());
    assertEquals(updatedTestCase, savedMeasure.getTestCases().get(0));

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user", updatedTestCase.getLastModifiedBy());
    assertEquals(originalTestCase.getCreatedBy(), updatedTestCase.getCreatedBy());
    assertEquals(1, lastModCompareTo);
    assertNotEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user5", updatedTestCase.getCreatedBy());
  }

  @Test
  public void testUpdateTestCasePreventsModificationOfCreatedByFields() {
    Instant createdAt = Instant.now().minus(300, ChronoUnit.SECONDS);
    TestCase originalTestCase =
        testCase
            .toBuilder()
            .createdAt(createdAt)
            .createdBy("test.user5")
            .lastModifiedAt(createdAt)
            .lastModifiedBy("test.user5")
            .build();
    List<TestCase> testCases = new ArrayList<>();
    testCases.add(originalTestCase);
    Measure originalMeasure = measure.toBuilder().testCases(testCases).build();
    Mockito.doReturn(Optional.of(originalMeasure)).when(repository).findById(any(String.class));

    TestCase updatingTestCase =
        testCase
            .toBuilder()
            .createdBy("Nobody")
            .createdAt(Instant.now())
            .title("UpdatedTitle")
            .series("UpdatedSeries")
            .build();
    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));

    TestCaseWrapper updatedTestCaseWrapper =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user");

    TestCase updatedTestCase = updatedTestCaseWrapper.getTestCase();
    assertNotNull(updatedTestCase);
    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user", updatedTestCase.getLastModifiedBy());
    assertEquals(1, lastModCompareTo);
    assertNotEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals(originalTestCase.getCreatedAt(), updatedTestCase.getCreatedAt());
    assertEquals(originalTestCase.getCreatedBy(), updatedTestCase.getCreatedBy());
  }

  @Test
  public void testThatUpdateTestCaseHandlesUpsertForNullTestCasesList() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Measure originalMeasure = measure.toBuilder().testCases(null).build();
    Mockito.doReturn(Optional.of(originalMeasure)).when(repository).findById(any(String.class));

    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));

    TestCase upsertingTestCase =
        testCase
            .toBuilder()
            .createdBy("Nobody")
            .createdAt(Instant.now())
            .title("UpdatedTitle")
            .series("UpdatedSeries")
            .build();

    TestCaseWrapper updatedTestCaseWrapper =
        testCaseService.updateTestCase(upsertingTestCase, measure.getId(), "test.user");

    TestCase updatedTestCase = updatedTestCaseWrapper.getTestCase();
    assertNotNull(updatedTestCase);
    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals(1, lastModCompareTo);
    assertNotNull(updatedTestCase.getId());
    assertEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user", updatedTestCase.getCreatedBy());
    assertEquals("test.user", updatedTestCase.getLastModifiedBy());

    verify(repository, times(1)).save(measureCaptor.capture());
    Measure savedMeasure = measureCaptor.getValue();
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());
    assertEquals(updatedTestCase, savedMeasure.getTestCases().get(0));
  }

  @Test
  public void testThatUpdateTestCaseHandlesUpsertForEmptyTestCasesList() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Measure originalMeasure = measure.toBuilder().testCases(new ArrayList<>()).build();
    Mockito.doReturn(Optional.of(originalMeasure)).when(repository).findById(any(String.class));

    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));

    TestCase upsertingTestCase =
        testCase
            .toBuilder()
            .createdBy("Nobody")
            .createdAt(Instant.now())
            .title("UpdatedTitle")
            .series("UpdatedSeries")
            .build();

    TestCaseWrapper updatedTestCaseWrapper =
        testCaseService.updateTestCase(upsertingTestCase, measure.getId(), "test.user");

    TestCase updatedTestCase = updatedTestCaseWrapper.getTestCase();
    assertNotNull(updatedTestCase);
    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals(1, lastModCompareTo);
    assertNotNull(updatedTestCase.getId());
    assertEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user", updatedTestCase.getCreatedBy());
    assertEquals("test.user", updatedTestCase.getLastModifiedBy());

    verify(repository, times(1)).save(measureCaptor.capture());
    Measure savedMeasure = measureCaptor.getValue();
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());
    assertEquals(updatedTestCase, savedMeasure.getTestCases().get(0));
  }

  @Test
  public void testThatUpdateTestCaseHandlesUpsertWithOtherExistingTestCases() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    TestCase otherExistingTC =
        TestCase.builder().id("TC1_ID").title("TC1").series("Series1").build();
    Measure originalMeasure =
        measure.toBuilder().testCases(new ArrayList<>(Arrays.asList(otherExistingTC))).build();
    Mockito.doReturn(Optional.of(originalMeasure)).when(repository).findById(any(String.class));

    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));

    TestCase upsertingTestCase =
        testCase
            .toBuilder()
            .createdBy("Nobody")
            .createdAt(Instant.now())
            .title("UpdatedTitle")
            .series("UpdatedSeries")
            .build();

    TestCaseWrapper updatedTestCaseWrapper =
        testCaseService.updateTestCase(upsertingTestCase, measure.getId(), "test.user");

    TestCase updatedTestCase = updatedTestCaseWrapper.getTestCase();
    assertNotNull(updatedTestCase);
    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals(1, lastModCompareTo);
    assertNotNull(updatedTestCase.getId());
    assertEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user", updatedTestCase.getCreatedBy());
    assertEquals("test.user", updatedTestCase.getLastModifiedBy());

    verify(repository, times(1)).save(measureCaptor.capture());
    Measure savedMeasure = measureCaptor.getValue();
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(2, savedMeasure.getTestCases().size());
    assertEquals(otherExistingTC, savedMeasure.getTestCases().get(0));
    assertEquals(updatedTestCase, savedMeasure.getTestCases().get(1));
  }
}
