package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.SecurityConfig;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
            .definition("test definition")
            .experimental(false)
            .transmissionFormat("test transmission format")
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

    doReturn(measure)
        .when(measureService)
        .importMatMeasure(
            any(Measure.class), any(String.class), any(String.class), any(String.class));

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
