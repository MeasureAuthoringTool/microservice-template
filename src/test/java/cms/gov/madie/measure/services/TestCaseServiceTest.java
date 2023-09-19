package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.JobStatus;
import cms.gov.madie.measure.dto.MeasureTestCaseValidationReport;
import cms.gov.madie.measure.exceptions.DuplicateTestCaseNameException;
import cms.gov.madie.measure.exceptions.InvalidDraftStatusException;
import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.utils.TestCaseServiceUtil;
import cms.gov.madie.measure.utils.ResourceUtil;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.common.Version;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseImportRequest;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestCaseServiceTest implements ResourceUtil {
  @Mock private MeasureRepository measureRepository;
  @Mock private ActionLogService actionLogService;
  @Mock private TestCaseServiceUtil testCaseServiceUtil;

  @Spy private ObjectMapper mapper;

  @Mock private FhirServicesClient fhirServicesClient;
  @Mock private MeasureService measureService;

  @Spy @InjectMocks private TestCaseService testCaseService;

  @Captor private ArgumentCaptor<ActionType> actionTypeArgumentCaptor;
  @Captor private ArgumentCaptor<String> targetIdArgumentCaptor;
  @Captor private ArgumentCaptor<Class> targetClassArgumentCaptor;

  private TestCase testCase;
  private Measure measure;
  private Group group;
  private Population population1;
  private Population population2;
  private Population population3;
  private Population population4;
  private Population population5;

  String testCaseImportWithMeasureReport = getData("/test_case_exported_json.json");

  @BeforeEach
  public void setUp() {
    testCase = new TestCase();
    testCase.setId("TESTID");
    testCase.setTitle("IPPPass");
    testCase.setSeries("BloodPressure>124");
    testCase.setCreatedBy("TestUser");
    testCase.setLastModifiedBy("TestUser2");
    testCase.setJson("{\"resourceType\":\"Patient\"}");
    testCase.setPatientId(UUID.randomUUID());

    measure = new Measure();
    measure.setCreatedBy("test.user5");
    measure.setId(ObjectId.get().toString());
    measure.setMeasureSetId("IDIDID");
    measure.setMeasureName("MSR01");
    measure.setVersion(new Version(0, 0, 1));
    measure.setMeasureMetaData(MeasureMetaData.builder().draft(true).build());
  }

  @Test
  public void testPersistTestCase() {
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(measureRepository).findById(any(String.class));

    Mockito.doReturn(measure).when(measureRepository).save(any(Measure.class));

    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(
            ResponseEntity.ok(HapiOperationOutcome.builder().code(200).successful(true).build()));

    TestCase persistTestCase =
        testCaseService.persistTestCase(testCase, measure.getId(), "test.user", "TOKEN");
    verify(measureRepository, times(1)).save(measureCaptor.capture());
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
    assertEquals(
        UUID.fromString(capturedTestCase.getPatientId().toString()).toString(),
        capturedTestCase.getPatientId().toString());

    assertNotNull(persistTestCase.getHapiOperationOutcome());
    assertEquals(200, persistTestCase.getHapiOperationOutcome().getCode());
    assertNotNull(persistTestCase.getPatientId());

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
    Mockito.doReturn(optional).when(measureRepository).findById(any(String.class));

    Mockito.doReturn(measure).when(measureRepository).save(any(Measure.class));

    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(
            ResponseEntity.ok(HapiOperationOutcome.builder().code(200).successful(true).build()));

    TestCase persistTestCase =
        testCaseService.persistTestCase(testCase, measure.getId(), "test.user", "TOKEN");
    assertThat(persistTestCase, is(notNullValue()));
    assertThat(persistTestCase.getId(), is(notNullValue()));
    assertThat(persistTestCase.getTitle(), is(equalTo(testCase.getTitle())));
    verify(measureRepository, times(1)).save(measureCaptor.capture());
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
  public void testUpdateTestCaseValidResourcesWithReportMeasureNotFound() {
    final String measureId = "M1234";
    final String accessToken = "Bearer Token";
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());

    MeasureTestCaseValidationReport output =
        testCaseService.updateTestCaseValidResourcesWithReport(measureId, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getMeasureId(), is(equalTo(measureId)));
    assertThat(output.getJobStatus(), is(equalTo(JobStatus.SKIPPED)));
    assertThat(output.getMeasureSetId(), is(nullValue()));
    assertThat(output.getMeasureVersionId(), is(nullValue()));
    assertThat(output.getMeasureName(), is(nullValue()));
  }

  @Test
  public void testUpdateTestCaseValidResourcesWithReportMeasureNullTestCases() {
    final String measureId = "M1234";
    final String accessToken = "Bearer Token";
    final String measureSetId = UUID.randomUUID().toString();
    final String versionId = UUID.randomUUID().toString();
    Measure measure =
        Measure.builder()
            .id(measureId)
            .measureName("Measure 1234")
            .measureSetId(measureSetId)
            .versionId(versionId)
            .testCases(null)
            .model(ModelType.QI_CORE.getValue())
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));

    MeasureTestCaseValidationReport output =
        testCaseService.updateTestCaseValidResourcesWithReport(measureId, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getMeasureId(), is(equalTo(measureId)));
    assertThat(output.getJobStatus(), is(equalTo(JobStatus.COMPLETED)));
    assertThat(output.getMeasureSetId(), is(equalTo(measureSetId)));
    assertThat(output.getMeasureVersionId(), is(equalTo(versionId)));
    assertThat(output.getMeasureName(), is(equalTo("Measure 1234")));
    assertThat(output.getTestCaseValidationReports(), is(notNullValue()));
    assertThat(output.getTestCaseValidationReports().isEmpty(), is(true));
  }

  @Test
  public void testUpdateTestCaseValidResourcesWithReportMeasureEmptyTestCases() {
    final String measureId = "M1234";
    final String accessToken = "Bearer Token";
    final String measureSetId = UUID.randomUUID().toString();
    final String versionId = UUID.randomUUID().toString();
    Measure measure =
        Measure.builder()
            .id(measureId)
            .measureName("Measure 1234")
            .measureSetId(measureSetId)
            .versionId(versionId)
            .testCases(List.of())
            .model(ModelType.QI_CORE.getValue())
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));

    MeasureTestCaseValidationReport output =
        testCaseService.updateTestCaseValidResourcesWithReport(measureId, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getMeasureId(), is(equalTo(measureId)));
    assertThat(output.getJobStatus(), is(equalTo(JobStatus.COMPLETED)));
    assertThat(output.getMeasureSetId(), is(equalTo(measureSetId)));
    assertThat(output.getMeasureVersionId(), is(equalTo(versionId)));
    assertThat(output.getMeasureName(), is(equalTo("Measure 1234")));
    assertThat(output.getTestCaseValidationReports(), is(notNullValue()));
    assertThat(output.getTestCaseValidationReports().isEmpty(), is(true));
  }

  @Test
  public void testUpdateTestCaseValidResourcesWithReportMeasureWithTestCases() {
    final String measureId = "M1234";
    final String accessToken = "Bearer Token";
    final String measureSetId = UUID.randomUUID().toString();
    final String versionId = UUID.randomUUID().toString();
    List<TestCase> prevTestCases =
        List.of(
            TestCase.builder()
                .id("TC1")
                .name("TC1")
                .validResource(true)
                .patientId(UUID.randomUUID())
                .json("{}")
                .build(),
            TestCase.builder()
                .id("TC2")
                .name("TC2")
                .validResource(true)
                .patientId(UUID.randomUUID())
                .json("{}")
                .build(),
            TestCase.builder()
                .id("TC3")
                .name("TC3")
                .validResource(false)
                .patientId(UUID.randomUUID())
                .json("{}")
                .build());

    Measure measure =
        Measure.builder()
            .id(measureId)
            .measureName("Measure 1234")
            .measureSetId(measureSetId)
            .versionId(versionId)
            .testCases(prevTestCases)
            .model(ModelType.QI_CORE.getValue())
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));

    TestCaseService spy = Mockito.spy(testCaseService);
    List<TestCase> validatedTestCases =
        List.of(
            TestCase.builder()
                .id("TC1")
                .name("TC1")
                .validResource(false)
                .patientId(UUID.randomUUID())
                .json("{}")
                .build(),
            TestCase.builder()
                .id("TC2")
                .name("TC2")
                .validResource(true)
                .patientId(UUID.randomUUID())
                .json("{}")
                .build(),
            TestCase.builder()
                .id("TC3")
                .name("TC3")
                .validResource(true)
                .patientId(UUID.randomUUID())
                .json("{}")
                .build());
    doReturn(validatedTestCases)
        .when(spy)
        .updateTestCaseValidResourcesForMeasure(any(Measure.class), anyString());

    MeasureTestCaseValidationReport output =
        spy.updateTestCaseValidResourcesWithReport(measureId, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getMeasureId(), is(equalTo(measureId)));
    assertThat(output.getJobStatus(), is(equalTo(JobStatus.COMPLETED)));
    assertThat(output.getMeasureSetId(), is(equalTo(measureSetId)));
    assertThat(output.getMeasureVersionId(), is(equalTo(versionId)));
    assertThat(output.getMeasureName(), is(equalTo("Measure 1234")));
    assertThat(output.getTestCaseValidationReports(), is(notNullValue()));
    assertThat(output.getTestCaseValidationReports().size(), is(equalTo(3)));
    assertThat(output.getTestCaseValidationReports().get(0), is(notNullValue()));
    assertThat(output.getTestCaseValidationReports().get(0).getTestCaseId(), is(equalTo("TC1")));
    assertThat(output.getTestCaseValidationReports().get(0).isPreviousValidResource(), is(true));
    assertThat(output.getTestCaseValidationReports().get(0).isCurrentValidResource(), is(false));
    assertThat(output.getTestCaseValidationReports().get(1), is(notNullValue()));
    assertThat(output.getTestCaseValidationReports().get(1).getTestCaseId(), is(equalTo("TC2")));
    assertThat(output.getTestCaseValidationReports().get(1).isPreviousValidResource(), is(true));
    assertThat(output.getTestCaseValidationReports().get(1).isCurrentValidResource(), is(true));
    assertThat(output.getTestCaseValidationReports().get(2), is(notNullValue()));
    assertThat(output.getTestCaseValidationReports().get(2).getTestCaseId(), is(equalTo("TC3")));
    assertThat(output.getTestCaseValidationReports().get(2).isPreviousValidResource(), is(false));
    assertThat(output.getTestCaseValidationReports().get(2).isCurrentValidResource(), is(true));
  }

  @Test
  public void testUpdateTestCaseValidResourcesForMeasureValidList() {
    TestCase testCase =
        TestCase.builder()
            .id("TestID")
            .json("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}")
            .build();
    Measure measure =
        Measure.builder().testCases(List.of(testCase)).model(ModelType.QI_CORE.getValue()).build();
    final String accessToken = "Bearer Token";
    TestCaseService spy = Mockito.spy(testCaseService);
    TestCase validatedTestCase =
        testCase
            .toBuilder()
            .hapiOperationOutcome(HapiOperationOutcome.builder().build())
            .validResource(true)
            .build();
    doReturn(List.of(validatedTestCase))
        .when(spy)
        .validateTestCasesAsResources(anyList(), any(ModelType.class), anyString());

    List<TestCase> output = spy.updateTestCaseValidResourcesForMeasure(measure, accessToken);

    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(1)));
    assertThat(output.get(0), is(equalTo(validatedTestCase)));
  }

  @Test
  public void testValidateTestCasesAsResourcesNullList() {
    final String accessToken = "Bearer Token";
    final ModelType model = ModelType.QI_CORE;
    List<TestCase> output = testCaseService.validateTestCasesAsResources(null, model, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.isEmpty(), is(true));
  }

  @Test
  public void testValidateTestCasesAsResourcesEmptyList() {
    final String accessToken = "Bearer Token";
    final ModelType model = ModelType.QI_CORE;
    final List<TestCase> testCases = List.of();
    List<TestCase> output =
        testCaseService.validateTestCasesAsResources(testCases, model, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.isEmpty(), is(true));
  }

  @Test
  public void testValidateTestCasesAsResourcesWithEntries() throws JsonProcessingException {
    TestCase testCase =
        TestCase.builder()
            .id("TestID")
            .json("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}")
            .build();
    final String accessToken = "Bearer Token";

    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(
            ResponseEntity.ok(HapiOperationOutcome.builder().code(200).successful(true).build()));
    final ModelType model = ModelType.QI_CORE;
    final List<TestCase> testCases = List.of(testCase);
    List<TestCase> output =
        testCaseService.validateTestCasesAsResources(testCases, model, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.isEmpty(), is(false));
    assertThat(output.get(0), is(notNullValue()));
    assertThat(output.get(0).isValidResource(), is(true));
    assertThat(output.get(0).getHapiOperationOutcome(), is(notNullValue()));
    assertThat(output.get(0).getHapiOperationOutcome().getCode(), is(equalTo(200)));
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
        .thenReturn(
            ResponseEntity.ok(HapiOperationOutcome.builder().code(200).successful(true).build()));

    TestCase output =
        testCaseService.validateTestCaseAsResource(testCase, ModelType.QI_CORE, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getJson(), is(notNullValue()));
    assertThat(output.getHapiOperationOutcome(), is(notNullValue()));
    assertThat(output.getHapiOperationOutcome().getCode(), is(equalTo(200)));
  }

  @Test
  public void testValidateTestCaseAsResourceForQDM() {
    final String qdmJson = "{\n \"qdmVersion\": \"5.6\",\n \"dataElements\": []\n }";
    TestCase testCase = TestCase.builder().id("TestID").json(qdmJson).build();
    final String accessToken = "Bearer Token";
    TestCase output =
        testCaseService.validateTestCaseAsResource(testCase, ModelType.QDM_5_6, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getJson(), is(notNullValue()));
    assertThat(output.isValidResource(), is(true));
  }

  @Test
  public void testValidateTestCaseAsResourceMalformedJsonForQDM() {
    final String qdmJson = "{\n BADTHINGHERE \"qdmVersion\": \"5.6\",\n \"dataElements\": []\n }";
    TestCase testCase = TestCase.builder().id("TestID").json(qdmJson).build();
    final String accessToken = "Bearer Token";
    TestCase output =
        testCaseService.validateTestCaseAsResource(testCase, ModelType.QDM_5_6, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getJson(), is(notNullValue()));
    assertThat(output.isValidResource(), is(false));
  }

  @Test
  public void testValidateTestCaseAsResourceNull() {
    final String accessToken = "Bearer Token";
    TestCase output =
        testCaseService.validateTestCaseAsResource(null, ModelType.QDM_5_6, accessToken);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testValidateTestCaseAsResourceHandlesNullTestCase() {
    TestCase testCase = null;
    final String accessToken = "Bearer Token";

    TestCase output =
        testCaseService.validateTestCaseAsResource(testCase, ModelType.QI_CORE, accessToken);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testPersistTestCasesThrowsResourceNotFoundExceptionForUnknownId() {
    List<TestCase> newTestCases = List.of(TestCase.builder().title("Test1").build());
    String measureId = measure.getId();
    String username = "user01";
    String accessToken = "Bearer Token";
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());

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
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));

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
    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(measure));

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
    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(measure));

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
    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(measure));
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(
            ResponseEntity.ok(HapiOperationOutcome.builder().code(200).successful(true).build()))
        .thenReturn(
            ResponseEntity.ok(HapiOperationOutcome.builder().code(400).successful(false).build()));

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
    Mockito.doReturn(optional).when(measureRepository).findById(any(String.class));

    assertThrows(
        InvalidDraftStatusException.class,
        () -> testCaseService.persistTestCase(testCase, measure.getId(), "test.user", "TOKEN"));
  }

  @Test
  public void testFindTestCasesByMeasureId() {
    measure.setTestCases(List.of(testCase));
    Optional<Measure> optional = Optional.of(measure);
    Mockito.doReturn(optional).when(measureRepository).findById(any(String.class));
    List<TestCase> persistTestCase = testCaseService.findTestCasesByMeasureId(measure.getId());
    assertEquals(1, persistTestCase.size());
    assertEquals(testCase.getId(), persistTestCase.get(0).getId());
  }

  @Test
  public void testFindTestCasesByMeasureIdWhenMeasureDoesNotExist() {
    Optional<Measure> optional = Optional.empty();
    Mockito.doReturn(optional).when(measureRepository).findById(any(String.class));
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.findTestCasesByMeasureId(measure.getId()));
  }

  @Test
  public void testFindTestCaseSeriesByMeasureIdThrowsExceptionWhenMeasureDoesNotExist() {
    Optional<Measure> optional = Optional.empty();
    when(measureRepository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.findTestCaseSeriesByMeasureId(measure.getId()));
  }

  @Test
  public void testFindTestCaseSeriesByMeasureIdReturnsEmptyListWhenTestCasesNull() {
    Measure noTestCases = measure.toBuilder().build();
    measure.setTestCases(null);
    Optional<Measure> optional = Optional.of(noTestCases);
    when(measureRepository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
    List<String> output = testCaseService.findTestCaseSeriesByMeasureId(measure.getId());
    assertEquals(List.of(), output);
  }

  @Test
  public void testFindTestCaseSeriesByMeasureIdReturnsEmptyListWhenTestCasesEmpty() {
    Measure noTestCases = measure.toBuilder().build();
    measure.setTestCases(new ArrayList<>());
    Optional<Measure> optional = Optional.of(noTestCases);
    when(measureRepository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
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
    when(measureRepository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
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
    when(measureRepository.findAllTestCaseSeriesByMeasureId(anyString())).thenReturn(optional);
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
    Mockito.doAnswer((args) -> args.getArgument(0))
        .when(measureRepository)
        .save(any(Measure.class));
    TestCase updatedTestCase =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user5", "TOKEN");
    assertNotNull(updatedTestCase);

    verify(measureRepository, times(1)).save(measureCaptor.capture());
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
    String patientId = "3d2abb9d-c10a-4ab3-ae1a-1684ab61c07e";
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
            .patientId(UUID.fromString(patientId))
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
                HapiOperationOutcome.builder()
                    .code(200)
                    .successful(true)
                    .outcomeResponse(
                        "{\n"
                            + "        \"resourceType\": \"OperationOutcome\",\n"
                            + "        \"issue\": [\n"
                            + "            {\n"
                            + "                \"severity\": \"information\",\n"
                            + "                \"code\": \"informational\",\n"
                            + "                \"diagnostics\": \"No issues detected during validation\"\n"
                            + "            }\n"
                            + "        ]\n"
                            + "    }\n")
                    .build()));

    TestCase updatingTestCase =
        testCase.toBuilder().title("UpdatedTitle").series("UpdatedSeries").json(json).build();
    Mockito.doAnswer((args) -> args.getArgument(0))
        .when(measureRepository)
        .save(any(Measure.class));
    TestCase updatedTestCase =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user5", "TOKEN");
    assertNotNull(updatedTestCase);

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    assertEquals(updatingTestCase.getId(), updatedTestCase.getId());
    Measure savedMeasure = measureCaptor.getValue();
    assertEquals(measure.getLastModifiedBy(), savedMeasure.getLastModifiedBy());
    assertEquals(measure.getLastModifiedAt(), savedMeasure.getLastModifiedAt());
    assertNotNull(savedMeasure.getTestCases());
    assertEquals(1, savedMeasure.getTestCases().size());

    assertTrue(savedMeasure.getTestCases().get(0).getJson().contains(patientId));

    int lastModCompareTo =
        updatedTestCase.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals("test.user5", updatedTestCase.getLastModifiedBy());
    assertEquals(originalTestCase.getCreatedBy(), updatedTestCase.getCreatedBy());
    assertEquals(1, lastModCompareTo);
    assertNotEquals(updatedTestCase.getLastModifiedAt(), updatedTestCase.getCreatedAt());
    assertEquals("test.user5", updatedTestCase.getCreatedBy());
  }

  @Test
  public void testUpdateTestCaseEnforcingPatientIdFail() {
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
                HapiOperationOutcome.builder()
                    .code(200)
                    .successful(true)
                    .outcomeResponse(
                        "{\n"
                            + "        \"resourceType\": \"OperationOutcome\",\n"
                            + "        \"issue\": [\n"
                            + "            {\n"
                            + "                \"severity\": \"information\",\n"
                            + "                \"code\": \"informational\",\n"
                            + "                \"diagnostics\": \"No issues detected during validation\"\n"
                            + "            }\n"
                            + "        ]\n"
                            + "    }\n")
                    .build()));

    TestCase updatingTestCase =
        testCase.toBuilder().title("UpdatedTitle").series("UpdatedSeries").json(json).build();
    Mockito.doAnswer((args) -> args.getArgument(0))
        .when(measureRepository)
        .save(any(Measure.class));
    TestCase updatedTestCase =
        testCaseService.updateTestCase(updatingTestCase, measure.getId(), "test.user5", "TOKEN");

    assertNotNull(updatedTestCase);

    verify(measureRepository, times(1)).save(measureCaptor.capture());
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
  public void testUpdateTestCaseWhenMeasureIsNull() {
    when(measureService.findMeasureById(anyString())).thenReturn(null);
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.updateTestCase(testCase, measure.getId(), "test.user", "TOKEN"));
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
    Mockito.doAnswer((args) -> args.getArgument(0))
        .when(measureRepository)
        .save(any(Measure.class));

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
    Mockito.doAnswer((args) -> args.getArgument(0))
        .when(measureRepository)
        .save(any(Measure.class));

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

    verify(measureRepository, times(1)).save(measureCaptor.capture());
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

    Mockito.doAnswer((args) -> args.getArgument(0))
        .when(measureRepository)
        .save(any(Measure.class));

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

    verify(measureRepository, times(1)).save(measureCaptor.capture());
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
    Mockito.doAnswer((args) -> args.getArgument(0))
        .when(measureRepository)
        .save(any(Measure.class));

    TestCase upsertingTestCase =
        testCase
            .toBuilder()
            .createdBy("Nobody")
            .createdAt(Instant.now())
            .title("UpdatedTitle")
            .series("UpdatedSeries")
            .patientId(null)
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

    verify(measureRepository, times(1)).save(measureCaptor.capture());
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
    Mockito.doReturn(optional).when(measureRepository).findById(any(String.class));
    TestCase output =
        testCaseService.getTestCase(measure.getId(), testCase.getId(), false, "TOKEN");
    assertEquals(testCase, output);
  }

  @Test
  public void testGetTestCaseReturnsTestCaseByIdValidatesByUpsert() {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(
            ResponseEntity.ok(HapiOperationOutcome.builder().code(200).successful(true).build()));

    Optional<Measure> optional =
        Optional.of(measure.toBuilder().testCases(Arrays.asList(testCase)).build());
    Mockito.doReturn(optional).when(measureRepository).findById(any(String.class));
    TestCase output = testCaseService.getTestCase(measure.getId(), testCase.getId(), true, "TOKEN");
    assertEquals(testCase, output);
    assertNotNull(output.getHapiOperationOutcome());
    assertEquals(200, output.getHapiOperationOutcome().getCode());
  }

  @Test
  public void testGetTestCaseThrowsNotFoundExceptionForMeasureWithEmptyListTestCases() {
    Mockito.doReturn(Optional.of(measure.toBuilder().testCases(Lists.emptyList()).build()))
        .when(measureRepository)
        .findById(any(String.class));
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseService.getTestCase(measure.getId(), testCase.getId(), false, "TOKEN"));
  }

  @Test
  public void testGetTestCaseThrowsNotFoundExceptionForMeasureWithNullTestCases() {
    Mockito.doReturn(Optional.of(measure.toBuilder().testCases(null).build()))
        .when(measureRepository)
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
        .when(measureRepository)
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
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    doReturn(existingMeasure).when(measureRepository).save(any(Measure.class));

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
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
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
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
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
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

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
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        InvalidDraftStatusException.class,
        () -> testCaseService.deleteTestCase("measure-id", "TC2_ID", "test.user"));
  }

  @Test
  void testDeleteTestCasesThrowsInvalidIdExceptionIfMeasureIdIsNull() {
    measure.setId(null);
    assertThrows(
        InvalidIdException.class,
        () ->
            testCaseService.deleteTestCases(
                measure.getId(), List.of("TC1_ID", "TC2_ID"), "test.user"));
  }

  @Test
  void testDeleteTestCasesThrowsInvalidIdExceptionIfTestCaseIdsIsAnEmptyList() {
    assertThrows(
        InvalidIdException.class,
        () -> testCaseService.deleteTestCases(measure.getId(), List.of(), "test.user"));
  }

  @Test
  void testDeleteTestCasesShouldThrowResourceNotFoundExceptionWhenMeasureIsNotFound() {
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            testCaseService.deleteTestCases(
                measure.getId(), List.of("TC1_ID", "TC2_ID"), "test.user"));
  }

  @Test
  void testDeleteTestCasesThrowsInvalidDraftStateException() {
    measure.getMeasureMetaData().setDraft(false);
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    assertThrows(
        InvalidDraftStatusException.class,
        () ->
            testCaseService.deleteTestCases(
                measure.getId(), List.of("TC1_ID", "TC2_ID"), "test.user"));
  }

  @Test
  void testDeleteTestCasesThrowsExceptionWhenMeasureDoesNotContainAnyTestCases() {
    measure.setTestCases(List.of());
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    assertThrows(
        InvalidIdException.class,
        () ->
            testCaseService.deleteTestCases(
                measure.getId(), List.of("TC1_ID", "TC2_ID"), "test.user"));
  }

  @Test
  void testDeleteTestCases() {
    List<TestCase> testCases =
        List.of(
            TestCase.builder().id("TC1_ID").title("TC1").build(),
            TestCase.builder().id("TC2_ID").title("TC2").build(),
            TestCase.builder().id("TC3_ID").title("TC3").build(),
            TestCase.builder().id("TC4_ID").title("TC4").build());

    measure.setTestCases(testCases);
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    doReturn(measure).when(measureRepository).save(any(Measure.class));

    String output =
        testCaseService.deleteTestCases(measure.getId(), List.of("TC1_ID", "TC2_ID"), "test.user");
    assertThat(output, is(equalTo("Succesfully deleted provided test cases")));
  }

  @Test
  void testDeleteTestCasesAndReturnNotFoundTestIds() {
    List<TestCase> testCases =
        List.of(
            TestCase.builder().id("TC1_ID").title("TC1").build(),
            TestCase.builder().id("TC2_ID").title("TC2").build(),
            TestCase.builder().id("TC3_ID").title("TC3").build(),
            TestCase.builder().id("TC4_ID").title("TC4").build());

    measure.setTestCases(testCases);
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    doReturn(measure).when(measureRepository).save(any(Measure.class));

    String output =
        testCaseService.deleteTestCases(
            measure.getId(), List.of("TC1_ID", "TC2_ID", "TC5_ID", "TC6_ID"), "test.user");
    assertThat(
        output, is(equalTo("Succesfully deleted provided test cases except [ TC5_ID, TC6_ID ]")));
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
  public void testValidateTestCaseJsonHandlesGoodResponse() {
    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(
            ResponseEntity.ok(
                HapiOperationOutcome.builder()
                    .code(200)
                    .successful(true)
                    .outcomeResponse(
                        "{\n"
                            + "        \"resourceType\": \"OperationOutcome\",\n"
                            + "        \"issue\": [\n"
                            + "            {\n"
                            + "                \"severity\": \"information\",\n"
                            + "                \"code\": \"informational\",\n"
                            + "                \"diagnostics\": \"No issues detected during validation\"\n"
                            + "            }\n"
                            + "        ]\n"
                            + "    }\n")
                    .build()));

    HapiOperationOutcome output =
        testCaseService.validateTestCaseJson(TestCase.builder().json("{}").build(), "TOKEN");
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(200)));
    assertThat(output.getMessage(), is(nullValue()));
    assertThat(output.isSuccessful(), is(true));
  }

  @Test
  void importTestCasesReturnValidOutcomes() {
    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    TestCase updatedTestCase = testCase;
    updatedTestCase.setJson(testCaseImportWithMeasureReport);

    doReturn(updatedTestCase)
        .when(testCaseService)
        .updateTestCase(any(), anyString(), anyString(), anyString());
    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(testCase.getPatientId())
            .json(testCaseImportWithMeasureReport)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertTrue(response.get(0).isSuccessful());
  }

  @Test
  void importTestCasesReturnValidOutcomeWithAnyExceptionsWhileUpdatingTestCases() {
    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    doThrow(new ResourceNotFoundException("Measure", measure.getId()))
        .when(testCaseService)
        .updateTestCase(any(), anyString(), anyString(), anyString());
    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(testCase.getPatientId())
            .json(testCaseImportWithMeasureReport)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertFalse(response.get(0).isSuccessful());
    assertEquals(
        "Could not find Measure with id: " + measure.getId(), response.get(0).getMessage());
  }

  @Test
  void importTestCasesReturnValidOutcomeWithAnyDefaultExceptionsWhileUpdatingTestCases() {
    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    doThrow(new NullPointerException())
        .when(testCaseService)
        .updateTestCase(any(), anyString(), anyString(), anyString());
    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(testCase.getPatientId())
            .json(testCaseImportWithMeasureReport)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertFalse(response.get(0).isSuccessful());
    assertEquals(
        "Unable to import test case, please try again. If the error persists, Please contact helpdesk.",
        response.get(0).getMessage());
  }

  @Test
  void importTestCasesReturnInvalidOutcomeWithSpecificExceptionMsgWhileUpdatingTestCases() {
    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    doThrow(new DuplicateTestCaseNameException())
        .when(testCaseService)
        .updateTestCase(any(), anyString(), anyString(), anyString());
    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(testCase.getPatientId())
            .json(testCaseImportWithMeasureReport)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertFalse(response.get(0).isSuccessful());
    assertEquals(
        "The Test Case Group and Title combination is not unique. The combination must be unique (case insensitive, spaces ignored) across all test cases associated with the measure.",
        response.get(0).getMessage());
  }

  @Test
  void importTestCaseReturnValidOutComeWithJsonParseException() {
    var importedJson = "{\n" + "    \"resourceType\": \"Bundle\",\n" + "}";
    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(testCase.getPatientId())
            .json(importedJson)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertFalse(response.get(0).isSuccessful());
    assertEquals(
        "Error while processing Test Case JSON.  Please make sure Test Case JSON is valid.",
        response.get(0).getMessage());
  }

  @Test
  void importTestCaseReturnValidOutComeWithExceptionWhenJsonIsNull() {
    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    var testCaseImportRequest =
        TestCaseImportRequest.builder().patientId(testCase.getPatientId()).json(null).build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertFalse(response.get(0).isSuccessful());
    assertEquals("Test Case file is missing.", response.get(0).getMessage());
  }

  @Test
  void importTestCaseReturnInvalidOutComeWithExceptionWhenJsonIsEmpty() {
    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    var testCaseImportRequest =
        TestCaseImportRequest.builder().patientId(testCase.getPatientId()).json("").build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertFalse(response.get(0).isSuccessful());
    assertEquals("Test Case file is missing.", response.get(0).getMessage());
  }

  @Test
  void importTestCasesReturnValidOutcomesWithMultipleFilesPerPatient() {
    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    TestCase updatedTestCase = testCase;
    updatedTestCase.setJson(testCaseImportWithMeasureReport);

    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(testCase.getPatientId())
            .json(testCaseImportWithMeasureReport)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest, testCaseImportRequest),
            measure.getId(),
            "test.user",
            "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertFalse(response.get(0).isSuccessful());
    assertEquals(
        "Multiple test case files are not supported. Please make sure only one JSON file is in the folder.",
        response.get(0).getMessage());
  }

  @Test
  void importTestCasesCreateNewAllCriteriaMatched() {
    population1 =
        Population.builder()
            .name(PopulationType.INITIAL_POPULATION)
            .definition("Initial Population")
            .build();
    population2 =
        Population.builder().name(PopulationType.DENOMINATOR).definition("Denominator").build();
    population3 =
        Population.builder()
            .name(PopulationType.DENOMINATOR_EXCLUSION)
            .definition("Denominator Exclusion")
            .build();
    population4 =
        Population.builder().name(PopulationType.NUMERATOR).definition("Numerator").build();
    population5 =
        Population.builder()
            .name(PopulationType.DENOMINATOR_EXCEPTION)
            .definition("Numerator Exception")
            .build();
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populations(List.of(population1, population2, population3, population4, population5))
            .populationBasis("Encounter")
            .build();
    measure.setGroups(List.of(group));

    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    TestCase updatedTestCase = testCase;
    updatedTestCase.setJson(testCaseImportWithMeasureReport);

    when(testCaseServiceUtil.getGroupsWithValidPopulations(any(List.class)))
        .thenReturn(List.of(group));
    when(testCaseServiceUtil.matchCriteriaGroups(
            any(List.class), any(List.class), any(TestCase.class)))
        .thenReturn(true);

    doReturn(updatedTestCase)
        .when(testCaseService)
        .updateTestCase(any(), anyString(), anyString(), anyString());
    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(UUID.randomUUID())
            .json(testCaseImportWithMeasureReport)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertTrue(response.get(0).isSuccessful());
  }

  @Test
  void importTestCasesCreateNewWhenMeasureHasNotTestCase() {
    population1 =
        Population.builder()
            .name(PopulationType.INITIAL_POPULATION)
            .definition("Initial Population")
            .build();
    population2 =
        Population.builder().name(PopulationType.DENOMINATOR).definition("Denominator").build();
    population3 =
        Population.builder()
            .name(PopulationType.DENOMINATOR_EXCLUSION)
            .definition("Denominator Exclusiob")
            .build();
    population4 =
        Population.builder().name(PopulationType.NUMERATOR).definition("Numerator").build();
    population5 =
        Population.builder()
            .name(PopulationType.DENOMINATOR_EXCEPTION)
            .definition("Numerator Exception")
            .build();
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populations(List.of(population1, population2, population3, population4, population5))
            .populationBasis("Encounter")
            .build();
    measure.setGroups(List.of(group));

    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    TestCase updatedTestCase = testCase;
    updatedTestCase.setJson(testCaseImportWithMeasureReport);

    when(testCaseServiceUtil.getGroupsWithValidPopulations(any(List.class)))
        .thenReturn(List.of(group));
    when(testCaseServiceUtil.matchCriteriaGroups(
            any(List.class), any(List.class), any(TestCase.class)))
        .thenReturn(true);

    doReturn(updatedTestCase)
        .when(testCaseService)
        .updateTestCase(any(), anyString(), anyString(), anyString());
    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(UUID.randomUUID())
            .json(testCaseImportWithMeasureReport)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertTrue(response.get(0).isSuccessful());
  }

  @Test
  void importTestCasesCreateNewCriteriaNotAllMatched() {
    population1 = Population.builder().name(PopulationType.INITIAL_POPULATION).build();
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
    measure.setGroups(List.of(group));

    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    TestCase updatedTestCase = testCase;
    updatedTestCase.setJson(testCaseImportWithMeasureReport);

    when(testCaseServiceUtil.getGroupsWithValidPopulations(any(List.class)))
        .thenReturn(List.of(group));

    doReturn(updatedTestCase)
        .when(testCaseService)
        .updateTestCase(any(), anyString(), anyString(), anyString());
    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(UUID.randomUUID())
            .json(testCaseImportWithMeasureReport)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertEquals(testCase.getPatientId(), response.get(0).getPatientId());
    assertTrue(response.get(0).isSuccessful());
  }

  @Test
  void importTestCasesCreateNewInvalidImportJson() {
    population1 = Population.builder().name(PopulationType.INITIAL_POPULATION).build();
    population2 = Population.builder().name(PopulationType.DENOMINATOR).build();
    population3 = Population.builder().name(PopulationType.DENOMINATOR_EXCLUSION).build();
    population4 = Population.builder().name(PopulationType.NUMERATOR).build();
    population5 = Population.builder().name(PopulationType.DENOMINATOR_EXCEPTION).build();
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populationBasis("Boolean")
            .populations(List.of(population1, population2, population3, population4, population5))
            .build();
    measure.setGroups(List.of(group));

    measure.setTestCases(List.of(testCase));
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));

    TestCase updatedTestCase = testCase;
    updatedTestCase.setJson(testCaseImportWithMeasureReport);

    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(UUID.randomUUID())
            .json("testInvalidJson")
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertEquals(1, response.size());
    assertFalse(response.get(0).isSuccessful());
  }

  @Test
  void importTestCasesDoesNotCreateNewNoGivenName() throws IOException {
    when(measureRepository.findById(anyString())).thenReturn(Optional.ofNullable(measure));
    String testCaseImportWithoutGivenName =
        removeGivenNameFromJson(testCaseImportWithMeasureReport);
    var testCaseImportRequest =
        TestCaseImportRequest.builder()
            .patientId(UUID.randomUUID())
            .json(testCaseImportWithoutGivenName)
            .build();

    var response =
        testCaseService.importTestCases(
            List.of(testCaseImportRequest), measure.getId(), "test.user", "TOKEN");
    assertFalse(response.get(0).isSuccessful());
    assertEquals("Test Case Title is required.", response.get(0).getMessage());
  }

  private String removeGivenNameFromJson(String testCaseJson) throws IOException {
    String modifiedjsonString = testCaseJson;
    if (!StringUtils.isEmpty(testCaseJson)) {
      ObjectMapper objectMapper = new ObjectMapper();

      JsonNode rootNode = objectMapper.readTree(testCaseJson);
      ArrayNode entryArray = (ArrayNode) rootNode.get("entry");

      for (JsonNode entryNode : entryArray) {
        if ("Patient".equalsIgnoreCase(entryNode.get("resource").get("resourceType").asText())) {

          JsonNode resourceNode = entryNode.get("resource");
          ObjectNode o = (ObjectNode) resourceNode;
          ObjectNode parent = (ObjectNode) o;

          parent.remove("name");

          ByteArrayOutputStream bout = new ByteArrayOutputStream();
          objectMapper.writerWithDefaultPrettyPrinter().writeValue(bout, rootNode);
          modifiedjsonString = bout.toString();
        }
      }
    }
    return modifiedjsonString;
  }

  @Test
  void testUniqueTestCaseName() {
    measure.setTestCases(List.of(testCase));
    TestCase anotherTestCase = testCase.toBuilder().id(null).build();
    assertThrows(
        DuplicateTestCaseNameException.class,
        () -> testCaseService.verifyUniqueTestCaseName(anotherTestCase, measure));
  }

  @Test
  void testUniqueNameCheckCoversNameOnlyCase() {
    TestCase nameOnly = testCase.toBuilder().series(null).build();
    measure.setTestCases(List.of(nameOnly));
    TestCase anotherTestCase = nameOnly.toBuilder().id(null).build();
    assertThrows(
        DuplicateTestCaseNameException.class,
        () -> testCaseService.verifyUniqueTestCaseName(anotherTestCase, measure));
  }

  @Test
  void testUniqueNameCheckIgnoredOnSelf() {
    measure.setTestCases(List.of(testCase));
    TestCase anotherTestCase = testCase.toBuilder().build();
    assertDoesNotThrow(() -> testCaseService.verifyUniqueTestCaseName(anotherTestCase, measure));
  }

  @Test
  void testAssumeUniqueNameOnEmptyList() {
    assertDoesNotThrow(() -> testCaseService.verifyUniqueTestCaseName(testCase, measure));
  }
}
