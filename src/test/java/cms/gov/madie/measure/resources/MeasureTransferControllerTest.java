package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import cms.gov.madie.measure.repositories.OrganizationRepository;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.ElmTranslatorClient;
import cms.gov.madie.measure.services.MeasureService;
import cms.gov.madie.measure.services.MeasureSetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.measure.*;
import gov.cms.madie.models.common.Version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static gov.cms.madie.packaging.utils.qicore411.ResourceUtils.getData;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class MeasureTransferControllerTest {

  private static final String LAMBDA_TEST_API_KEY = "TOUCH-DOWN";

  private Measure measure;
  private MeasureSet measureSet;

  private List<Organization> organizationList;

  @Mock private MeasureService measureService;
  @Mock private MeasureRepository repository;
  @Mock private MeasureSetRepository measureSetRepository;
  @Mock private MeasureSetService measureSetService;
  @Mock private ActionLogService actionLogService;
  @Mock private ElmTranslatorClient elmTranslatorClient;

  @Mock private OrganizationRepository organizationRepository;
  @Mock private ElmJson elmJson;
  private static final String CQL =
      "library MedicationDispenseTest version '0.0.001' using FHIR version '4.0.1'";
  private static final String ELM_JSON_SUCCESS = "{\"result\":\"success\"}";
  private static final String ELM_JSON_FAIL =
      "{\"errorExceptions\": [{\"Error\":\"UNAUTHORIZED\"}]}";

  @Captor private ArgumentCaptor<ActionType> actionTypeArgumentCaptor;
  @Captor private ArgumentCaptor<Class> targetClassArgumentCaptor;
  @Captor private ArgumentCaptor<String> targetIdArgumentCaptor;
  @Captor private ArgumentCaptor<String> performedByArgumentCaptor;

  @InjectMocks private MeasureTransferController controller;

  MockHttpServletRequest request;

  @BeforeEach
  public void setUp() {
    request = new MockHttpServletRequest();
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
                List.of(
                    new MeasureObservation(
                        "mo-id-1",
                        "ipp",
                        "a description of ipp",
                        null,
                        AggregateMethodType.AVERAGE.getValue())),
                "Description",
                "improvmentNotation",
                "rateAggragation",
                List.of(MeasureGroupTypes.PROCESS),
                "testScoringUnit",
                List.of(new Stratification()),
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
                .endorsementId("test EndorsementId")
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
            .draft(false)
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
            .createdBy("testCreatedBy")
            .measureSetId("abc-pqr-xyz")
            .version(new Version(0, 0, 0))
            .measureName("MedicationDispenseTest")
            .cqlLibraryName("MedicationDispenseTest")
            .model("QI-Core")
            .measureMetaData(measureMetaData)
            .groups(groups)
            .cql(CQL)
            .cqlErrors(false)
            .elmJson(ELM_JSON_SUCCESS)
            .build();

    measureSet = MeasureSet.builder().id(null).measureSetId("abc-pqr-xyz").owner("testID").build();

    organizationList = new ArrayList<>();
    organizationList.add(Organization.builder().name("SB").url("SB Url").build());
    organizationList.add(Organization.builder().name("SB 2").url("SB 2 Url").build());
    organizationList.add(Organization.builder().name("CancerLinQ").url("CancerLinQ Url").build());
    organizationList.add(Organization.builder().name("Innovaccer").url("Innovaccer Url").build());
  }

  @Test
  public void createMeasureSuccessTest() {
    ArgumentCaptor<Measure> persistedMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    doReturn(measure).when(repository).save(any(Measure.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    ResponseEntity<Measure> response =
        controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY);

    verify(repository, times(1)).save(persistedMeasureArgCaptor.capture());
    Measure persistedMeasure = response.getBody();
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
    assertTrue(persistedMeasure.getMeasureMetaData().isDraft());
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
  public void createMeasureSuccessDefaultToDraftTest() {
    ArgumentCaptor<Measure> persistedMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    measure.setMeasureMetaData(null);
    doReturn(measure).when(repository).save(any(Measure.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    ResponseEntity<Measure> response =
        controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY);

    verify(repository, times(1)).save(persistedMeasureArgCaptor.capture());
    Measure persistedMeasure = response.getBody();
    assertNotNull(persistedMeasure);

    assertEquals(measure.getMeasureSetId(), persistedMeasure.getMeasureSetId());
    assertEquals(measure.getMeasureName(), persistedMeasure.getMeasureName());
    assertEquals(measure.getCqlLibraryName(), persistedMeasure.getCqlLibraryName());
    assertEquals(measure.getCql(), persistedMeasure.getCql());
    assertEquals(measure.getGroups().size(), persistedMeasure.getGroups().size());
    assertEquals(
        measure.getGroups().get(0).getPopulations().get(0).getDescription(),
        persistedMeasure.getGroups().get(0).getPopulations().get(0).getDescription());
    assertTrue(persistedMeasure.getMeasureMetaData().isDraft());

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
  public void createMeasureDuplicateCqlLibraryTest() {
    doThrow(new DuplicateKeyException("cqlLibraryName", "CQL library already exists."))
        .when(measureService)
        .checkDuplicateCqlLibraryName(anyString());

    assertThrows(
        DuplicateKeyException.class,
        () -> controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY));
  }

  @Test
  public void createMeasureNoElmJsonErrorTest() {
    ArgumentCaptor<Measure> persistedMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());

    when(elmJson.getJson()).thenReturn(ELM_JSON_SUCCESS);
    doReturn(elmJson)
        .when(elmTranslatorClient)
        .getElmJsonForMatMeasure(CQL, LAMBDA_TEST_API_KEY, null);
    doReturn(false).when(elmTranslatorClient).hasErrors(elmJson);
    doReturn(measure).when(repository).save(any(Measure.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    ResponseEntity<Measure> response =
        controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY);

    verify(repository, times(1)).save(persistedMeasureArgCaptor.capture());
    Measure persistedMeasure = response.getBody();
    assertNotNull(persistedMeasure);

    assertFalse(measure.isCqlErrors());
    assertEquals(measure.getElmJson(), ELM_JSON_SUCCESS);

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(targetClassArgumentCaptor.getValue(), is(equalTo(Measure.class)));
  }

  @Test
  public void createMeasureElmJsonExceptionTest() {
    ArgumentCaptor<Measure> persistedMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    doThrow(
            new CqlElmTranslationServiceException(
                "There was an error calling CQL-ELM translation service for MAT transferred measure",
                null))
        .when(elmTranslatorClient)
        .getElmJsonForMatMeasure(anyString(), anyString(), anyString());

    measure.setCqlErrors(true);
    measure.setElmJson(ELM_JSON_FAIL);
    doReturn(measure).when(repository).save(any(Measure.class));
    doReturn(measureSet).when(measureSetRepository).save(any(MeasureSet.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    ResponseEntity<Measure> response =
        controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY);

    verify(repository, times(1)).save(persistedMeasureArgCaptor.capture());
    Measure persistedMeasure = response.getBody();
    assertNotNull(persistedMeasure);
    assertTrue(measure.isCqlErrors());
    assertEquals(measure.getElmJson(), ELM_JSON_FAIL);

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(targetClassArgumentCaptor.getValue(), is(equalTo(Measure.class)));
  }

  @Test
  public void removesStewardIfOrganizationIsNotFound() {
    measure.getMeasureMetaData().getSteward().setName("Random steward name");
    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    doReturn(measure).when(repository).save(any(Measure.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    ResponseEntity<Measure> response =
        controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY);

    Measure persistedMeasure = response.getBody();
    assertNotNull(persistedMeasure);
    assertNull(persistedMeasure.getMeasureMetaData().getSteward());
  }

  @Test
  public void removesDeveloperIfOrganizationIsNotFound() {
    measure
        .getMeasureMetaData()
        .setDevelopers(List.of(Organization.builder().name("Random steward name").build()));
    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    doReturn(measure).when(repository).save(any(Measure.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    ResponseEntity<Measure> response =
        controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY);

    Measure persistedMeasure = response.getBody();
    assertNotNull(persistedMeasure);
    assertEquals(0, persistedMeasure.getMeasureMetaData().getDevelopers().size());
  }

  @Test
  public void throwsExceptionWhenOrganizationListIsEmpty() {
    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    when(organizationRepository.findAll()).thenReturn(null);

    assertThrows(
        RuntimeException.class,
        () -> controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY));
  }

  @Test
  public void verifyIfOrganizationNamesAreUpdated() {
    // CancerLin Q is updated to CancerLinQ & Innovaccer Anylytics should be updated to Innovaccer
    measure.getMeasureMetaData().getSteward().setName("CancerLin Q");
    measure
        .getMeasureMetaData()
        .getDevelopers()
        .add(Organization.builder().name("CancerLin Q").build());
    measure
        .getMeasureMetaData()
        .getDevelopers()
        .add(Organization.builder().name("Innovaccer Anylytics").build());

    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    doReturn(measure).when(repository).save(any(Measure.class));
    when(organizationRepository.findAll()).thenReturn(organizationList);

    ResponseEntity<Measure> response =
        controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY);

    Measure persistedMeasure = response.getBody();
    assertNotNull(persistedMeasure);
    assertEquals("CancerLinQ", persistedMeasure.getMeasureMetaData().getSteward().getName());
    assertEquals("CancerLinQ Url", persistedMeasure.getMeasureMetaData().getSteward().getUrl());

    assertEquals(3, persistedMeasure.getMeasureMetaData().getDevelopers().size());
    assertEquals(
        "CancerLinQ", persistedMeasure.getMeasureMetaData().getDevelopers().get(1).getName());
    assertEquals(
        "CancerLinQ Url", persistedMeasure.getMeasureMetaData().getDevelopers().get(1).getUrl());
    assertEquals(
        "Innovaccer", persistedMeasure.getMeasureMetaData().getDevelopers().get(2).getName());
    assertEquals(
        "Innovaccer Url", persistedMeasure.getMeasureMetaData().getDevelopers().get(2).getUrl());
  }

  @Test
  public void updateReferenceListIdsTest() {
    ObjectMapper objectMapper = new ObjectMapper();

    try {
      String testMeasureStringQiCore = getData("/test_measure_for_transfer_qi_core.json");
      Measure testMeasureQiCore = objectMapper.readValue(testMeasureStringQiCore, Measure.class);
      controller.updateReferenceListIds(testMeasureQiCore);
      List<Reference> qiCoreRefList = testMeasureQiCore.getMeasureMetaData().getReferences();
      for (Reference reference : qiCoreRefList) {
        assertNotNull(reference.getId());
      }
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }

    try {
      String testMeasureStringQdm = getData("/test_measure_for_transfer_qdm.json");
      Measure testMeasureQDM = objectMapper.readValue(testMeasureStringQdm, Measure.class);
      controller.updateReferenceListIds(testMeasureQDM);
      List<Reference> qdmRefList = testMeasureQDM.getMeasureMetaData().getReferences();
      for (Reference reference : qdmRefList) {
        assertNotNull(reference.getId());
      }
    } catch (JsonProcessingException e) {
      e.printStackTrace(); // Example: Print the stack trace
    }
  }
}
