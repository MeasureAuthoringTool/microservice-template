package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.exceptions.InvalidCmsIdException;
import cms.gov.madie.measure.exceptions.InvalidReturnTypeException;
import cms.gov.madie.measure.exceptions.InvalidVersionIdException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.GroupService;
import cms.gov.madie.measure.services.MeasureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({MeasureController.class})
@ActiveProfiles("test")
public class MeasureControllerMvcTest {

  @MockBean private MeasureRepository measureRepository;
  @MockBean private MeasureService measureService;
  @MockBean private GroupService groupService;
  @MockBean private ActionLogService actionLogService;

  @Autowired private MockMvc mockMvc;
  @Captor private ArgumentCaptor<Measure> measureArgumentCaptor;

  private static final String TEST_USER_ID = "test-okta-user-id-123";
  @Captor ArgumentCaptor<Group> groupCaptor;
  @Captor ArgumentCaptor<String> measureIdCaptor;
  @Captor ArgumentCaptor<String> usernameCaptor;
  @Captor ArgumentCaptor<PageRequest> pageRequestCaptor;
  @Captor ArgumentCaptor<Boolean> activeCaptor;
  @Captor private ArgumentCaptor<ActionType> actionTypeArgumentCaptor;
  @Captor private ArgumentCaptor<Class> targetClassArgumentCaptor;
  @Captor private ArgumentCaptor<String> targetIdArgumentCaptor;
  @Captor private ArgumentCaptor<String> performedByArgumentCaptor;

  private static final String MODEL = ModelType.QI_CORE.toString();

  @Test
  public void testGrantAccess() throws Exception {
    String measureId = "f225481c-921e-4015-9e14-e5046bfac9ff";

    doReturn(true)
        .when(measureService)
        .grantAccess(eq(measureId), eq("akinsgre"), eq(TEST_USER_ID));
    mockMvc
        .perform(
            put("/measures/" + measureId + "/grant/?userid=akinsgre")
                .with(user(TEST_USER_ID))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("akinsgre granted access to Measure successfully."));

    verify(measureService, times(1)).grantAccess(eq(measureId), eq("akinsgre"), eq(TEST_USER_ID));

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(actionTypeArgumentCaptor.getValue(), is(equalTo(ActionType.UPDATED)));
    assertThat(performedByArgumentCaptor.getValue(), is(equalTo(TEST_USER_ID)));
  }

  @Test
  public void testUpdatePassed() throws Exception {
    String measureId = "f225481c-921e-4015-9e14-e5046bfac9ff";
    String measureName = "TestMeasure";
    String steward = "d0cc18ce-63fd-4b94-b713-c1d9fd6b2329";
    String description = "TestDescription";
    String copyright = "TestCopyright";
    String disclaimer = "TestDisclaimer";
    String rationale = "TestRationale";
    String author = "TestAuthor";
    String guidance = "TestGuidance";
    String libName = "TestLib";
    String ecqmTitle = "ecqmTitle";

    Measure priorMeasure = new Measure();
    priorMeasure.setId(measureId);
    priorMeasure.setMeasureName(measureName);
    priorMeasure.setCqlLibraryName(libName);
    priorMeasure.setModel(MODEL);
    priorMeasure.setVersionId(measureId);

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(priorMeasure));
    when(measureRepository.save(any(Measure.class))).thenReturn(mock(Measure.class));

    final String measureAsJson =
        "{\"id\": \"%s\", \"measureName\": \"%s\", \"cqlLibraryName\":\"%s\", \"ecqmTitle\":\"%s\", \"measureMetaData\": { \"steward\" : \"%s\", \"description\" : \"%s\", \"copyright\" : \"%s\", \"disclaimer\" : \"%s\", \"rationale\" : \"%s\", \"author\" : \"%s\", \"guidance\" : \"%s\"}, \"model\":\"%s\", \"versionId\":\"%s\"}"
            .formatted(
                measureId,
                measureName,
                libName,
                ecqmTitle,
                steward,
                description,
                copyright,
                disclaimer,
                rationale,
                author,
                guidance,
                MODEL,
                measureId);
    mockMvc
        .perform(
            put("/measures/" + measureId)
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().string("Measure updated successfully."));

    verify(measureRepository, times(1)).findById(eq(measureId));
    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository);
    Measure savedMeasure = measureArgumentCaptor.getValue();
    assertNotNull(savedMeasure.getMeasureMetaData());
    assertEquals(measureName, savedMeasure.getMeasureName());
    assertEquals(steward, savedMeasure.getMeasureMetaData().getSteward());
    assertEquals(description, savedMeasure.getMeasureMetaData().getDescription());
    assertEquals(copyright, savedMeasure.getMeasureMetaData().getCopyright());
    assertEquals(disclaimer, savedMeasure.getMeasureMetaData().getDisclaimer());
    assertEquals(rationale, savedMeasure.getMeasureMetaData().getRationale());
    assertEquals(author, savedMeasure.getMeasureMetaData().getAuthor());
    assertEquals(guidance, savedMeasure.getMeasureMetaData().getGuidance());
    assertEquals(MODEL, savedMeasure.getModel());
    assertEquals(measureId, savedMeasure.getVersionId());
    assertNotNull(savedMeasure.getLastModifiedAt());
    assertEquals(TEST_USER_ID, savedMeasure.getLastModifiedBy());
    int lastModCompareTo =
        savedMeasure.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals(1, lastModCompareTo);

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(actionTypeArgumentCaptor.getValue(), is(equalTo(ActionType.UPDATED)));
    assertThat(performedByArgumentCaptor.getValue(), is(equalTo(TEST_USER_ID)));
  }

  @Test
  public void testUpdatePassedLogDeleted() throws Exception {
    String measureId = "f225481c-921e-4015-9e14-e5046bfac9ff";
    String measureName = "TestMeasure";
    String steward = "d0cc18ce-63fd-4b94-b713-c1d9fd6b2329";
    String description = "TestDescription";
    String copyright = "TestCopyright";
    String disclaimer = "TestDisclaimer";
    String rationale = "TestRationale";
    String author = "TestAuthor";
    String guidance = "TestGuidance";
    String libName = "TestLib";
    String ecqmTitle = "ecqmTitle";

    Measure priorMeasure = new Measure();
    priorMeasure.setId(measureId);
    priorMeasure.setMeasureName(measureName);
    priorMeasure.setCqlLibraryName(libName);
    priorMeasure.setEcqmTitle(ecqmTitle);
    priorMeasure.setModel(MODEL);
    priorMeasure.setVersionId(measureId);

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(priorMeasure));
    when(measureRepository.save(any(Measure.class))).thenReturn(mock(Measure.class));

    final String measureAsJson =
        "{\"id\": \"%s\", \"active\": \"%s\", \"measureName\": \"%s\", \"cqlLibraryName\":\"%s\", \"ecqmTitle\":\"%s\", \"measureMetaData\": { \"steward\" : \"%s\", \"description\" : \"%s\", \"copyright\" : \"%s\", \"disclaimer\" : \"%s\", \"rationale\" : \"%s\", \"author\" : \"%s\", \"guidance\" : \"%s\"}, \"model\":\"%s\", \"versionId\":\"%s\"}"
            .formatted(
                measureId,
                false,
                measureName,
                libName,
                ecqmTitle,
                steward,
                description,
                copyright,
                disclaimer,
                rationale,
                author,
                guidance,
                MODEL,
                measureId);
    mockMvc
        .perform(
            put("/measures/" + measureId)
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().string("Measure updated successfully."));

    verify(measureRepository, times(1)).findById(eq(measureId));
    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository);
    Measure savedMeasure = measureArgumentCaptor.getValue();
    assertNotNull(savedMeasure.getMeasureMetaData());
    assertEquals(measureName, savedMeasure.getMeasureName());
    assertEquals(steward, savedMeasure.getMeasureMetaData().getSteward());
    assertEquals(description, savedMeasure.getMeasureMetaData().getDescription());
    assertEquals(copyright, savedMeasure.getMeasureMetaData().getCopyright());
    assertEquals(disclaimer, savedMeasure.getMeasureMetaData().getDisclaimer());
    assertEquals(rationale, savedMeasure.getMeasureMetaData().getRationale());
    assertEquals(author, savedMeasure.getMeasureMetaData().getAuthor());
    assertEquals(guidance, savedMeasure.getMeasureMetaData().getGuidance());
    assertEquals(MODEL, savedMeasure.getModel());
    assertNotNull(savedMeasure.getLastModifiedAt());
    assertEquals(TEST_USER_ID, savedMeasure.getLastModifiedBy());
    int lastModCompareTo =
        savedMeasure.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals(1, lastModCompareTo);

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(targetClassArgumentCaptor.getValue(), is(equalTo(Measure.class)));
    assertThat(actionTypeArgumentCaptor.getValue(), is(equalTo(ActionType.DELETED)));
    assertThat(performedByArgumentCaptor.getValue(), is(equalTo(TEST_USER_ID)));
  }

  @Test
  public void testNewMeasureNameMustNotBeNull() throws Exception {
    final String measureAsJson = "{  }";
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.measureName").value("Measure Name is required."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureNameMustNotBeNull() throws Exception {
    final String measureAsJson = "{ \"id\": \"m1234\" }";
    mockMvc
        .perform(
            put("/measures/m1234")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.measureName").value("Measure Name is required."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureNameMustNotBeEmpty() throws Exception {
    final String measureAsJson = "{ \"measureName\":\"\" }";
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.measureName").value("Measure Name is required."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureNameMustNotBeEmpty() throws Exception {
    final String measureAsJson = "{ \"id\": \"m1234\", \"measureName\":\"\" }";
    mockMvc
        .perform(
            put("/measures/m1234")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.measureName").value("Measure Name is required."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureFailsIfUnderscoreInMeasureName() throws Exception {
    final String measureAsJson =
        "{ \"measureName\":\"A_Name\", \"cqlLibraryName\":\"ALib\" , \"versionId\":\"versionId\"}";
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.measureName")
                .value("Measure Name can not contain underscores."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureFailsIfUnderscoreInMeasureName() throws Exception {
    final String measureAsJson =
        "{ \"id\": \"m1234\", \"measureName\":\"A_Name\", \"cqlLibraryName\":\"ALib\", \"versionId\":\"versionId\" }";
    mockMvc
        .perform(
            put("/measures/m1234")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.measureName")
                .value("Measure Name can not contain underscores."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureNameMaxLengthFailed() throws Exception {
    final String measureName = "A".repeat(501);
    final String measureAsJson =
        "{ \"measureName\":\"%s\", \"cqlLibraryName\":\"ALib\", \"versionId\":\"versionId\"  }"
            .formatted(measureName);
    verifyNoInteractions(measureRepository);
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.measureName")
                .value("Measure Name can not be more than 500 characters."));
  }

  @Test
  public void testUpdateMeasureNameMaxLengthFailed() throws Exception {
    final String measureName = "A".repeat(501);
    final String measureAsJson =
        "{ \"id\": \"m1234\", \"measureName\":\"%s\", \"cqlLibraryName\":\"ALib\", \"versionId\":\"versionId\" }"
            .formatted(measureName);
    mockMvc
        .perform(
            put("/measures/m1234")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.measureName")
                .value("Measure Name can not be more than 500 characters."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureECQMTitleNullFailed() throws Exception {
    final String measureAsJson =
        "{ \"id\": \"m1234\", \"measureName\":\"TestMeasure\", \"cqlLibraryName\":\"ALib\",\"model\": \"%s\", \"versionId\":\"versionId\" }"
            .formatted(MODEL);
    mockMvc
        .perform(
            put("/measures/m1234")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.ecqmTitle").value("eCQM Abbreviated Title is required."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureECQMTitleMaxLengthFailed() throws Exception {
    final String ecqmTitle = "A".repeat(33);
    final String measureAsJson =
        "{ \"id\": \"m1234\", \"measureName\":\"TestMeasure\", \"cqlLibraryName\":\"ALib\", \"ecqmTitle\":\"%s\", \"model\":\"%s\", \"versionId\":\"versionId\" }"
            .formatted(ecqmTitle, MODEL);
    mockMvc
        .perform(
            put("/measures/m1234")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.ecqmTitle")
                .value("eCQM Abbreviated Title cannot be more than 32 characters."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasurePassed() throws Exception {
    Measure saved = new Measure();
    String measureId = "id123";
    saved.setId(measureId);
    String measureName = "SavedMeasure";
    String libraryName = "Lib1";
    String ecqmTitle = "ecqmTitle";
    saved.setMeasureName(measureName);
    saved.setCqlLibraryName(libraryName);
    saved.setModel(MODEL);
    saved.setEcqmTitle(ecqmTitle);
    saved.setVersionId(measureId);
    when(measureRepository.save(any(Measure.class))).thenReturn(saved);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(any(String.class));

    final String measureAsJson =
        "{\"measureName\": \"%s\", \"cqlLibraryName\": \"%s\" , \"ecqmTitle\": \"%s\", \"model\": \"%s\", \"versionId\":\"%s\"}"
            .formatted(measureName, libraryName, ecqmTitle, MODEL, measureId);

    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.measureName").value(measureName))
        .andExpect(jsonPath("$.cqlLibraryName").value(libraryName))
        .andExpect(jsonPath("$.ecqmTitle").value(ecqmTitle))
        .andExpect(jsonPath("$.model").value(MODEL))
        .andExpect(jsonPath("$.id").value(measureId))
        .andExpect(jsonPath("$.versionId").value(measureId));

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository);
    Measure savedMeasure = measureArgumentCaptor.getValue();
    assertEquals(measureName, savedMeasure.getMeasureName());
    assertEquals(libraryName, savedMeasure.getCqlLibraryName());
    assertEquals(ecqmTitle, savedMeasure.getEcqmTitle());
    assertEquals(MODEL, savedMeasure.getModel());
    assertEquals(TEST_USER_ID, savedMeasure.getCreatedBy());
    assertEquals(TEST_USER_ID, savedMeasure.getLastModifiedBy());
    assertEquals(measureId, savedMeasure.getVersionId());
    assertNotNull(savedMeasure.getCreatedAt());
    assertNotNull(savedMeasure.getLastModifiedAt());

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(actionTypeArgumentCaptor.getValue(), is(equalTo(ActionType.CREATED)));
    assertThat(performedByArgumentCaptor.getValue(), is(equalTo(TEST_USER_ID)));
  }

  @Test
  public void testNewMeasureFailsIfDuplicatedLibraryName() throws Exception {
    Measure existing = new Measure();
    String measureId = "id123";
    existing.setId(measureId);
    existing.setMeasureName("ExistingMeasure");
    String cqlLibraryName = "ExistingLibrary";
    existing.setCqlLibraryName(cqlLibraryName);
    existing.setModel(MODEL);
    String ecqmTitle = "ecqmTitle";
    existing.setEcqmTitle(ecqmTitle);
    existing.setVersionId(measureId);

    doThrow(
            new DuplicateKeyException(
                "cqlLibraryName", "CQL library with given name already exists."))
        .when(measureService)
        .checkDuplicateCqlLibraryName(any(String.class));

    final String newMeasureAsJson =
        "{\"measureName\": \"NewMeasure\", \"cqlLibraryName\": \"%s\", \"ecqmTitle\": \"%s\",\"model\":\"%s\",\"versionId\":\"%s\"}"
            .formatted(cqlLibraryName, ecqmTitle, MODEL, measureId);
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(newMeasureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.cqlLibraryName")
                .value("CQL library with given name already exists."));

    verify(measureService, times(1)).checkDuplicateCqlLibraryName(eq(cqlLibraryName));
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureFailsIfDuplicatedLibraryName() throws Exception {
    Measure priorMeasure = new Measure();
    priorMeasure.setId("id0");
    priorMeasure.setMeasureName("TestMeasure");
    priorMeasure.setCqlLibraryName("TestMeasureLibrary");
    priorMeasure.setModel(MODEL);
    priorMeasure.setEcqmTitle("ecqmTitle");
    priorMeasure.setVersionId(priorMeasure.getId());
    when(measureRepository.findById(eq(priorMeasure.getId())))
        .thenReturn(Optional.of(priorMeasure));

    Measure existingMeasure = new Measure();
    existingMeasure.setId("id1");
    existingMeasure.setMeasureName("ExistingMeasure");
    existingMeasure.setCqlLibraryName("ExistingMeasureLibrary");
    existingMeasure.setEcqmTitle("ecqmTitle");
    existingMeasure.setVersionId(existingMeasure.getId());
    doThrow(
            new DuplicateKeyException(
                "cqlLibraryName", "CQL library with given name already exists."))
        .when(measureService)
        .checkDuplicateCqlLibraryName(any(String.class));

    final String updatedMeasureAsJson =
        "{\"id\": \"%s\",\"measureName\": \"%s\", \"cqlLibraryName\": \"%s\", \"ecqmTitle\": \"%s\", \"model\":\"%s\",\"versionId\":\"%s\"}"
            .formatted(
                priorMeasure.getId(),
                priorMeasure.getMeasureName(),
                existingMeasure.getCqlLibraryName(),
                priorMeasure.getEcqmTitle(),
                priorMeasure.getModel(),
                priorMeasure.getVersionId());
    mockMvc
        .perform(
            put("/measures/" + priorMeasure.getId())
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(updatedMeasureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.cqlLibraryName")
                .value("CQL library with given name already exists."));

    verify(measureRepository, times(1)).findById(eq(priorMeasure.getId()));
    verify(measureService, times(1))
        .checkDuplicateCqlLibraryName(eq(existingMeasure.getCqlLibraryName()));
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureFailsIfInvalidVersionId() throws Exception {
    Measure priorMeasure = new Measure();
    priorMeasure.setId("id0");
    priorMeasure.setMeasureName("TestMeasure");
    priorMeasure.setCqlLibraryName("TestMeasureLibrary");
    priorMeasure.setModel(MODEL);
    priorMeasure.setEcqmTitle("ecqmTitle");
    priorMeasure.setVersionId(priorMeasure.getId());
    when(measureRepository.findById(eq(priorMeasure.getId())))
        .thenReturn(Optional.of(priorMeasure));

    Measure existingMeasure = new Measure();
    existingMeasure.setId("id0");
    existingMeasure.setMeasureName("ExistingMeasure");
    existingMeasure.setCqlLibraryName("ExistingMeasureLibrary");
    existingMeasure.setEcqmTitle("ecqmTitle");
    existingMeasure.setVersionId("newVersionID");
    doThrow(new InvalidVersionIdException("newVersionId"))
        .when(measureService)
        .checkVersionIdChanged(existingMeasure.getVersionId(), priorMeasure.getVersionId());

    final String updatedMeasureAsJson =
        "{\"id\": \"%s\",\"measureName\": \"%s\", \"cqlLibraryName\": \"%s\", \"ecqmTitle\": \"%s\", \"model\":\"%s\",\"versionId\":\"%s\"}"
            .formatted(
                priorMeasure.getId(),
                priorMeasure.getMeasureName(),
                priorMeasure.getCqlLibraryName(),
                priorMeasure.getEcqmTitle(),
                priorMeasure.getModel(),
                existingMeasure.getVersionId());
    mockMvc
        .perform(
            put("/measures/" + priorMeasure.getId())
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(updatedMeasureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest());

    verify(measureRepository, times(1)).findById(eq(priorMeasure.getId()));
    verify(measureService, times(1))
        .checkVersionIdChanged(existingMeasure.getVersionId(), priorMeasure.getVersionId());
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureFailsIfInvalidCMSId() throws Exception {
    Measure priorMeasure = new Measure();
    priorMeasure.setId("id0");
    priorMeasure.setMeasureName("TestMeasure");
    priorMeasure.setCqlLibraryName("TestMeasureLibrary");
    priorMeasure.setModel(MODEL);
    priorMeasure.setEcqmTitle("ecqmTitle");
    priorMeasure.setVersionId(priorMeasure.getId());
    priorMeasure.setCmsId("testCmsId");
    when(measureRepository.findById(eq(priorMeasure.getId())))
        .thenReturn(Optional.of(priorMeasure));

    Measure existingMeasure = new Measure();
    existingMeasure.setId("id0");
    existingMeasure.setMeasureName("ExistingMeasure");
    existingMeasure.setCqlLibraryName("ExistingMeasureLibrary");
    existingMeasure.setEcqmTitle("ecqmTitle");
    existingMeasure.setVersionId(priorMeasure.getVersionId());
    existingMeasure.setCmsId("newCmsId");
    doThrow(new InvalidCmsIdException(existingMeasure.getCmsId()))
        .when(measureService)
        .checkCmsIdChanged(existingMeasure.getCmsId(), priorMeasure.getCmsId());

    final String updatedMeasureAsJson =
        "{\"id\": \"%s\",\"measureName\": \"%s\", \"cqlLibraryName\": \"%s\", \"ecqmTitle\": \"%s\", \"model\":\"%s\",\"versionId\":\"%s\",\"cmsId\":\"%s\"}"
            .formatted(
                priorMeasure.getId(),
                priorMeasure.getMeasureName(),
                priorMeasure.getCqlLibraryName(),
                priorMeasure.getEcqmTitle(),
                priorMeasure.getModel(),
                priorMeasure.getVersionId(),
                existingMeasure.getCmsId());
    mockMvc
        .perform(
            put("/measures/" + priorMeasure.getId())
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(updatedMeasureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest());

    verify(measureRepository, times(1)).findById(eq(priorMeasure.getId()));
    verify(measureService, times(1))
        .checkCmsIdChanged(existingMeasure.getCmsId(), priorMeasure.getCmsId());
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureNoUnderscore() throws Exception {
    final String measureAsJson =
        "{ \"id\": \"m1234\", \"measureName\":\"A_Name\", \"cqlLibraryName\":\"ALib\", \"versionId\":\"versionId\" }";
    mockMvc
        .perform(
            put("/measures/m1234")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.measureName")
                .value("Measure Name can not contain underscores."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureFailsIfCqlLibaryNameStartsWithLowerCase() throws Exception {
    final String measureAsJson =
        "{ \"measureName\":\"AName\", \"cqlLibraryName\":\"aLib\", \"ecqmTitle\":\"ecqmTitle\", \"versionId\":\"versionId\" }";
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.cqlLibraryName")
                .value("Measure Library Name is invalid."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureFailsIfCqlLibaryNameStartsWithLowerCase() throws Exception {
    final String measureAsJson =
        "{ \"id\": \"m1234\", \"measureName\":\"AName\", \"cqlLibraryName\":\"aLib\", \"ecqmTitle\":\"ecqmTitle\", \"versionId\":\"versionId\" }";
    mockMvc
        .perform(
            put("/measures/m1234")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.cqlLibraryName")
                .value("Measure Library Name is invalid."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureFailsIfCqlLibraryNameHasQuotes() throws Exception {
    final String measureAsJson =
        "{ \"measureName\":\"AName\", \"cqlLibraryName\":\"ALi''b\", \"ecqmTitle\":\"ecqmTitle\", \"versionId\":\"versionId\" }";
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.cqlLibraryName")
                .value("Measure Library Name is invalid."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureFailsIfCqlLibraryNameHasUnderscore() throws Exception {
    final String measureAsJson =
        "{ \"measureName\":\"AName\", \"cqlLibraryName\":\"ALi_'b\", \"ecqmTitle\":\"ecqmTitle\", \"versionId\":\"versionId\" }";
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.cqlLibraryName")
                .value("Measure Library Name is invalid."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasurePassesIfCqlLibraryNameStartsWithCapitalCharAndFollowedByAlphaNumeric()
      throws Exception {
    Measure saved = new Measure();
    String measureId = "id123";
    saved.setId(measureId);
    String measureName = "SavedMeasure";
    String libraryName = "ALi12aAccllklk6U";
    String ecqmTitle = "ecqmTitle";
    saved.setMeasureName(measureName);
    saved.setCqlLibraryName(libraryName);
    saved.setEcqmTitle(ecqmTitle);
    saved.setModel(MODEL);
    saved.setVersionId(measureId);

    when(measureRepository.save(any(Measure.class))).thenReturn(saved);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(any(String.class));

    final String measureAsJson =
        "{ \"measureName\":\"%s\", \"cqlLibraryName\":\"%s\", \"ecqmTitle\":\"%s\", \"model\":\"%s\", \"versionId\":\"%s\"}"
            .formatted(measureName, libraryName, ecqmTitle, MODEL, measureId);
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.measureName").value(measureName))
        .andExpect(jsonPath("$.cqlLibraryName").value(libraryName))
        .andExpect(jsonPath("$.ecqmTitle").value(ecqmTitle))
        .andExpect(jsonPath("$.model").value(MODEL))
        .andExpect(jsonPath("$.id").value(measureId))
        .andExpect(jsonPath("$.versionId").value(measureId));

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void
      testUpdateMeasurePassesIfCqlLibraryNameStartsWithCapitalCharAndFollowedByAlphaNumeric()
          throws Exception {
    String measureId = "id123";
    Measure saved = new Measure();
    saved.setId(measureId);
    String measureName = "SavedMeasure";
    String libraryName = "ALi12aAccllklk6U";
    String ecqmTitle = "ecqmTitle";
    saved.setMeasureName(measureName);
    saved.setCqlLibraryName(libraryName);
    saved.setEcqmTitle(ecqmTitle);
    saved.setModel(MODEL);
    saved.setVersionId(measureId);

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(saved));
    when(measureRepository.save(any(Measure.class))).thenReturn(saved);

    final String measureAsJson =
        "{ \"id\": \"%s\", \"measureName\":\"%s\", \"cqlLibraryName\":\"%s\" , \"ecqmTitle\":\"%s\", \"model\":\"%s\", \"versionId\":\"%s\"}"
            .formatted(measureId, measureName, libraryName, ecqmTitle, MODEL, measureId);
    mockMvc
        .perform(
            put("/measures/" + measureId)
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().string("Measure updated successfully."));

    verify(measureRepository, times(1)).findById(eq(measureId));
    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureReturnsBadRequestWhenIdsDoNotMatch() throws Exception {
    String measureId = "id123";
    Measure saved = new Measure();
    saved.setId(measureId);
    String measureName = "SavedMeasure";
    String libraryName = "ALi12aAccllklk6U";
    saved.setMeasureName(measureName);
    saved.setCqlLibraryName(libraryName);
    saved.setModel(MODEL);
    String scoring = MeasureScoring.CONTINUOUS_VARIABLE.toString();

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(saved));
    when(measureRepository.save(any(Measure.class))).thenReturn(saved);

    final String measureAsJson =
        "{ \"id\": \"id1234\", \"measureName\":\"%s\", \"cqlLibraryName\":\"%s\", \"model\":\"%s\", \"measureScoring\":\"%s\"}"
            .formatted(measureName, libraryName, MODEL, scoring);
    mockMvc
        .perform(
            put("/measures/" + measureId)
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureReturnsBadRequestWhenIdInObjectIsNull() throws Exception {
    String measureId = "id123";
    Measure saved = new Measure();
    saved.setId(measureId);
    String measureName = "SavedMeasure";
    String libraryName = "ALi12aAccllklk6U";
    saved.setMeasureName(measureName);
    saved.setCqlLibraryName(libraryName);
    saved.setModel(MODEL);
    String scoring = MeasureScoring.CONTINUOUS_VARIABLE.toString();

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(saved));
    when(measureRepository.save(any(Measure.class))).thenReturn(saved);

    final String measureAsJson =
        "{ \"id\": null, \"measureName\":\"%s\", \"cqlLibraryName\":\"%s\", \"model\":\"%s\", \"measureScoring\":\"%s\"}"
            .formatted(measureName, libraryName, MODEL, scoring);
    mockMvc
        .perform(
            put("/measures/" + measureId)
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureFailsWithInvalidModelType() throws Exception {
    final String measureAsJson =
        "{ \"measureName\":\"TestName\", \"cqlLibraryName\":\"TEST1\", \"model\":\"Test\", \"versionId\":\"versionId\" }";
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.model")
                .value("MADiE was unable to complete your request, please try again."));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureFailedWithoutSecurityToken() throws Exception {
    final String measureAsJson =
        "{\"measureName\": \"%s\", \"cqlLibraryName\": \"%s\", \"model\": \"%s\", \"measureScoring\": \"%s\" }"
            .formatted(
                "testMeasureName",
                "testLibraryName",
                ModelType.QI_CORE.toString(),
                MeasureScoring.PROPORTION.toString());

    MvcResult result =
        mockMvc
            .perform(
                post("/measure")
                    .content(measureAsJson)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andReturn();
    String resultStr = result.getResponse().getErrorMessage();
    assertEquals("Forbidden", resultStr);
  }

  @Test
  public void testGetMeasuresNoQueryParams() throws Exception {
    Measure m1 =
        Measure.builder()
            .active(true)
            .measureName("Measure1")
            .cqlLibraryName("TestLib1")
            .createdBy("test-okta-user-id-123")
            .model(MODEL)
            .build();
    Measure m2 =
        Measure.builder()
            .active(true)
            .measureName("Measure2")
            .cqlLibraryName("TestLib2")
            .createdBy("test-okta-user-id-123")
            .model(MODEL)
            .build();
    Measure m3 =
        Measure.builder()
            .active(true)
            .measureName("Measure3")
            .cqlLibraryName("TestLib3")
            .createdBy("test-okta-user-id-999")
            .model(MODEL)
            .build();

    Page<Measure> allMeasures = new PageImpl<>(List.of(m1, m2, m3));
    when(measureRepository.findAllByActive(any(Boolean.class), any(Pageable.class)))
        .thenReturn(allMeasures);

    MvcResult result =
        mockMvc
            .perform(get("/measures").with(user(TEST_USER_ID)).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    String resultStr = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    String expectedJsonStr = mapper.writeValueAsString(allMeasures);

    assertThat(resultStr, is(equalTo(expectedJsonStr)));
    verify(measureRepository, times(1)).findAllByActive(any(Boolean.class), any(Pageable.class));
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testGetMeasuresWithCurrentUserFalse() throws Exception {
    Measure m1 =
        Measure.builder()
            .active(true)
            .measureName("Measure1")
            .cqlLibraryName("TestLib1")
            .createdBy("test-okta-user-id-123")
            .model(MODEL)
            .build();
    Measure m2 =
        Measure.builder()
            .active(true)
            .measureName("Measure2")
            .cqlLibraryName("TestLib2")
            .createdBy("test-okta-user-id-123")
            .model(MODEL)
            .build();
    Measure m3 =
        Measure.builder()
            .active(true)
            .measureName("Measure3")
            .cqlLibraryName("TestLib3")
            .createdBy("test-okta-user-id-999")
            .model(MODEL)
            .build();

    Page<Measure> allMeasures = new PageImpl<>(List.of(m1, m2, m3));
    when(measureRepository.findAllByActive(any(Boolean.class), any(Pageable.class)))
        .thenReturn(allMeasures);

    MvcResult result =
        mockMvc
            .perform(
                get("/measures")
                    .with(user(TEST_USER_ID))
                    .queryParam("currentUser", "false")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    String resultStr = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    String expectedJsonStr = mapper.writeValueAsString(allMeasures);

    assertThat(resultStr, is(equalTo(expectedJsonStr)));
    verify(measureRepository, times(1)).findAllByActive(any(Boolean.class), any(Pageable.class));
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void getMeasuresWithCustomPaging() throws Exception {
    Measure m1 =
        Measure.builder()
            .active(true)
            .measureName("Measure1")
            .cqlLibraryName("TestLib1")
            .createdBy("test-okta-user-id-123")
            .model(MODEL)
            .build();
    Measure m2 =
        Measure.builder()
            .active(true)
            .measureName("Measure2")
            .cqlLibraryName("TestLib2")
            .createdBy("test-okta-user-id-123")
            .model(MODEL)
            .build();
    Measure m3 =
        Measure.builder()
            .active(true)
            .measureName("Measure3")
            .cqlLibraryName("TestLib3")
            .createdBy("test-okta-user-id-999")
            .model(MODEL)
            .build();

    Page<Measure> allMeasures = new PageImpl<>(List.of(m1, m2, m3));
    when(measureRepository.findAllByActive(any(Boolean.class), any(Pageable.class)))
        .thenReturn(allMeasures);

    MvcResult result =
        mockMvc
            .perform(
                get("/measures")
                    .with(user(TEST_USER_ID))
                    .queryParam("currentUser", "false")
                    .queryParam("limit", "25")
                    .queryParam("page", "3")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    String resultStr = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    String expectedJsonStr = mapper.writeValueAsString(allMeasures);

    assertThat(resultStr, is(equalTo(expectedJsonStr)));
    verify(measureRepository, times(1))
        .findAllByActive(activeCaptor.capture(), pageRequestCaptor.capture());
    PageRequest pageRequestValue = pageRequestCaptor.getValue();
    assertEquals(25, pageRequestValue.getPageSize());
    assertEquals(3, pageRequestValue.getPageNumber());
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testGetMeasuresFilterByCurrentUser() throws Exception {
    Measure m1 =
        Measure.builder()
            .active(true)
            .measureName("Measure1")
            .cqlLibraryName("TestLib1")
            .createdBy("test-okta-user-id-123")
            .model(MODEL)
            .build();
    Measure m2 =
        Measure.builder()
            .active(true)
            .measureName("Measure2")
            .cqlLibraryName("TestLib2")
            .createdBy("test-okta-user-id-123")
            .model(MODEL)
            .active(true)
            .build();

    final Page<Measure> measures = new PageImpl<>(List.of(m1, m2));

    when(measureRepository.findAllByCreatedByAndActive(
            anyString(), any(Boolean.class), any(PageRequest.class)))
        .thenReturn(measures); // fix

    MvcResult result =
        mockMvc
            .perform(
                get("/measures")
                    .with(user(TEST_USER_ID))
                    .queryParam("currentUser", "true")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    String resultStr = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    String expectedJsonStr = mapper.writeValueAsString(measures);

    assertThat(resultStr, is(equalTo(expectedJsonStr)));
    verify(measureRepository, times(1))
        .findAllByCreatedByAndActive(eq(TEST_USER_ID), any(Boolean.class), any(PageRequest.class));
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testCreateGroup() throws Exception {
    Group group =
        Group.builder()
            .scoring("Cohort")
            .id("test-id")
            .populations(
                List.of(
                    new Population(
                        "id-1", PopulationType.INITIAL_POPULATION, "Initial Population", null)))
            .measureGroupTypes(List.of(MeasureGroupTypes.PROCESS))
            .build();
    final String groupJson =
        "{\"scoring\":\"Cohort\",\"populations\":[{\"id\":\"id-1\",\"name\":\"initialPopulation\",\"definition\":\"Initial Population\"}],\"measureGroupTypes\":[\"Process\"],\"populationBasis\": \"Boolean\"}";
    when(groupService.createOrUpdateGroup(any(Group.class), any(String.class), any(String.class)))
        .thenReturn(group);

    mockMvc
        .perform(
            post("/measures/1234/groups")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(groupJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isCreated());

    verify(groupService, times(1))
        .createOrUpdateGroup(
            groupCaptor.capture(), measureIdCaptor.capture(), usernameCaptor.capture());

    Group persistedGroup = groupCaptor.getValue();
    assertEquals(group.getScoring(), persistedGroup.getScoring());
    assertEquals("Initial Population", persistedGroup.getPopulations().get(0).getDefinition());
    assertEquals(
        PopulationType.INITIAL_POPULATION, persistedGroup.getPopulations().get(0).getName());
    assertEquals(group.getMeasureGroupTypes().get(0), persistedGroup.getMeasureGroupTypes().get(0));
  }

  @Test
  public void testUpdateGroup() throws Exception {
    String updateIppDefinition = "FactorialOfFive";
    Group group =
        Group.builder()
            .scoring("Cohort")
            .id("test-id")
            .populations(
                List.of(
                    new Population(
                        "id-1", PopulationType.INITIAL_POPULATION, updateIppDefinition, null)))
            .measureGroupTypes(List.of(MeasureGroupTypes.PROCESS))
            .build();

    final String groupJson =
        "{\"id\":\"test-id\",\"scoring\":\"Cohort\",\"populations\":[{\"id\":\"id-2\",\"name\":\"initialPopulation\",\"definition\":\"FactorialOfFive\"}],\"measureGroupTypes\":[\"Process\"], \"populationBasis\": \"Boolean\"}";
    when(groupService.createOrUpdateGroup(any(Group.class), any(String.class), any(String.class)))
        .thenReturn(group);

    mockMvc
        .perform(
            put("/measures/1234/groups")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(groupJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk());

    verify(groupService, times(1))
        .createOrUpdateGroup(
            groupCaptor.capture(), measureIdCaptor.capture(), usernameCaptor.capture());

    Group persistedGroup = groupCaptor.getValue();
    assertEquals(group.getScoring(), persistedGroup.getScoring());
    assertEquals(updateIppDefinition, persistedGroup.getPopulations().get(0).getDefinition());
    assertEquals(
        PopulationType.INITIAL_POPULATION, persistedGroup.getPopulations().get(0).getName());
    assertEquals(group.getMeasureGroupTypes().get(0), persistedGroup.getMeasureGroupTypes().get(0));
  }

  @Test
  public void testUpdateGroupIfPopulationDefinitionReturnTypesAreInvalid() throws Exception {
    final String groupJson =
        "{\"id\":\"test-id\",\"scoring\":\"Cohort\",\"populations\":[{\"id\":\"id-2\",\"name\":\"initialPopulation\",\"definition\":\"FactorialOfFive\"}],\"measureGroupTypes\":[\"Process\"], \"populationBasis\": \"Boolean\"}";
    when(groupService.createOrUpdateGroup(any(Group.class), any(String.class), any(String.class)))
        .thenThrow(new InvalidReturnTypeException("Initial Population"));

    mockMvc
        .perform(
            put("/measures/1234/groups")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(groupJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Return type for the CQL definition selected for the Initial Population does not match with population basis."));

    verify(groupService, times(1))
        .createOrUpdateGroup(
            groupCaptor.capture(), measureIdCaptor.capture(), usernameCaptor.capture());
  }

  @Test
  void getMeasureGroupsReturnsNotFound() throws Exception {
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());
    mockMvc
        .perform(
            get("/measures/1234/groups")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
    verify(measureRepository, times(1)).findById(eq("1234"));
    verifyNoInteractions(measureService);
  }

  @Test
  void testGetMeasureBundleReturnsEmptyArray() throws Exception {
    Measure measure = new Measure();
    measure.setCreatedBy(TEST_USER_ID);
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    mockMvc
        .perform(
            get("/measures/1234/groups")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string("[]"));
    verify(measureRepository, times(1)).findById(eq("1234"));
    verifyNoInteractions(measureService);
  }

  @Test
  void testGetMeasureBundleReturnsGroupsArray() throws Exception {
    Measure measure =
        Measure.builder()
            .createdBy(TEST_USER_ID)
            .groups(
                List.of(
                    Group.builder()
                        .groupDescription("Group1")
                        .scoring(MeasureScoring.RATIO.toString())
                        .build()))
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    mockMvc
        .perform(
            get("/measures/1234/groups")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].groupDescription").value("Group1"))
        .andExpect(jsonPath("$[0].scoring").value("Ratio"));
    verify(measureRepository, times(1)).findById(eq("1234"));
    verifyNoInteractions(measureService);
  }

  @Test
  void getMeasureBundleReturnsNotFound() throws Exception {
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());
    mockMvc
        .perform(
            get("/measures/1234/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
    verify(measureRepository, times(1)).findById(eq("1234"));
    verifyNoInteractions(measureService);
  }

  @Test
  void testGetMeasureBundleReturnsForbidden() throws Exception {
    Measure measure = Measure.builder().measureName("TestMeasure").createdBy("OTHER_USER").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    mockMvc
        .perform(
            get("/measures/1234/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.message")
                .value("User " + TEST_USER_ID + " is not authorized for Measure with ID 1234"));
    verify(measureRepository, times(1)).findById(eq("1234"));
    verifyNoInteractions(measureService);
  }

  @Test
  void testGetMeasureBundleReturnsConflict() throws Exception {
    Measure measure =
        Measure.builder()
            .measureName("TestMeasure")
            .createdBy(TEST_USER_ID)
            .cqlErrors(true)
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    mockMvc
        .perform(
            get("/measures/1234/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Response could not be completed for Measure with ID 1234, since CQL errors exist."));
    verify(measureRepository, times(1)).findById(eq("1234"));
    verifyNoInteractions(measureService);
  }

  @Test
  void testGetMeasureBundleReturnsConflictWhenNoGroupsPresent() throws Exception {
    Measure measure =
        Measure.builder()
            .measureName("TestMeasure")
            .createdBy(TEST_USER_ID)
            .cqlErrors(false)
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    mockMvc
        .perform(
            get("/measures/1234/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Response could not be completed for Measure with ID 1234, since there are no associated measure groups."));
    verify(measureRepository, times(1)).findById(eq("1234"));
    verifyNoInteractions(measureService);
  }

  @Test
  void testGetMeasureBundleReturnsConflictWhenNoElmJsonPresent() throws Exception {
    Measure measure =
        Measure.builder()
            .measureName("TestMeasure")
            .createdBy(TEST_USER_ID)
            .cqlErrors(false)
            .groups(
                List.of(
                    Group.builder()
                        .groupDescription("Group1")
                        .scoring(MeasureScoring.RATIO.toString())
                        .build()))
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    mockMvc
        .perform(
            get("/measures/1234/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Response could not be completed for Measure with ID 1234, since there are issues with the CQL."));
    verify(measureRepository, times(1)).findById(eq("1234"));
    verifyNoInteractions(measureService);
  }

  @Test
  void testGetMeasureBundleReturnsConflictWhenElmJsonContainsErrors() throws Exception {
    Measure measure =
        Measure.builder()
            .measureName("TestMeasure")
            .createdBy(TEST_USER_ID)
            .cqlErrors(false)
            .groups(
                List.of(
                    Group.builder()
                        .groupDescription("Group1")
                        .scoring(MeasureScoring.RATIO.toString())
                        .build()))
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureService.bundleMeasure(any(Measure.class), anyString()))
        .thenThrow(new CqlElmTranslationErrorException(measure.getMeasureName()));
    mockMvc
        .perform(
            get("/measures/1234/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Response could not be completed for Measure with ID 1234, since there are issues with the CQL."));
    verify(measureRepository, times(1)).findById(eq("1234"));
    verifyNoInteractions(measureService);
  }

  @Test
  void testGetMeasureBundleReturnsServerExceptionForCqlElmTranslationFailure() throws Exception {
    final String elmJson = "{\"text\": \"ELM JSON\"}";
    Measure measure =
        Measure.builder()
            .measureName("EXM124")
            .createdBy(TEST_USER_ID)
            .cqlErrors(false)
            .groups(
                List.of(
                    Group.builder()
                        .groupDescription("Group1")
                        .scoring(MeasureScoring.RATIO.toString())
                        .build()))
            .elmJson(elmJson)
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureService.bundleMeasure(any(Measure.class), anyString()))
        .thenThrow(
            new CqlElmTranslationServiceException(
                measure.getMeasureName(), new RuntimeException("CAUSE")));
    mockMvc
        .perform(
            get("/measures/1234/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
    verify(measureRepository, times(1)).findById(anyString());
  }

  @Test
  void testGetMeasureBundleReturnsMeasureBundle() throws Exception {
    final String elmJson = "{\"text\": \"ELM JSON\"}";
    final String bundleString =
        "{\n"
            + "    \"resourceType\": \"Bundle\",\n"
            + "    \"type\": \"transaction\",\n"
            + "    \"entry\": [\n"
            + "        {\n"
            + "            \"resource\": {\n"
            + "                \"resourceType\": \"Measure\",\n"
            + "                \"meta\": {\n"
            + "                    \"profile\": [\n"
            + "                        \"http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/proportion-measure-cqfm\"\n"
            + "                    ]\n"
            + "                },\n"
            + "                \"url\": \"http://hl7.org/fhir/us/cqfmeasures/Measure/EXM124\",\n"
            + "                \"version\": \"9.0.000\",\n"
            + "                \"name\": \"EXM124\",\n"
            + "                \"title\": \"Cervical Cancer Screening\",\n"
            + "                \"experimental\": true,\n"
            + "                \"effectivePeriod\": {\n"
            + "                    \"start\": \"2022-05-09\",\n"
            + "                    \"end\": \"2022-05-09\"\n"
            + "                }\n"
            + "            }\n"
            + "        }\n"
            + "    ]\n"
            + "}";
    Measure measure =
        Measure.builder()
            .measureName("EXM124")
            .createdBy(TEST_USER_ID)
            .cqlErrors(false)
            .groups(
                List.of(
                    Group.builder()
                        .groupDescription("Group1")
                        .scoring(MeasureScoring.RATIO.toString())
                        .build()))
            .elmJson(elmJson)
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureService.bundleMeasure(any(Measure.class), anyString())).thenReturn(bundleString);
    mockMvc
        .perform(
            get("/measures/1234/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resourceType").value("Bundle"))
        .andExpect(jsonPath("$.type").value("transaction"))
        .andExpect(jsonPath("$.entry[0].resource.resourceType").value("Measure"))
        .andExpect(jsonPath("$.entry[0].resource.name").value("EXM124"))
        .andExpect(jsonPath("$.entry[0].resource.version").value("9.0.000"));
    verify(measureRepository, times(1)).findById(eq("1234"));
  }
}
