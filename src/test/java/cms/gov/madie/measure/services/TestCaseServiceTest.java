package cms.gov.madie.measure.services;

import cms.gov.madie.measure.HapiFhirConfig;
import cms.gov.madie.measure.exceptions.InvalidDraftStatusException;
import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.*;
import gov.cms.madie.models.common.Version;
import cms.gov.madie.measure.repositories.MeasureRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
  @Mock private MeasureService measureService;

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
    measure.setCreatedBy("test.user5");
    measure.setId(ObjectId.get().toString());
    measure.setMeasureSetId("IDIDID");
    measure.setMeasureName("MSR01");
    measure.setVersion(new Version(0, 0, 1));
    measure.setMeasureMetaData(MeasureMetaData.builder().draft(true).build());

    ReflectionTestUtils.setField(testCaseService, "enforcePatientIdFeatureFlag", true);
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
  public void testPersistTestCaseWithExistingTestCases() {
    List<TestCase> existingTestCases = new ArrayList<>();
    TestCase existingTestCase = TestCase.builder().id("Test1ID").title("Test0").build();
    existingTestCases.add(existingTestCase);
    measure.setTestCases(existingTestCases);
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(repository).findById(any(String.class));

    Mockito.doReturn(measure).when(repository).save(any(Measure.class));

    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(ResponseEntity.ok("{ \"code\": 200, \"successful\": true }"));

    TestCase persistTestCase =
        testCaseService.persistTestCase(testCase, measure.getId(), "test.user", "TOKEN");
    assertThat(persistTestCase, is(notNullValue()));
    assertThat(persistTestCase.getId(), is(notNullValue()));
    assertThat(persistTestCase.getTitle(), is(equalTo(testCase.getTitle())));
    verify(repository, times(1)).save(measureCaptor.capture());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(2, savedMeasure.getTestCases().size());
    assertThat(savedMeasure.getTestCases().get(0), is(equalTo(existingTestCase)));
    TestCase capturedTestCase = savedMeasure.getTestCases().get(1);
    int lastModCompareTo =
        capturedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user", capturedTestCase.getLastModifiedBy());
    assertEquals("test.user", capturedTestCase.getCreatedBy());
    assertEquals(1, lastModCompareTo);
    assertEquals(capturedTestCase.getLastModifiedAt(), capturedTestCase.getCreatedAt());
  }

  @Test
  public void testEnrichNewTestCase() {
    TestCase testCase = new TestCase();
    final String username = "user01";
    TestCase output = testCaseService.enrichNewTestCase(testCase, username);
    assertThat(output, is(not(equalTo(testCase))));
    assertThat(output.getId(), is(notNullValue()));
    assertThat(output.getCreatedAt(), is(notNullValue()));
    assertThat(output.getCreatedBy(), is(equalTo(username)));
    assertThat(output.getLastModifiedAt(), is(notNullValue()));
    assertThat(output.getLastModifiedAt(), is(equalTo(output.getCreatedAt())));
    assertThat(output.getLastModifiedBy(), is(equalTo(username)));
    assertThat(output.getResourceUri(), is(nullValue()));
    assertThat(output.getHapiOperationOutcome(), is(nullValue()));
    assertThat(output.isValidResource(), is(false));
  }

  @Test
  public void testValidateTestCaseAsResource() throws JsonProcessingException {
    TestCase testCase =
        TestCase.builder()
            .id("TestID")
            .json("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}")
            .build();
    final String accessToken = "Bearer Token";

    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(ResponseEntity.ok("{}"));
    when(mapper.readValue("{}", HapiOperationOutcome.class))
        .thenReturn(HapiOperationOutcome.builder().code(200).successful(true).build());

    TestCase output = testCaseService.validateTestCaseAsResource(testCase, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getJson(), is(notNullValue()));
    assertThat(output.getHapiOperationOutcome(), is(notNullValue()));
    assertThat(output.getHapiOperationOutcome().getCode(), is(equalTo(200)));
  }

  @Test
  public void testValidateTestCaseAsResourceHandlesNullTestCase() {
    TestCase testCase = null;
    final String accessToken = "Bearer Token";

    TestCase output = testCaseService.validateTestCaseAsResource(testCase, accessToken);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testPersistTestCasesThrowsResourceNotFoundExceptionForUnknownId() {
    List<TestCase> newTestCases = List.of(TestCase.builder().title("Test1").build());
    String measureId = measure.getId();
    String username = "user01";
    String accessToken = "Bearer Token";
    when(repository.findById(anyString())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.persistTestCases(newTestCases, measureId, username, accessToken));
  }

  @Test
  public void testPersistTestCasesThrowsInvalidDraftStatusExceptionForNonDraftMeasure() {
    List<TestCase> newTestCases = List.of(TestCase.builder().title("Test1").build());
    String measureId = measure.getId();
    String username = "user01";
    String accessToken = "Bearer Token";
    measure.getMeasureMetaData().setDraft(false);
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));

    assertThrows(
        InvalidDraftStatusException.class,
        () -> testCaseService.persistTestCases(newTestCases, measureId, username, accessToken));
  }

  @Test
  public void testPersistTestCasesHandlesNullList() {
    List<TestCase> newTestCases = null;
    String measureId = measure.getId();
    String username = "user01";
    String accessToken = "Bearer Token";

    List<TestCase> output =
        testCaseService.persistTestCases(newTestCases, measureId, username, accessToken);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testPersistTestCasesHandlesEmptyList() {
    List<TestCase> newTestCases = List.of();
    String measureId = measure.getId();
    String username = "user01";
    String accessToken = "Bearer Token";

    List<TestCase> output =
        testCaseService.persistTestCases(newTestCases, measureId, username, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.isEmpty(), is(true));
  }

  @Test
  public void testPersistTestCasesHandlesListToMeasureNoExistingTestCases() {
    List<TestCase> newTestCases =
        List.of(
            TestCase.builder().title("Test1").build(), TestCase.builder().title("Test2").build());
    String measureId = measure.getId();
    String username = "user01";
    String accessToken = "Bearer Token";
    when(repository.findById(eq(measureId))).thenReturn(Optional.of(measure));

    List<TestCase> output =
        testCaseService.persistTestCases(newTestCases, measureId, username, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(2)));
    assertThat(output.get(0).getId(), is(notNullValue()));
    assertThat(output.get(0).getCreatedAt(), is(notNullValue()));
    assertThat(output.get(0).getCreatedBy(), is(equalTo("user01")));
    assertThat(output.get(0).getLastModifiedAt(), is(notNullValue()));
    assertThat(output.get(0).getLastModifiedBy(), is(equalTo("user01")));
    assertThat(output.get(0).getResourceUri(), is(nullValue()));
    assertThat(output.get(0).getHapiOperationOutcome(), is(nullValue()));
    assertThat(output.get(0).isValidResource(), is(false));
    assertThat(output.get(1).getId(), is(notNullValue()));
    assertThat(output.get(1).getCreatedAt(), is(notNullValue()));
    assertThat(output.get(1).getCreatedBy(), is(equalTo("user01")));
    assertThat(output.get(1).getLastModifiedAt(), is(notNullValue()));
    assertThat(output.get(1).getLastModifiedBy(), is(equalTo("user01")));
    assertThat(output.get(1).getResourceUri(), is(nullValue()));
    assertThat(output.get(1).getHapiOperationOutcome(), is(nullValue()));
    assertThat(output.get(1).isValidResource(), is(false));

    verifyNoInteractions(fhirServicesClient);
  }

  @Test
  public void testPersistTestCasesHandlesListToMeasureWithExistingTestCases() {
    List<TestCase> existingTestCases = new ArrayList<>();
    existingTestCases.add(TestCase.builder().id("Test1ID").title("Test0").build());
    measure.setTestCases(existingTestCases);
    List<TestCase> newTestCases =
        List.of(
            TestCase.builder().title("Test1").build(), TestCase.builder().title("Test2").build());
    String measureId = measure.getId();
    String username = "user01";
    String accessToken = "Bearer Token";
    when(repository.findById(eq(measureId))).thenReturn(Optional.of(measure));

    List<TestCase> output =
        testCaseService.persistTestCases(newTestCases, measureId, username, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(2)));
    assertThat(output.get(0).getId(), is(notNullValue()));
    assertThat(output.get(0).getCreatedAt(), is(notNullValue()));
    assertThat(output.get(0).getCreatedBy(), is(equalTo("user01")));
    assertThat(output.get(0).getLastModifiedAt(), is(notNullValue()));
    assertThat(output.get(0).getLastModifiedBy(), is(equalTo("user01")));
    assertThat(output.get(0).getResourceUri(), is(nullValue()));
    assertThat(output.get(0).getHapiOperationOutcome(), is(nullValue()));
    assertThat(output.get(0).isValidResource(), is(false));
    assertThat(output.get(1).getId(), is(notNullValue()));
    assertThat(output.get(1).getCreatedAt(), is(notNullValue()));
    assertThat(output.get(1).getCreatedBy(), is(equalTo("user01")));
    assertThat(output.get(1).getLastModifiedAt(), is(notNullValue()));
    assertThat(output.get(1).getLastModifiedBy(), is(equalTo("user01")));
    assertThat(output.get(1).getResourceUri(), is(nullValue()));
    assertThat(output.get(1).getHapiOperationOutcome(), is(nullValue()));
    assertThat(output.get(1).isValidResource(), is(false));

    verifyNoInteractions(fhirServicesClient);
  }

  @Test
  public void testPersistTestCasesHandlesListToMeasureWithJson() throws JsonProcessingException {
    List<TestCase> newTestCases =
        List.of(
            TestCase.builder()
                .title("Test1")
                .json("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}")
                .build(),
            TestCase.builder()
                .title("Test2")
                .json("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}")
                .build());
    String measureId = measure.getId();
    String username = "user01";
    String accessToken = "Bearer Token";
    when(repository.findById(eq(measureId))).thenReturn(Optional.of(measure));
    when(mapper.readValue("{}", HapiOperationOutcome.class))
        .thenReturn(HapiOperationOutcome.builder().code(200).successful(true).build())
        .thenReturn(HapiOperationOutcome.builder().code(400).successful(false).build());
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(ResponseEntity.ok("{}"));

    List<TestCase> output =
        testCaseService.persistTestCases(newTestCases, measureId, username, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(2)));
    assertThat(output.get(0).getId(), is(notNullValue()));
    assertThat(output.get(0).getCreatedAt(), is(notNullValue()));
    assertThat(output.get(0).getCreatedBy(), is(equalTo("user01")));
    assertThat(output.get(0).getLastModifiedAt(), is(notNullValue()));
    assertThat(output.get(0).getLastModifiedBy(), is(equalTo("user01")));
    assertThat(output.get(0).getResourceUri(), is(nullValue()));
    assertThat(output.get(0).getHapiOperationOutcome(), is(notNullValue()));
    assertThat(output.get(0).getHapiOperationOutcome().getCode(), is(equalTo(200)));
    assertThat(output.get(0).isValidResource(), is(true));
    assertThat(output.get(1).getId(), is(notNullValue()));
    assertThat(output.get(1).getCreatedAt(), is(notNullValue()));
    assertThat(output.get(1).getCreatedBy(), is(equalTo("user01")));
    assertThat(output.get(1).getLastModifiedAt(), is(notNullValue()));
    assertThat(output.get(1).getLastModifiedBy(), is(equalTo("user01")));
    assertThat(output.get(1).getResourceUri(), is(nullValue()));
    assertThat(output.get(1).getHapiOperationOutcome(), is(notNullValue()));
    assertThat(output.get(1).getHapiOperationOutcome().getCode(), is(equalTo(400)));
    assertThat(output.get(1).isValidResource(), is(false));

    verify(fhirServicesClient, times(2)).validateBundle(anyString(), anyString());
  }

  @Test
  public void testPersistTestCaseReturnsInvalidDraftStatusException() {
    measure.setMeasureMetaData(MeasureMetaData.builder().draft(false).build());
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(repository).findById(any(String.class));

    assertThrows(
        InvalidDraftStatusException.class,
        () -> testCaseService.persistTestCase(testCase, measure.getId(), "test.user", "TOKEN"));
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
    when(measureService.findMeasureById(anyString())).thenReturn(originalMeasure);

    TestCase updatingTestCase =
        testCase.toBuilder().title("UpdatedTitle").series("UpdatedSeries").build();
    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));
    TestCase updatedTestCase =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user5", "TOKEN");
    assertNotNull(updatedTestCase);

    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(updatingTestCase.getId(), updatedTestCase.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());
    TestCase expectedTestCase =
        updatedTestCase
            .toBuilder()
            .hapiOperationOutcome(
                HapiOperationOutcome.builder()
                    .code(500)
                    .message("An unknown exception occurred while validating the test case JSON.")
                    .build())
            .build();

    assertEquals(expectedTestCase, savedMeasure.getTestCases().get(0));

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user5", updatedTestCase.getLastModifiedBy());
    assertEquals(originalTestCase.getCreatedBy(), updatedTestCase.getCreatedBy());
    assertEquals(1, lastModCompareTo);
    assertNotEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user5", updatedTestCase.getCreatedBy());
  }

  @Test
  public void testUpdateTestCaseWithEnforcedPatientIdSuccess() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Instant createdAt = Instant.now().minus(300, ChronoUnit.SECONDS);
    String json =
        "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \n"
            + "  \"entry\" : [ {\n"
            + "    \"fullUrl\" : \"http://local/Patient/1\",\n"
            + "    \"resource\" : {\n"
            + "      \"id\" : \"testUniqueId\",\n"
            + "      \"resourceType\" : \"Patient\"    \n"
            + "    }\n"
            + "  } ]             }";
    TestCase originalTestCase =
        testCase
            .toBuilder()
            .createdAt(createdAt)
            .createdBy("test.user5")
            .lastModifiedAt(createdAt)
            .lastModifiedBy("test.user5")
            .json(json)
            .build();
    List<TestCase> testCases = new ArrayList<>();
    testCases.add(originalTestCase);
    Measure originalMeasure =
        measure
            .toBuilder()
            .model(ModelType.QI_CORE.getValue())
            .cqlLibraryName("Test1CQLLibraryName")
            .testCases(testCases)
            .build();
    when(measureService.findMeasureById(anyString())).thenReturn(originalMeasure);

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

    TestCase updatingTestCase =
        testCase.toBuilder().title("UpdatedTitle").series("UpdatedSeries").json(json).build();
    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));
    TestCase updatedTestCase =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user5", "TOKEN");
    assertNotNull(updatedTestCase);

    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(updatingTestCase.getId(), updatedTestCase.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());

    assertTrue(
        savedMeasure
            .getTestCases()
            .get(0)
            .getJson()
            .contains("Updatedtitle-Updatedseries-Test1CQLLibraryName-0.0.1"));

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user5", updatedTestCase.getLastModifiedBy());
    assertEquals(originalTestCase.getCreatedBy(), updatedTestCase.getCreatedBy());
    assertEquals(1, lastModCompareTo);
    assertNotEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user5", updatedTestCase.getCreatedBy());
  }

  @Test
  public void testUpdateTestCaseWithFeatureFlagFalse() {
    ReflectionTestUtils.setField(testCaseService, "enforcePatientIdFeatureFlag", false);

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Instant createdAt = Instant.now().minus(300, ChronoUnit.SECONDS);
    String json =
        "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \n"
            + "  \"entry\" : [ {\n"
            + "    \"fullUrl\" : \"http://local/Patient/1\",\n"
            + "    \"resource\" : {\n"
            + "      \"id\" : \"testUniqueId\",\n"
            + "      \"resourceType\" : \"Patient\"    \n"
            + "    }\n"
            + "  } ]             }";
    TestCase originalTestCase =
        testCase
            .toBuilder()
            .createdAt(createdAt)
            .createdBy("test.user5")
            .lastModifiedAt(createdAt)
            .lastModifiedBy("test.user5")
            .json(json)
            .build();
    List<TestCase> testCases = new ArrayList<>();
    testCases.add(originalTestCase);
    Measure originalMeasure =
        measure
            .toBuilder()
            .model(ModelType.QI_CORE.getValue())
            .cqlLibraryName("Test1CQLLibraryName")
            .testCases(testCases)
            .build();
    when(measureService.findMeasureById(anyString())).thenReturn(originalMeasure);

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

    TestCase updatingTestCase =
        testCase.toBuilder().title("UpdatedTitle").series("UpdatedSeries").json(json).build();
    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));
    TestCase updatedTestCase =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user5", "TOKEN");
    assertNotNull(updatedTestCase);

    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(updatingTestCase.getId(), updatedTestCase.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());

    assertFalse(
        savedMeasure
            .getTestCases()
            .get(0)
            .getJson()
            .contains("Updatedtitle-Updatedseries-Test1CQLLibraryName-0.0.1"));
    assertTrue(savedMeasure.getTestCases().get(0).getJson().contains("testUniqueId"));
    assertEquals(savedMeasure.getTestCases().get(0).getJson(), json);

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user5", updatedTestCase.getLastModifiedBy());
    assertEquals(originalTestCase.getCreatedBy(), updatedTestCase.getCreatedBy());
    assertEquals(1, lastModCompareTo);
    assertNotEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user5", updatedTestCase.getCreatedBy());
  }

  @Test
  public void testUpdateTestCaseEnforcingdPatientIdFail() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Instant createdAt = Instant.now().minus(300, ChronoUnit.SECONDS);
    String json = "invalid test case json";
    TestCase originalTestCase =
        testCase
            .toBuilder()
            .createdAt(createdAt)
            .createdBy("test.user5")
            .lastModifiedAt(createdAt)
            .lastModifiedBy("test.user5")
            .json(json)
            .build();
    List<TestCase> testCases = new ArrayList<>();
    testCases.add(originalTestCase);
    Measure originalMeasure =
        measure
            .toBuilder()
            .model(ModelType.QI_CORE.getValue())
            .cqlLibraryName("Test1CQLLibraryName")
            .testCases(testCases)
            .build();
    when(measureService.findMeasureById(anyString())).thenReturn(originalMeasure);

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

    TestCase updatingTestCase =
        testCase.toBuilder().title("UpdatedTitle").series("UpdatedSeries").json(json).build();
    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));
    TestCase updatedTestCase =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user5", "TOKEN");

    assertNotNull(updatedTestCase);

    verify(repository, times(1)).save(measureCaptor.capture());
    assertEquals(updatingTestCase.getId(), updatedTestCase.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());

    assertFalse(
        savedMeasure
            .getTestCases()
            .get(0)
            .getJson()
            .contains("Updatedtitle-Updatedseries-Test1CQLLibraryName-0.0.1"));

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user5", updatedTestCase.getLastModifiedBy());
    assertEquals(originalTestCase.getCreatedBy(), updatedTestCase.getCreatedBy());
    assertEquals(1, lastModCompareTo);
    assertNotEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user5", updatedTestCase.getCreatedBy());
  }

  @Test
  public void testEnforcePatientIdEmptyJson() {
    String modifiedJson = testCaseService.enforcePatientId(null, measure, testCase);
    assertNull(modifiedJson);
  }

  @Test
  public void testEnforcePatientIdNoEntry() {
    String json = "{\"resourceType\": \"Bundle\", \"type\": \"collection\"}";
    String modifiedJson = testCaseService.enforcePatientId(json, measure, testCase);
    assertEquals(modifiedJson, json);
  }

  @Test
  public void testEnforcePatientIdNoResource() {
    String json =
        "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \n"
            + "  \"entry\" : [ {\n"
            + "    \"fullUrl\" : \"http://local/Patient/1\"\n"
            + "  } ]             }";
    String modifiedJson = testCaseService.enforcePatientId(json, measure, testCase);
    assertEquals(modifiedJson, json);
  }

  @Test
  public void testEnforcePatientIdNoResourceType() {
    String json =
        "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \n"
            + "  \"entry\" : [ {\n"
            + "    \"fullUrl\" : \"http://local/Patient/1\",\n"
            + "    \"resource\" : {\n"
            + "      \"id\" : \"testUniqueId\"\n"
            + "    }\n"
            + "  } ]             }";
    String modifiedJson = testCaseService.enforcePatientId(json, measure, testCase);
    assertEquals(modifiedJson, json);
  }

  @Test
  public void testEnforcePatientIdNoPatientResourceType() {
    String json =
        "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \n"
            + "  \"entry\" : [ {\n"
            + "    \"fullUrl\" : \"http://local/Patient/1\",\n"
            + "    \"resource\" : {\n"
            + "      \"id\" : \"testUniqueId\",\n"
            + "      \"resourceType\" : \"NOTPatient\"    \n"
            + "    }\n"
            + "  } ]             }";
    String modifiedJson = testCaseService.enforcePatientId(json, measure, testCase);
    assertEquals(modifiedJson, json);
  }

  @Test
  public void testEnforcePatientIdNoTestCaseSeries() {
    TestCase noSeriesTC = TestCase.builder().title("Test Title").build();
    Measure testMeasure =
        Measure.builder()
            .cqlLibraryName("Test1CQLLibraryName")
            .version(Version.builder().major(0).minor(0).revisionNumber(1).build())
            .build();
    testMeasure.setTestCases(List.of(noSeriesTC));
    String json =
        "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \n"
            + "  \"entry\" : [ {\n"
            + "    \"fullUrl\" : \"http://local/Patient/1\",\n"
            + "    \"resource\" : {\n"
            + "      \"id\" : \"testUniqueId\",\n"
            + "      \"resourceType\" : \"Patient\"    \n"
            + "    }\n"
            + "  } ]             }";
    String modifiedJson = testCaseService.enforcePatientId(json, testMeasure, noSeriesTC);
    assertNotEquals(modifiedJson, json);
    assertTrue(modifiedJson.contains("TestTitle-Test1CQLLibraryName-0.0.1"));
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
    when(measureService.findMeasureById(anyString())).thenReturn(originalMeasure);

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
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user5", "TOKEN");
    assertNotNull(updatedTestCase);

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user5", updatedTestCase.getLastModifiedBy());
    assertEquals(1, lastModCompareTo);
    assertNotEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals(originalTestCase.getCreatedAt(), updatedTestCase.getCreatedAt());
    assertEquals(originalTestCase.getCreatedBy(), updatedTestCase.getCreatedBy());
  }

  @Test
  public void testUpdateTestCaseReturnsInvalidDraftStatusException() {
    when(measureService.findMeasureById(anyString())).thenReturn(measure);
    measure.setMeasureMetaData(MeasureMetaData.builder().draft(false).build());
    Optional<Measure> optional = Optional.of(measure);

    assertThrows(
        InvalidDraftStatusException.class,
        () -> testCaseService.updateTestCase(testCase, measure.getId(), "test.user", "TOKEN"));
  }

  @Test
  public void testThatUpdateTestCaseHandlesUpsertForNullTestCasesList() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Measure originalMeasure = measure.toBuilder().testCases(null).build();
    when(measureService.findMeasureById(anyString())).thenReturn(originalMeasure);
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
        testCaseService.updateTestCase(upsertingTestCase, measure.getId(), "test.user5", "TOKEN");
    assertNotNull(updatedTestCase);

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals(1, lastModCompareTo);
    assertNotNull(updatedTestCase.getId());
    assertEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user5", updatedTestCase.getCreatedBy());
    assertEquals("test.user5", updatedTestCase.getLastModifiedBy());

    verify(repository, times(1)).save(measureCaptor.capture());
    Measure savedMeasure = measureCaptor.getValue();
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());
    TestCase expectedTestCase =
        upsertingTestCase
            .toBuilder()
            .hapiOperationOutcome(
                HapiOperationOutcome.builder()
                    .code(500)
                    .message("An unknown exception occurred while validating the test case JSON.")
                    .build())
            .build();
    assertEquals(expectedTestCase, savedMeasure.getTestCases().get(0));
  }

  @Test
  public void testThatUpdateTestCaseHandlesUpsertForEmptyTestCasesList() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Measure originalMeasure = measure.toBuilder().testCases(new ArrayList<>()).build();

    Mockito.doAnswer((args) -> args.getArgument(0)).when(repository).save(any(Measure.class));

    TestCase upsertingTestCase =
        testCase
            .toBuilder()
            .createdBy("Nobody")
            .createdAt(Instant.now())
            .title("UpdatedTitle")
            .series("UpdatedSeries")
            .build();
    when(measureService.findMeasureById(anyString())).thenReturn(originalMeasure);

    TestCase updatedTestCase =
        testCaseService.updateTestCase(upsertingTestCase, measure.getId(), "test.user5", "TOKEN");
    assertNotNull(updatedTestCase);

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals(1, lastModCompareTo);
    assertNotNull(updatedTestCase.getId());
    assertEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user5", updatedTestCase.getCreatedBy());
    assertEquals("test.user5", updatedTestCase.getLastModifiedBy());

    verify(repository, times(1)).save(measureCaptor.capture());
    Measure savedMeasure = measureCaptor.getValue();
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());

    TestCase expectedTestCase =
        updatedTestCase
            .toBuilder()
            .hapiOperationOutcome(
                HapiOperationOutcome.builder()
                    .code(500)
                    .message("An unknown exception occurred while validating the test case JSON.")
                    .build())
            .build();
    assertEquals(expectedTestCase, savedMeasure.getTestCases().get(0));
  }

  @Test
  public void testThatUpdateTestCaseHandlesUpsertWithOtherExistingTestCases() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    TestCase otherExistingTC =
        TestCase.builder().id("TC1_ID").title("TC1").series("Series1").build();
    Measure originalMeasure =
        measure.toBuilder().testCases(new ArrayList<>(Arrays.asList(otherExistingTC))).build();
    when(measureService.findMeasureById(anyString())).thenReturn(originalMeasure);
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
        testCaseService.updateTestCase(upsertingTestCase, measure.getId(), "test.user5", "TOKEN");
    assertNotNull(updatedTestCase);

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals(1, lastModCompareTo);
    assertNotNull(updatedTestCase.getId());
    assertEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user5", updatedTestCase.getCreatedBy());
    assertEquals("test.user5", updatedTestCase.getLastModifiedBy());

    verify(repository, times(1)).save(measureCaptor.capture());
    Measure savedMeasure = measureCaptor.getValue();
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(2, savedMeasure.getTestCases().size());
    assertEquals(otherExistingTC, savedMeasure.getTestCases().get(0));

    TestCase expectedTestCase =
        updatedTestCase
            .toBuilder()
            .hapiOperationOutcome(
                HapiOperationOutcome.builder()
                    .code(500)
                    .message("An unknown exception occurred while validating the test case JSON.")
                    .build())
            .build();
    assertEquals(expectedTestCase, savedMeasure.getTestCases().get(1));
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
        Measure.builder()
            .id("measure-id")
            .createdBy("test.user")
            .testCases(testCases)
            .measureMetaData(MeasureMetaData.builder().draft(true).build())
            .build();
    when(repository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    doReturn(existingMeasure).when(repository).save(any(Measure.class));

    String output = testCaseService.deleteTestCase("measure-id", "TC2_ID", "test.user");
    assertThat(output, is(equalTo("Test case deleted successfully: TC2_ID")));
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
  void testDeleteTestCaseReturnsExceptionForTestCaseNotFound() {
    List<TestCase> testCases =
        List.of(
            TestCase.builder().id("TC1_ID").title("TC1").build(),
            TestCase.builder().id("TC2_ID").title("TC2").build());
    final Measure measure =
        Measure.builder()
            .id("measure-id")
            .createdBy("OtherUser")
            .testCases(testCases)
            .measureMetaData(MeasureMetaData.builder().draft(true).build())
            .build();
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        InvalidIdException.class,
        () -> testCaseService.deleteTestCase("measure-id", "testCaseId", "OtherUser"));
  }

  @Test
  void testDeleteTestCaseReturnsExceptionThrowsAccessException() {
    final Measure measure =
        Measure.builder()
            .id("measure-id")
            .createdBy("OtherUser")
            .measureMetaData(MeasureMetaData.builder().draft(true).build())
            .build();
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));
    doThrow(new UnauthorizedException("Measure", "measure-id", "user2"))
        .when(measureService)
        .verifyAuthorization(anyString(), any(Measure.class));
    assertThrows(
        UnauthorizedException.class,
        () -> testCaseService.deleteTestCase("measure-id", "testCaseId", "user2"));
  }

  @Test
  void testDeleteTestCaseReturnsExceptionForTestCaseNotFoundInMeasure() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.deleteTestCase("measure-id", "testCaseId", "test.user"));
  }

  @Test
  void testDeleteTestCasReturnsExceptionForNullTestCasesinMeasure() {
    Measure existingMeasure =
        Measure.builder()
            .id("measure-id")
            .createdBy("test.user")
            .testCases(null)
            .measureMetaData(MeasureMetaData.builder().draft(true).build())
            .build();
    when(repository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        InvalidIdException.class,
        () -> testCaseService.deleteTestCase("measure-id", "testCaseId", "test.user"));
  }

  @Test
  void testDeleteTestCaseReturnsInvalidDraftStatusException() {
    List<TestCase> testCases =
        List.of(
            TestCase.builder().id("TC1_ID").title("TC1").build(),
            TestCase.builder().id("TC2_ID").title("TC2").build());

    Measure existingMeasure =
        Measure.builder()
            .id("measure-id")
            .createdBy("test.user")
            .testCases(testCases)
            .measureMetaData(MeasureMetaData.builder().draft(false).build())
            .build();
    when(repository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        InvalidDraftStatusException.class,
        () -> testCaseService.deleteTestCase("measure-id", "TC2_ID", "test.user"));
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
