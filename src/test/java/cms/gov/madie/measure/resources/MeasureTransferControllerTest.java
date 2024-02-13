package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.exceptions.DuplicateMeasureException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import cms.gov.madie.measure.repositories.OrganizationRepository;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.AppConfigService;
import cms.gov.madie.measure.services.ElmTranslatorClient;
import cms.gov.madie.measure.services.MeasureService;
import cms.gov.madie.measure.services.MeasureSetService;
import cms.gov.madie.measure.services.MeasureTransferService;
import cms.gov.madie.measure.services.VersionService;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.measure.*;
import gov.cms.madie.models.common.Version;

import org.apache.commons.collections4.CollectionUtils;
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
  @Mock private AppConfigService appConfigService;
  @Mock private VersionService versionService;
  @Mock private MeasureTransferService measureTransferService;

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

  List<Group> groups;

  @BeforeEach
  public void setUp() {
    request = new MockHttpServletRequest();
    groups =
        List.of(
            new Group(
                "id-abc",
                "Ratio",
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        "test description"),
                    new Population(
                        "id-2",
                        PopulationType.DENOMINATOR,
                        "Denominator",
                        null,
                        "test description denom"),
                    new Population(
                        "id-3",
                        PopulationType.DENOMINATOR_EXCEPTION,
                        "Denominator Exceptions",
                        null,
                        "test description denom excep"),
                    new Population(
                        "id-4",
                        PopulationType.NUMERATOR,
                        "Numerator",
                        null,
                        "test description num")),
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
            .testCases(List.of(TestCase.builder().id("testCaseId").build()))
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
  public void testCreateMeasureDuplicateMeasureExceptionForQiCore() {
    when(measureService.findAllByMeasureSetId(anyString()))
        .thenReturn(List.of(Measure.builder().id("testMeasureId").build()));

    assertThrows(
        DuplicateMeasureException.class,
        () -> controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY));
  }

  @Test
  public void testCreateMeasureSuccessForQDM() {
    measure.setModel(ModelType.QDM_5_6.getValue());
    Measure measureWithSameMeasureSetId =
        Measure.builder().id("testMeasureId").measureSetId("abc-pqr-xyz").build();
    doNothing().when(measureService).checkDuplicateCqlLibraryName(anyString());
    when(organizationRepository.findAll()).thenReturn(organizationList);
    when(measureService.findAllByMeasureSetId(anyString()))
        .thenReturn(List.of(measureWithSameMeasureSetId));
    doNothing().when(measureTransferService).deleteVersionedMeasures(any(List.class));
    when(measureTransferService.overwriteExistingMeasure(any(List.class), any(Measure.class)))
        .thenReturn(measure);
    doReturn(measure).when(repository).save(any(Measure.class));

    ArgumentCaptor<Measure> persistedMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    ResponseEntity<Measure> response =
        controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY);
    verify(repository, times(1)).save(persistedMeasureArgCaptor.capture());

    Measure persistedMeasure = response.getBody();
    assertNotNull(persistedMeasure);

    assertEquals(measure.getMeasureSetId(), persistedMeasure.getMeasureSetId());
    assertEquals(measure.getMeasureName(), persistedMeasure.getMeasureName());
    assertEquals(measure.getCqlLibraryName(), persistedMeasure.getCqlLibraryName());
    assertEquals(measure.getCql(), persistedMeasure.getCql());
    assertEquals("testCaseId", persistedMeasure.getTestCases().get(0).getId());
  }

  @Test
  public void testReorderGroupPopulationsRatio() {
    Group copiedGroup = Group.builder().populations(groups.get(0).getPopulations()).build();

    controller.reorderGroupPopulations(groups);

    assertEquals(1, groups.size());
    assertEquals(4, copiedGroup.getPopulations().size());
    assertEquals(5, groups.get(0).getPopulations().size());
    assertEquals(
        copiedGroup.getPopulations().get(0).getId(), groups.get(0).getPopulations().get(0).getId());
    assertEquals("Initial Population", groups.get(0).getPopulations().get(0).getDefinition());
    assertEquals(
        copiedGroup.getPopulations().get(1).getId(), groups.get(0).getPopulations().get(1).getId());
    assertEquals("Denominator", groups.get(0).getPopulations().get(1).getDefinition());
    // DENOMINATOR_EXCEPTION is not in the reordered group population
    assertNotEquals(
        copiedGroup.getPopulations().get(2).getId(), groups.get(0).getPopulations().get(2).getId());
    assertEquals(
        "DENOMINATOR_EXCLUSION", groups.get(0).getPopulations().get(2).getName().toString());
    assertEquals(
        copiedGroup.getPopulations().get(3).getId(), groups.get(0).getPopulations().get(3).getId());
    assertEquals("Numerator", groups.get(0).getPopulations().get(3).getDefinition());
    assertEquals(
        PopulationType.NUMERATOR_EXCLUSION, groups.get(0).getPopulations().get(4).getName());
  }

  @Test
  public void testReorderGroupPopulationsProportion() {
    groups.get(0).setScoring(MeasureScoring.PROPORTION.toString());
    Group copiedGroup = Group.builder().populations(groups.get(0).getPopulations()).build();

    controller.reorderGroupPopulations(groups);

    assertEquals(1, groups.size());
    assertEquals(4, copiedGroup.getPopulations().size());
    assertEquals(6, groups.get(0).getPopulations().size());
    assertEquals(
        copiedGroup.getPopulations().get(0).getId(), groups.get(0).getPopulations().get(0).getId());
    assertEquals("Initial Population", groups.get(0).getPopulations().get(0).getDefinition());
    assertEquals(
        copiedGroup.getPopulations().get(1).getId(), groups.get(0).getPopulations().get(1).getId());
    assertEquals("Denominator", groups.get(0).getPopulations().get(1).getDefinition());
    assertNotEquals(
        copiedGroup.getPopulations().get(2).getId(), groups.get(0).getPopulations().get(2).getId());
    assertEquals(
        "DENOMINATOR_EXCLUSION", groups.get(0).getPopulations().get(2).getName().toString());
    assertEquals(
        copiedGroup.getPopulations().get(3).getId(), groups.get(0).getPopulations().get(3).getId());
    assertEquals("Numerator", groups.get(0).getPopulations().get(3).getDefinition());
    assertEquals("NUMERATOR_EXCLUSION", groups.get(0).getPopulations().get(4).getName().toString());
    // DENOMINATOR_EXCEPTION is in the reordered group population
    assertEquals(
        "DENOMINATOR_EXCEPTION", groups.get(0).getPopulations().get(5).getName().toString());
  }

  @Test
  public void testReorderGroupPopulationsCohort() {
    groups.get(0).setScoring("Cohort");

    controller.reorderGroupPopulations(groups);

    assertEquals(1, groups.size());
    assertEquals(1, groups.get(0).getPopulations().size());
  }

  @Test
  public void testReorderGroupPopulationsEmptyGroups() {
    List<Group> reorderedGroups = List.of();
    controller.reorderGroupPopulations(reorderedGroups);
    assertTrue(CollectionUtils.isEmpty(reorderedGroups));
  }

  @Test
  public void testReorderGroupPopulationsEmptyPopulations() {
    List<Group> reorderGroups = List.of(Group.builder().build());
    controller.reorderGroupPopulations(reorderGroups);
    assertFalse(CollectionUtils.isEmpty(reorderGroups));
  }

  @Test
  public void testReorderGroupPopulationsForCV() {
    Group copiedGroup =
        Group.builder()
            .scoring("Continuous Variable")
            .populations(
                List.of(
                    groups.get(0).getPopulations().get(0),
                    Population.builder()
                        .id("id-6")
                        .definition(PopulationType.MEASURE_POPULATION.name())
                        .description("test description measure population")
                        .build()))
            .measureObservations(groups.get(0).getMeasureObservations())
            .build();
    List<Group> reorderedGroups = List.of(copiedGroup);

    controller.reorderGroupPopulations(reorderedGroups);

    assertEquals(1, reorderedGroups.size());
    assertEquals(3, reorderedGroups.get(0).getPopulations().size());
    assertEquals(1, reorderedGroups.get(0).getMeasureObservations().size());
  }
}
