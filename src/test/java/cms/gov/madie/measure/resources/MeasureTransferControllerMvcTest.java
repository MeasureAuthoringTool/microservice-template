package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.SecurityConfig;
import cms.gov.madie.measure.dto.MadieFeatureFlag;
import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import cms.gov.madie.measure.repositories.OrganizationRepository;
import cms.gov.madie.measure.services.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.measure.*;
import gov.cms.madie.models.common.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MeasureTransferController.class})
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class MeasureTransferControllerMvcTest {
  private static final String HARP_ID_HEADER_KEY = "harp-id";
  private static final String HARP_ID_HEADER_VALUE = "XxYyZz";
  private static final String LAMBDA_TEST_API_KEY_HEADER = "api-key";
  private static final String LAMBDA_TEST_API_KEY_HEADER_VALUE = "9202c9fa";

  @MockBean private MeasureRepository measureRepository;
  @MockBean private MeasureSetRepository measureSetRepository;
  @MockBean private MeasureService measureService;
  @MockBean private ActionLogService actionLogService;
  @MockBean private MeasureSetService measureSetService;
  @MockBean private ElmTranslatorClient elmTranslatorClient;
  @MockBean private AppConfigService appConfigService;
  @MockBean private VersionService versionService;
  @MockBean private MeasureTransferService measureTransferService;

  @MockBean private OrganizationRepository organizationRepository;

  private ElmJson elmJson;
  private static final String ELM_JSON_SUCCESS = "{\"result\":\"success\"}";
  private static final String ELM_JSON_FAIL =
      "{\"errorExceptions\": [{\"Error\":\"UNAUTHORIZED\"}]}";
  private static final String CQL =
      "library MedicationDispenseTest version '0.0.001' using FHIR version '4.0.1'";

  @Captor private ArgumentCaptor<ActionType> actionTypeArgumentCaptor;
  @Captor private ArgumentCaptor<Class> targetClassArgumentCaptor;
  @Captor private ArgumentCaptor<String> targetIdArgumentCaptor;
  @Captor private ArgumentCaptor<String> performedByArgumentCaptor;

  @Autowired private MockMvc mockMvc;

  Measure measure;
  Measure qdmMeasure;
  MeasureSet measureSet;

  private List<Organization> organizationList;

  String cmsId;

  @BeforeEach
  public void setUp() {
    Stratification strat = new Stratification();
    strat.setAssociation(PopulationType.INITIAL_POPULATION);
    List<Group> groups =
        List.of(
            new Group(
                "id-abc",
                "Cohort",
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        "test description")),
                List.of(),
                "Description",
                "improvmentNotation",
                "rateAggragation",
                List.of(MeasureGroupTypes.PROCESS),
                "testScoringUnit",
                List.of(strat),
                "populationBasis"));
    List<Reference> references =
        List.of(
            Reference.builder()
                .id("test reference id")
                .referenceText("test reference text")
                .referenceType("DOCUMENT")
                .build());
    List<Endorsement> endorsements =
        List.of(
            Endorsement.builder()
                .endorserSystemId("test endorsement system id")
                .endorser("NQF")
                .endorsementId("testEndorsementId")
                .build());

    List<Organization> developersList = new ArrayList<>();
    developersList.add(Organization.builder().name("SB 2").build());
    developersList.add(Organization.builder().name("SB 3").build());

    var measureMetaData =
        MeasureMetaData.builder()
            .steward(Organization.builder().name("SB").build())
            .developers(developersList)
            .copyright("Copyright@SB")
            .references(references)
            .draft(true)
            .endorsements(endorsements)
            .riskAdjustment("test risk adjustment")
            .definition("test definition")
            .experimental(false)
            .transmissionFormat("test transmission format")
            .supplementalDataElements("test supplemental data elements")
            .build();

    measure =
        Measure.builder()
            .id("testId")
            .versionId("testId")
            .createdBy("testCreatedBy")
            .measureSetId("abc-pqr-xyz")
            .version(new Version(0, 0, 1))
            .measureName("MedicationDispenseTest")
            .cqlLibraryName("MedicationDispenseTest")
            .ecqmTitle("ecqmTitle")
            .model(ModelType.QI_CORE.toString())
            .measureMetaData(measureMetaData)
            .groups(groups)
            .cql("library MedicationDispenseTest version '0.0.001' using FHIR version '4.0.1'")
            .build();

    qdmMeasure =
        Measure.builder()
            .id("qdmMeasureId")
            .versionId("qdmMeasureId")
            .createdBy("testCreatedBy")
            .measureSetId("abc-pqr-xyz")
            .version(new Version(3, 2, 0))
            .measureName("MedicationDispenseTest")
            .cqlLibraryName("MedicationDispenseTest")
            .ecqmTitle("ecqmTitle")
            .model(ModelType.QDM_5_6.toString())
            .measureMetaData(measureMetaData)
            .groups(groups)
            .cql("library MedicationDispenseTest version '3.2.000' using QDM version '5.6'")
            .build();

    organizationList = new ArrayList<>();
    organizationList.add(Organization.builder().name("SB").url("SB Url").build());
    organizationList.add(Organization.builder().name("SB 2").url("SB 2 Url").build());
    organizationList.add(Organization.builder().name("CancerLinQ").url("CancerLinQ Url").build());
    organizationList.add(Organization.builder().name("Innovaccer").url("Innovaccer Url").build());

    measureSet =
        MeasureSet.builder()
            .id("msid-xyz-p12r-12ert")
            .measureSetId("abc-pqr-xyz")
            .owner("user-1")
            .build();

    elmJson = new ElmJson();
    elmJson.setJson(ELM_JSON_SUCCESS);
    elmJson.setXml(ELM_JSON_SUCCESS);

    cmsId = "1";
  }

  @Test
  public void testCreateMeasureSuccess() throws Exception {
    String measureJson = new ObjectMapper().writeValueAsString(measure);

    ArgumentCaptor<Measure> persistedMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);

    when(elmTranslatorClient.getElmJsonForMatMeasure(anyString(), anyString(), anyString()))
        .thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(elmJson)).thenReturn(false);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    doNothing()
        .when(measureSetService)
        .createMeasureSet(anyString(), anyString(), anyString(), anyString());
    doReturn(measure).when(measureRepository).save(any(Measure.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/measure-transfer/mat-measures?cmsId=" + cmsId)
                .content(measureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(LAMBDA_TEST_API_KEY_HEADER, LAMBDA_TEST_API_KEY_HEADER_VALUE)
                .header(HARP_ID_HEADER_KEY, HARP_ID_HEADER_VALUE))
        .andExpect(status().isCreated());

    verify(measureRepository, times(1)).save(persistedMeasureArgCaptor.capture());
    Measure persistedMeasure = persistedMeasureArgCaptor.getValue();
    assertNotNull(persistedMeasure);

    assertEquals(measure.getMeasureSetId(), persistedMeasure.getMeasureSetId());
    assertEquals(measure.getMeasureName(), persistedMeasure.getMeasureName());
    assertEquals(measure.getCqlLibraryName(), persistedMeasure.getCqlLibraryName());
    assertEquals(measure.getCql(), persistedMeasure.getCql());
    assertEquals(measure.getGroups().size(), persistedMeasure.getGroups().size());
    assertEquals(
        measure.getGroups().get(0).getPopulations().get(0).getDescription(),
        persistedMeasure.getGroups().get(0).getPopulations().get(0).getDescription());
    assertEquals(
        measure.getMeasureMetaData().getReferences().get(0).getReferenceText(),
        persistedMeasure.getMeasureMetaData().getReferences().get(0).getReferenceText());
    assertEquals(
        measure.getMeasureMetaData().getEndorsements().get(0).getEndorser(),
        persistedMeasure.getMeasureMetaData().getEndorsements().get(0).getEndorser());
    assertEquals(
        measure.getMeasureMetaData().isDraft(), persistedMeasure.getMeasureMetaData().isDraft());
    assertEquals(
        measure.getMeasureMetaData().getRiskAdjustment(),
        persistedMeasure.getMeasureMetaData().getRiskAdjustment());
    assertEquals(
        measure.getMeasureMetaData().getDefinition(),
        persistedMeasure.getMeasureMetaData().getDefinition());
    assertEquals(
        measure.getMeasureMetaData().isExperimental(),
        persistedMeasure.getMeasureMetaData().isExperimental());
    assertEquals(
        measure.getMeasureMetaData().getTransmissionFormat(),
        persistedMeasure.getMeasureMetaData().getTransmissionFormat());
    assertEquals(
        measure.getMeasureMetaData().getSupplementalDataElements(),
        persistedMeasure.getMeasureMetaData().getSupplementalDataElements());

    assertEquals("SB Url", persistedMeasure.getMeasureMetaData().getSteward().getUrl());
    assertEquals(1, persistedMeasure.getMeasureMetaData().getDevelopers().size());
    assertEquals("SB 2 Url", persistedMeasure.getMeasureMetaData().getDevelopers().get(0).getUrl());

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(targetClassArgumentCaptor.getValue(), is(equalTo(Measure.class)));
    assertThat(actionTypeArgumentCaptor.getValue(), is(equalTo(ActionType.IMPORTED)));
    assertThat(performedByArgumentCaptor.getValue(), is(equalTo("testCreatedBy")));
  }

  @Test
  public void testCreateMeasureFailureWhenDuplicateLibraryName() throws Exception {
    String measureJson = new ObjectMapper().writeValueAsString(measure);

    doThrow(new DuplicateKeyException("cqlLibraryName", "CQL library already exists."))
        .when(measureService)
        .checkDuplicateCqlLibraryName(anyString());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/measure-transfer/mat-measures?cmsId=" + cmsId)
                .content(measureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(LAMBDA_TEST_API_KEY_HEADER, LAMBDA_TEST_API_KEY_HEADER_VALUE)
                .header(HARP_ID_HEADER_KEY, HARP_ID_HEADER_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.cqlLibraryName").value("CQL library already exists."));

    verify(measureService, times(1)).checkDuplicateCqlLibraryName(eq(measure.getCqlLibraryName()));
  }

  @Test
  public void testCreateMeasureUpdateVersion() throws Exception {
    String measureJson = new ObjectMapper().writeValueAsString(qdmMeasure);

    ArgumentCaptor<Measure> persistedMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);

    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    when(elmTranslatorClient.getElmJsonForMatMeasure(anyString(), anyString(), anyString()))
        .thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(elmJson)).thenReturn(false);
    doReturn(true)
        .when(appConfigService)
        .isFlagEnabled(MadieFeatureFlag.ENABLE_QDM_REPEAT_TRANSFER);
    doReturn("library MedicationDispenseTest version '3.2.000' using QDM version '5.6'")
        .when(versionService)
        .generateLibraryContentLine("MedicationDispenseTest", new Version(3, 2, 0));

    doReturn("library MedicationDispenseTest version '0.0.000' using QDM version '5.6'")
        .when(versionService)
        .generateLibraryContentLine("MedicationDispenseTest", new Version(0, 0, 0));
    when(organizationRepository.findAll()).thenReturn(organizationList);
    doReturn(qdmMeasure).when(measureRepository).save(any(Measure.class));
    doNothing()
        .when(measureSetService)
        .createMeasureSet(anyString(), anyString(), anyString(), anyString());

    when(actionLogService.logAction(anyString(), any(), any(), anyString())).thenReturn(true);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/measure-transfer/mat-measures?cmsId=" + cmsId)
                .content(measureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(LAMBDA_TEST_API_KEY_HEADER, LAMBDA_TEST_API_KEY_HEADER_VALUE)
                .header(HARP_ID_HEADER_KEY, HARP_ID_HEADER_VALUE))
        .andExpect(status().isCreated());

    verify(measureRepository, times(1)).save(persistedMeasureArgCaptor.capture());
    Measure persistedMeasure = persistedMeasureArgCaptor.getValue();
    assertNotNull(persistedMeasure);
    assertEquals(qdmMeasure.getMeasureSetId(), persistedMeasure.getMeasureSetId());
    assertEquals(qdmMeasure.getMeasureName(), persistedMeasure.getMeasureName());
    assertEquals(qdmMeasure.getCqlLibraryName(), persistedMeasure.getCqlLibraryName());
    assertEquals(
        "library MedicationDispenseTest version '0.0.000' using QDM version '5.6'",
        persistedMeasure.getCql());

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(targetClassArgumentCaptor.getValue(), is(equalTo(Measure.class)));
    assertThat(actionTypeArgumentCaptor.getValue(), is(equalTo(ActionType.IMPORTED)));
    assertThat(performedByArgumentCaptor.getValue(), is(equalTo("testCreatedBy")));
  }

  @Test
  public void testCreateMeasureFailureWhenInvalidApiKey() throws Exception {
    String measureJson = new ObjectMapper().writeValueAsString(measure);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/measure-transfer/mat-measures?cmsId=" + cmsId)
                    .content(measureJson)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(LAMBDA_TEST_API_KEY_HEADER, "invalid-api-key")
                    .header(HARP_ID_HEADER_KEY, HARP_ID_HEADER_VALUE))
            .andExpect(status().isUnauthorized())
            .andReturn();

    assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getResponse().getStatus());
  }

  @Test
  public void testCreateMeasureSuccessWithNoElmJsonError() throws Exception {
    String measureJson = new ObjectMapper().writeValueAsString(measure);

    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    when(elmTranslatorClient.getElmJsonForMatMeasure(
            CQL, LAMBDA_TEST_API_KEY_HEADER_VALUE, HARP_ID_HEADER_VALUE))
        .thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(elmJson)).thenReturn(false);
    doNothing()
        .when(measureSetService)
        .createMeasureSet(anyString(), anyString(), anyString(), anyString());
    doReturn(measure).when(measureRepository).save(any(Measure.class));
    doReturn(measureSet).when(measureSetRepository).save(any(MeasureSet.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/measure-transfer/mat-measures?cmsId=" + cmsId)
                    .content(measureJson)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(LAMBDA_TEST_API_KEY_HEADER, LAMBDA_TEST_API_KEY_HEADER_VALUE)
                    .header(HARP_ID_HEADER_KEY, HARP_ID_HEADER_VALUE))
            .andExpect(status().isCreated())
            .andReturn();

    assertEquals(HttpStatus.CREATED.value(), result.getResponse().getStatus());
  }

  @Test
  public void testCreateMeasureSuccessWithElmJsonError() throws Exception {
    String measureJson = new ObjectMapper().writeValueAsString(measure);

    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    when(elmTranslatorClient.getElmJsonForMatMeasure(
            CQL, LAMBDA_TEST_API_KEY_HEADER_VALUE, HARP_ID_HEADER_VALUE))
        .thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(elmJson)).thenReturn(true);
    doReturn(measure).when(measureRepository).save(any(Measure.class));
    doReturn(measureSet).when(measureSetRepository).save(any(MeasureSet.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/measure-transfer/mat-measures?cmsId=" + cmsId)
                    .content(measureJson)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(LAMBDA_TEST_API_KEY_HEADER, LAMBDA_TEST_API_KEY_HEADER_VALUE)
                    .header(HARP_ID_HEADER_KEY, HARP_ID_HEADER_VALUE))
            .andExpect(status().isCreated())
            .andReturn();

    assertEquals(HttpStatus.CREATED.value(), result.getResponse().getStatus());
  }

  @Test
  public void testCreateMeasureSuccessWithElmTranslatorException() throws Exception {
    String measureJson = new ObjectMapper().writeValueAsString(measure);

    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    doThrow(
            new CqlElmTranslationServiceException(
                "There was an error calling CQL-ELM translation service for MAT transferred measure",
                null))
        .when(elmTranslatorClient)
        .getElmJsonForMatMeasure(anyString(), anyString(), anyString());
    doReturn(measure).when(measureRepository).save(any(Measure.class));
    doReturn(measureSet).when(measureSetRepository).save(any(MeasureSet.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/measure-transfer/mat-measures?cmsId=" + cmsId)
                    .content(measureJson)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(LAMBDA_TEST_API_KEY_HEADER, LAMBDA_TEST_API_KEY_HEADER_VALUE)
                    .header(HARP_ID_HEADER_KEY, HARP_ID_HEADER_VALUE))
            .andExpect(status().isCreated())
            .andReturn();

    assertEquals(HttpStatus.CREATED.value(), result.getResponse().getStatus());
  }
}
