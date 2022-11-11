package cms.gov.madie.measure.service;

import cms.gov.madie.measure.HapiFhirConfig;
import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.FhirServicesClient;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.*;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.TestCaseService;
import org.assertj.core.util.Lists;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestCaseServiceTest {
  @Mock private MeasureRepository repository;
  @Mock private HapiFhirConfig hapiFhirConfig;
  @Mock private RestTemplate hapiFhirRestTemplate;
  @Mock private ActionLogService actionLogService;

  @Spy private ObjectMapper mapper;

  @Mock private FhirServicesClient fhirServicesClient;

  @InjectMocks private TestCaseService testCaseService;

  @Captor private ArgumentCaptor<ActionType> actionTypeArgumentCaptor;
  @Captor private ArgumentCaptor<String> targetIdArgumentCaptor;
  @Captor private ArgumentCaptor<Class> targetClassArgumentCaptor;

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
    testCase.setJson("{\"resourceType\":\"Patient\"}");

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

    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(ResponseEntity.ok("{ \"code\": 200, \"successful\": true }"));

    TestCase persistTestCase =
        testCaseService.persistTestCase(testCase, measure.getId(), "test.user", "TOKEN");
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

    assertNotNull(persistTestCase.getHapiOperationOutcome());
    assertEquals(200, persistTestCase.getHapiOperationOutcome().getCode());

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            anyString());
    assertEquals(persistTestCase.getId(), targetIdArgumentCaptor.getValue());
    assertEquals(TestCase.class, targetClassArgumentCaptor.getValue());
    assertEquals(ActionType.CREATED, actionTypeArgumentCaptor.getValue());
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

    TestCase updatedTestCase =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user", "TOKEN");
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

    TestCase updatedTestCase =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user", "TOKEN");
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

    TestCase updatedTestCase =
        testCaseService.updateTestCase(upsertingTestCase, measure.getId(), "test.user", "TOKEN");
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

    TestCase updatedTestCase =
        testCaseService.updateTestCase(upsertingTestCase, measure.getId(), "test.user", "TOKEN");
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

    TestCase updatedTestCase =
        testCaseService.updateTestCase(upsertingTestCase, measure.getId(), "test.user", "TOKEN");
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

  @Test
  public void testGetTestCaseReturnsTestCaseById() {
    Optional<Measure> optional =
        Optional.of(measure.toBuilder().testCases(Arrays.asList(testCase)).build());
    Mockito.doReturn(optional).when(repository).findById(any(String.class));
    TestCase output =
        testCaseService.getTestCase(measure.getId(), testCase.getId(), false, "TOKEN");
    assertEquals(testCase, output);
  }

  @Test
  public void testGetTestCaseReturnsTestCaseByIdValidatesByUpsert() {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(ResponseEntity.ok("{ \"code\": 200, \"successful\": true }"));

    Optional<Measure> optional =
        Optional.of(measure.toBuilder().testCases(Arrays.asList(testCase)).build());
    Mockito.doReturn(optional).when(repository).findById(any(String.class));
    TestCase output = testCaseService.getTestCase(measure.getId(), testCase.getId(), true, "TOKEN");
    assertEquals(testCase, output);
    assertNotNull(output.getHapiOperationOutcome());
    assertEquals(200, output.getHapiOperationOutcome().getCode());
  }

  @Test
  public void testGetTestCaseThrowsNotFoundExceptionForMeasureWithEmptyListTestCases() {
    Mockito.doReturn(Optional.of(measure.toBuilder().testCases(Lists.emptyList()).build()))
        .when(repository)
        .findById(any(String.class));
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.getTestCase(measure.getId(), testCase.getId(), false, "TOKEN"));
  }

  @Test
  public void testGetTestCaseThrowsNotFoundExceptionForMeasureWithNullTestCases() {
    Mockito.doReturn(Optional.of(measure.toBuilder().testCases(null).build()))
        .when(repository)
        .findById(any(String.class));
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.getTestCase(measure.getId(), testCase.getId(), false, "TOKEN"));
  }

  @Test
  public void testGetTestCaseThrowsNotFoundExceptionForMeasureWithOtherTestCases() {
    List<TestCase> testCases =
        List.of(
            TestCase.builder().id("TC1_ID").title("TC1").build(),
            TestCase.builder().id("TC2_ID").title("TC2").build());
    Mockito.doReturn(Optional.of(measure.toBuilder().testCases(testCases).build()))
        .when(repository)
        .findById(any(String.class));
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.getTestCase(measure.getId(), testCase.getId(), false, "TOKEN"));
  }

  @Test
  void testDeleteTestCase() {
    List<TestCase> testCases =
        List.of(
            TestCase.builder().id("TC1_ID").title("TC1").build(),
            TestCase.builder().id("TC2_ID").title("TC2").build());

    Measure existingMeasure =
        Measure.builder().id("measure-id").createdBy("test.user").testCases(testCases).build();
    when(repository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    doReturn(existingMeasure).when(repository).save(any(Measure.class));

    List<TestCase> output = testCaseService.deleteTestCase("measure-id", "TC2_ID", "test.user");

    assertEquals(1, output.size());
  }

  @Test
  void testDeleteTestCaseReturnsExceptionForNullMeasureId() {
    assertThrows(
        InvalidIdException.class,
        () -> testCaseService.deleteTestCase("", "testCaseId", "OtherUser"));
  }

  @Test
  void testDeleteTestCaseReturnsExceptionForResourceNotFound() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.deleteTestCase("testid", "testCaseId", "user2"));
  }

  @Test
  void testDeleteTestCaseReturnsExceptionForNullTestCaseId() {
    assertThrows(
        InvalidIdException.class,
        () -> testCaseService.deleteTestCase("measure-id", "", "OtherUser"));
  }

  @Test
  void testDeleteTestCaseReturnsExceptionThrowsAccessException() {
    final Measure measure = Measure.builder().id("measure-id").createdBy("OtherUser").build();
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        UnauthorizedException.class,
        () -> testCaseService.deleteTestCase("measure-id", "testCaseId", "user2"));
  }

  @Test
  void testDeleteTestCasReturnsExceptionForGroupNotFoundInMeasure() {
    List<TestCase> testCases =
        List.of(
            TestCase.builder().id("TC1_ID").title("TC1").build(),
            TestCase.builder().id("TC2_ID").title("TC2").build());

    Measure existingMeasure =
        Measure.builder().id("measure-id").createdBy("test.user").testCases(testCases).build();
    when(repository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.deleteTestCase("measure-id", "testCaseId", "test.user"));
  }

  @Test
  void testDeleteTestCasReturnsExceptionForNullTestCasesinMeasure() {
    List<TestCase> testCases =
        List.of(
            TestCase.builder().id("TC1_ID").title("TC1").build(),
            TestCase.builder().id("TC2_ID").title("TC2").build());

    Measure existingMeasure =
        Measure.builder().id("measure-id").createdBy("test.user").testCases(null).build();
    when(repository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.deleteTestCase("measure-id", "testCaseId", "test.user"));
  }

  @Test
  public void testValidateTestCaseJsonHandlesNullTestCase() {
    HapiOperationOutcome output = testCaseService.validateTestCaseJson(null, "TOKEN");
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testValidateTestCaseJsonHandlesNullJson() {
    TestCase tc = new TestCase();
    tc.setJson(null);
    HapiOperationOutcome output = testCaseService.validateTestCaseJson(tc, "TOKEN");
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testValidateTestCaseJsonHandlesEmptyJson() {
    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("").build(), "TOKEN");
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testValidateTestCaseJsonHandlesWhitespaceJson() {
    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("   ").build(), "TOKEN");
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testValidateTestCaseJsonHandlesGenericException() {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenThrow(new RuntimeException("something bad happened!"));

    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("{}").build(), "TOKEN");
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(500)));
  }

  @Test
  public void testValidateTestCaseJsonHandlesNotFound() {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenThrow(
            new HttpClientErrorException(
                HttpStatus.NOT_FOUND, "path not found", "{}".getBytes(), Charset.defaultCharset()));

    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("{}").build(), "TOKEN");
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(404)));
  }

  @Test
  public void testValidateTestCaseJsonHandlesUnsupportedMediaType() {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenThrow(
            new HttpClientErrorException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type",
                "{}".getBytes(),
                Charset.defaultCharset()));

    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("{}").build(), "TOKEN");
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(415)));
  }

  @Test
  public void testValidateTestCaseJsonHandlesInternalServerError() {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenThrow(
            new HttpClientErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported Media Type"));

    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("{}").build(), "TOKEN");
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(500)));
  }

  @Test
  public void testValidateTestCaseJsonHandlesProcessingErrorDuringHttpClientException()
      throws JsonProcessingException {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenThrow(
            new HttpClientErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported Media Type"));
    doThrow(
            new JsonParseException(
                mapper.getDeserializationContext().getParser(), "Something bad happened!"))
        .when(mapper)
        .readValue(anyString(), any(Class.class));

    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("{}").build(), "TOKEN");
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(500)));
    assertThat(
        output.getMessage(),
        is(
            equalTo(
                "Unable to validate test case JSON due to errors, but outcome not able to be interpreted!")));
  }

  @Test
  public void testValidateTestCaseJsonHandlesProcessingErrorDuringGoodResponse()
      throws JsonProcessingException {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(
            ResponseEntity.ok(
                "{\n"
                    + "    \"code\": 200,\n"
                    + "    \"message\": null,\n"
                    + "    \"successful\": true,\n"
                    + "    \"outcomeResponse\": {\n"
                    + "        \"resourceType\": \"OperationOutcome\",\n"
                    + "        \"issue\": [\n"
                    + "            {\n"
                    + "                \"severity\": \"information\",\n"
                    + "                \"code\": \"informational\",\n"
                    + "                \"diagnostics\": \"No issues detected during validation\"\n"
                    + "            }\n"
                    + "        ]\n"
                    + "    }\n"
                    + "}"));
    doThrow(
            new JsonParseException(
                mapper.getDeserializationContext().getParser(), "Something bad happened!"))
        .when(mapper)
        .readValue(anyString(), any(Class.class));

    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("{}").build(), "TOKEN");
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(500)));
    assertThat(
        output.getMessage(),
        is(
            equalTo(
                "Unable to validate test case JSON due to errors, but outcome not able to be interpreted!")));
  }

  @Test
  public void testValidateTestCaseJsonHandlesGoodResponse() {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(
            ResponseEntity.ok(
                "{\n"
                    + "    \"code\": 200,\n"
                    + "    \"message\": null,\n"
                    + "    \"successful\": true,\n"
                    + "    \"outcomeResponse\": {\n"
                    + "        \"resourceType\": \"OperationOutcome\",\n"
                    + "        \"issue\": [\n"
                    + "            {\n"
                    + "                \"severity\": \"information\",\n"
                    + "                \"code\": \"informational\",\n"
                    + "                \"diagnostics\": \"No issues detected during validation\"\n"
                    + "            }\n"
                    + "        ]\n"
                    + "    }\n"
                    + "}"));

    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("{}").build(), "TOKEN");
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(200)));
    assertThat(output.getMessage(), is(nullValue()));
    assertThat(output.isSuccessful(), is(true));
  }
}
