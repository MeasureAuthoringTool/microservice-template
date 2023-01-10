package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.ElmTranslatorClient;
import cms.gov.madie.measure.services.MeasureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Endorsement;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Reference;
import gov.cms.madie.models.measure.Stratification;
import gov.cms.madie.models.common.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
public class MeasureTransferControllerMvcTest {
  private static final String HARP_ID_HEADER_KEY = "harp-id";
  private static final String HARP_ID_HEADER_VALUE = "XxYyZz";
  private static final String LAMBDA_TEST_API_KEY_HEADER = "api-key";
  private static final String LAMBDA_TEST_API_KEY_HEADER_VALUE = "9202c9fa";

  @MockBean private MeasureRepository measureRepository;
  @MockBean private MeasureService measureService;
  @MockBean private ActionLogService actionLogService;
  @MockBean private ElmTranslatorClient elmTranslatorClient;
  @Mock private ElmJson elmJson;
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

  @BeforeEach
  public void setUp() {
    MeasureMetaData measureMetaData = new MeasureMetaData();
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
                .id("test endorsement id")
                .endorser("test endorser")
                .endorsementId("NQF")
                .build());

    measureMetaData.setSteward("SB");
    measureMetaData.setCopyright("Copyright@SB");
    measureMetaData.setReferences(references);
    measureMetaData.setDraft(true);
    measureMetaData.setEndorsements(endorsements);
    measureMetaData.setRiskAdjustment("test risk adjustment");
    measureMetaData.setDefinition("test definition");
    measureMetaData.setExperimental(false);
    measureMetaData.setTransmissionFormat("test transmission format");
    measureMetaData.setSupplementalDataElements("test supplemental data elements");

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
  }

  @Test
  public void testCreateMeasureSuccess() throws Exception {
    String measureJson = new ObjectMapper().writeValueAsString(measure);

    ArgumentCaptor<Measure> persistedMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);

    doNothing().when(measureService).checkDuplicateCqlLibraryName(any(String.class));
    doReturn(measure).when(measureRepository).save(any(Measure.class));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/measure-transfer/mat-measures")
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
        .checkDuplicateCqlLibraryName(any(String.class));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/measure-transfer/mat-measures")
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
  public void testCreateMeasureFailureWhenInvalidApiKey() throws Exception {
    String measureJson = new ObjectMapper().writeValueAsString(measure);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/measure-transfer/mat-measures")
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

    doNothing().when(measureService).checkDuplicateCqlLibraryName(any(String.class));
    when(elmJson.getJson()).thenReturn(ELM_JSON_SUCCESS);
    when(elmTranslatorClient.getElmJsonForMatMeasure(
            CQL, LAMBDA_TEST_API_KEY_HEADER_VALUE, HARP_ID_HEADER_VALUE))
        .thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(elmJson)).thenReturn(false);
    doReturn(measure).when(measureRepository).save(any(Measure.class));

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/measure-transfer/mat-measures")
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

    doNothing().when(measureService).checkDuplicateCqlLibraryName(any(String.class));
    when(elmJson.getJson()).thenReturn(ELM_JSON_FAIL);
    when(elmTranslatorClient.getElmJsonForMatMeasure(
            CQL, LAMBDA_TEST_API_KEY_HEADER_VALUE, HARP_ID_HEADER_VALUE))
        .thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(elmJson)).thenReturn(true);
    doReturn(measure).when(measureRepository).save(any(Measure.class));

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/measure-transfer/mat-measures")
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

    doNothing().when(measureService).checkDuplicateCqlLibraryName(any(String.class));
    doThrow(
            new CqlElmTranslationServiceException(
                "There was an error calling CQL-ELM translation service for MAT transferred measure",
                null))
        .when(elmTranslatorClient)
        .getElmJsonForMatMeasure(any(String.class), any(String.class), any(String.class));
    doReturn(measure).when(measureRepository).save(any(Measure.class));

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/measure-transfer/mat-measures")
                    .content(measureJson)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(LAMBDA_TEST_API_KEY_HEADER, LAMBDA_TEST_API_KEY_HEADER_VALUE)
                    .header(HARP_ID_HEADER_KEY, HARP_ID_HEADER_VALUE))
            .andExpect(status().isCreated())
            .andReturn();

    assertEquals(HttpStatus.CREATED.value(), result.getResponse().getStatus());
  }
}
