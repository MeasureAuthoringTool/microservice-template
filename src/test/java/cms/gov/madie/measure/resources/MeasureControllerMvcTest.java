package cms.gov.madie.measure.resources;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.models.MeasurePopulation;
import cms.gov.madie.measure.models.MeasureScoring;
import cms.gov.madie.measure.models.ModelType;

import cms.gov.madie.measure.services.MeasureService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.repositories.MeasureRepository;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest({MeasureController.class})
public class MeasureControllerMvcTest {

  @MockBean private MeasureRepository measureRepository;
  @MockBean private MeasureService measureService;

  @Autowired private MockMvc mockMvc;
  @Captor private ArgumentCaptor<Measure> measureArgumentCaptor;
  private static final String TEST_USER_ID = "test-okta-user-id-123";
  @Captor ArgumentCaptor<Group> groupCaptor;
  @Captor ArgumentCaptor<String> measureIdCaptor;
  @Captor ArgumentCaptor<String> usernameCaptor;
  @Captor ArgumentCaptor<PageRequest> pageRequestCaptor;
  @Captor ArgumentCaptor<Boolean> activeCaptor;

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
    String model = "QI-Core";
    String scoring = MeasureScoring.COHORT.toString();

    Measure priorMeasure = new Measure();
    priorMeasure.setId(measureId);
    priorMeasure.setMeasureName(measureName);
    priorMeasure.setCqlLibraryName(libName);
    priorMeasure.setModel(model);
    priorMeasure.setMeasureScoring(scoring);

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(priorMeasure));
    when(measureRepository.save(any(Measure.class))).thenReturn(mock(Measure.class));

    final String measureAsJson =
        "{\"id\": \"%s\", \"measureName\": \"%s\", \"cqlLibraryName\":\"%s\", \"measureMetaData\": { \"steward\" : \"%s\", \"description\" : \"%s\", \"copyright\" : \"%s\", \"disclaimer\" : \"%s\", \"rationale\" : \"%s\", \"author\" : \"%s\", \"guidance\" : \"%s\"}, \"model\":\"%s\", \"measureScoring\":\"%s\" }"
            .formatted(
                measureId,
                measureName,
                libName,
                steward,
                description,
                copyright,
                disclaimer,
                rationale,
                author,
                guidance,
                model,
                scoring);
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
    assertEquals(model, savedMeasure.getModel());
    assertNotNull(savedMeasure.getLastModifiedAt());
    assertEquals(TEST_USER_ID, savedMeasure.getLastModifiedBy());
    int lastModCompareTo =
        savedMeasure.getLastModifiedAt().compareTo(Instant.now().minus(60, ChronoUnit.SECONDS));
    assertEquals(1, lastModCompareTo);
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
    final String measureAsJson = "{ \"measureName\":\"A_Name\", \"cqlLibraryName\":\"ALib\" }";
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
        "{ \"id\": \"m1234\", \"measureName\":\"A_Name\", \"cqlLibraryName\":\"ALib\" }";
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
        "{ \"measureName\":\"%s\", \"cqlLibraryName\":\"ALib\"  }".formatted(measureName);
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
        "{ \"id\": \"m1234\", \"measureName\":\"%s\", \"cqlLibraryName\":\"ALib\" }"
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
  public void testNewMeasurePassed() throws Exception {
    Measure saved = new Measure();
    String measureId = "id123";
    saved.setId(measureId);
    String measureName = "SavedMeasure";
    String libraryName = "Lib1";
    String model = "QI-Core";
    String scoring = MeasureScoring.PROPORTION.toString();
    saved.setMeasureName(measureName);
    saved.setCqlLibraryName(libraryName);
    saved.setModel(model);
    saved.setMeasureScoring(scoring);
    when(measureRepository.save(any(Measure.class))).thenReturn(saved);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(any(String.class));

    final String measureAsJson =
        "{\"measureName\": \"%s\", \"cqlLibraryName\": \"%s\", \"model\": \"%s\", \"measureScoring\": \"%s\" }"
            .formatted(measureName, libraryName, model, scoring);

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
        .andExpect(jsonPath("$.model").value(model))
        .andExpect(jsonPath("$.id").value(measureId))
        .andExpect(jsonPath("$.measureScoring").value(scoring));

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository);
    Measure savedMeasure = measureArgumentCaptor.getValue();
    assertEquals(measureName, savedMeasure.getMeasureName());
    assertEquals(libraryName, savedMeasure.getCqlLibraryName());
    assertEquals(model, savedMeasure.getModel());
    assertEquals(scoring, savedMeasure.getMeasureScoring());
    assertEquals(TEST_USER_ID, savedMeasure.getCreatedBy());
    assertEquals(TEST_USER_ID, savedMeasure.getLastModifiedBy());
    assertNotNull(savedMeasure.getCreatedAt());
    assertNotNull(savedMeasure.getLastModifiedAt());
  }

  @Test
  public void testNewMeasureFailsIfDuplicatedLibraryName() throws Exception {
    Measure existing = new Measure();
    String measureId = "id123";
    existing.setId(measureId);
    existing.setMeasureName("ExistingMeasure");
    String cqlLibraryName = "ExistingLibrary";
    existing.setCqlLibraryName(cqlLibraryName);
    String model = "QI-Core";
    existing.setModel(model);
    String scoring = MeasureScoring.PROPORTION.toString();
    existing.setMeasureScoring(scoring);

    doThrow(
            new DuplicateKeyException(
                "cqlLibraryName", "CQL library with given name already exists."))
        .when(measureService)
        .checkDuplicateCqlLibraryName(any(String.class));

    final String newMeasureAsJson =
        "{\"measureName\": \"NewMeasure\", \"cqlLibraryName\": \"%s\",\"model\":\"%s\",\"measureScoring\":\"%s\"}"
            .formatted(cqlLibraryName, model, scoring);
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
    priorMeasure.setModel("QI-Core");
    priorMeasure.setMeasureScoring(MeasureScoring.RATIO.toString());
    when(measureRepository.findById(eq(priorMeasure.getId())))
        .thenReturn(Optional.of(priorMeasure));

    Measure existingMeasure = new Measure();
    existingMeasure.setId("id1");
    existingMeasure.setMeasureName("ExistingMeasure");
    existingMeasure.setCqlLibraryName("ExistingMeasureLibrary");
    doThrow(
            new DuplicateKeyException(
                "cqlLibraryName", "CQL library with given name already exists."))
        .when(measureService)
        .checkDuplicateCqlLibraryName(any(String.class));

    final String updatedMeasureAsJson =
        "{\"id\": \"%s\",\"measureName\": \"%s\", \"cqlLibraryName\": \"%s\", \"model\":\"%s\", \"measureScoring\":\"%s\"}"
            .formatted(
                priorMeasure.getId(),
                priorMeasure.getMeasureName(),
                existingMeasure.getCqlLibraryName(),
                priorMeasure.getModel(),
                priorMeasure.getMeasureScoring());
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
  public void testNewMeasureNoUnderscore() throws Exception {
    final String measureAsJson =
        "{ \"id\": \"m1234\", \"measureName\":\"A_Name\", \"cqlLibraryName\":\"ALib\" }";
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
    final String measureAsJson = "{ \"measureName\":\"AName\", \"cqlLibraryName\":\"aLib\" }";
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
        "{ \"id\": \"m1234\", \"measureName\":\"AName\", \"cqlLibraryName\":\"aLib\" }";
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
    final String measureAsJson = "{ \"measureName\":\"AName\", \"cqlLibraryName\":\"ALi''b\" }";
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
    final String measureAsJson = "{ \"measureName\":\"AName\", \"cqlLibraryName\":\"ALi_'b\" }";
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
    saved.setMeasureName(measureName);
    saved.setCqlLibraryName(libraryName);
    String model = "QI-Core";
    saved.setModel(model);
    String scoring = MeasureScoring.CONTINUOUS_VARIABLE.toString();
    saved.setMeasureScoring(scoring);

    when(measureRepository.save(any(Measure.class))).thenReturn(saved);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(any(String.class));

    final String measureAsJson =
        "{ \"measureName\":\"%s\", \"cqlLibraryName\":\"%s\", \"model\":\"%s\", \"measureScoring\":\"%s\" }"
            .formatted(measureName, libraryName, model, scoring);
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
        .andExpect(jsonPath("$.model").value(model))
        .andExpect(jsonPath("$.id").value(measureId));

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
    saved.setMeasureName(measureName);
    saved.setCqlLibraryName(libraryName);
    String model = "QI-Core";
    saved.setModel(model);
    String scoring = MeasureScoring.CONTINUOUS_VARIABLE.toString();
    saved.setMeasureScoring(scoring);

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(saved));
    when(measureRepository.save(any(Measure.class))).thenReturn(saved);

    final String measureAsJson =
        "{ \"id\": \"%s\", \"measureName\":\"%s\", \"cqlLibraryName\":\"%s\", \"model\":\"%s\", \"measureScoring\":\"%s\"}"
            .formatted(measureId, measureName, libraryName, model, scoring);
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
    String model = "QI-Core";
    saved.setModel(model);
    String scoring = MeasureScoring.CONTINUOUS_VARIABLE.toString();
    saved.setMeasureScoring(scoring);

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(saved));
    when(measureRepository.save(any(Measure.class))).thenReturn(saved);

    final String measureAsJson =
        "{ \"id\": \"id1234\", \"measureName\":\"%s\", \"cqlLibraryName\":\"%s\", \"model\":\"%s\", \"measureScoring\":\"%s\"}"
            .formatted(measureName, libraryName, model, scoring);
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
    String model = "QI-Core";
    saved.setModel(model);
    String scoring = MeasureScoring.CONTINUOUS_VARIABLE.toString();
    saved.setMeasureScoring(scoring);

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(saved));
    when(measureRepository.save(any(Measure.class))).thenReturn(saved);

    final String measureAsJson =
        "{ \"id\": null, \"measureName\":\"%s\", \"cqlLibraryName\":\"%s\", \"model\":\"%s\", \"measureScoring\":\"%s\"}"
            .formatted(measureName, libraryName, model, scoring);
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
        "{ \"measureName\":\"TestName\", \"cqlLibraryName\":\"TEST1\", \"model\":\"Test\" }";
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
  public void testNewMeasureFailsWithInvalidMeasureScoringType() throws Exception {
    final String measureAsJson =
        "{ \"measureName\":\"TestName\", \"cqlLibraryName\":\"TEST1\", \"model\":\"QI-Core\", \"measureScoring\":\"Test\" }";
    mockMvc
        .perform(
            post("/measure")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(measureAsJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.measureScoring")
                .value("Value provided is not a valid option."));
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
            .measureScoring("Proportion")
            .model("QI-Core")
            .build();
    Measure m2 =
        Measure.builder()
            .active(true)
            .measureName("Measure2")
            .cqlLibraryName("TestLib2")
            .createdBy("test-okta-user-id-123")
            .measureScoring("Proportion")
            .model("QI-Core")
            .build();
    Measure m3 =
        Measure.builder()
            .active(true)
            .measureName("Measure3")
            .cqlLibraryName("TestLib3")
            .createdBy("test-okta-user-id-999")
            .measureScoring("Proportion")
            .model("QI-Core")
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
            .measureScoring("Proportion")
            .model("QI-Core")
            .build();
    Measure m2 =
        Measure.builder()
            .active(true)
            .measureName("Measure2")
            .cqlLibraryName("TestLib2")
            .createdBy("test-okta-user-id-123")
            .measureScoring("Proportion")
            .model("QI-Core")
            .build();
    Measure m3 =
        Measure.builder()
            .active(true)
            .measureName("Measure3")
            .cqlLibraryName("TestLib3")
            .createdBy("test-okta-user-id-999")
            .measureScoring("Proportion")
            .model("QI-Core")
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
            .measureScoring("Proportion")
            .model("QI-Core")
            .build();
    Measure m2 =
        Measure.builder()
            .active(true)
            .measureName("Measure2")
            .cqlLibraryName("TestLib2")
            .createdBy("test-okta-user-id-123")
            .measureScoring("Proportion")
            .model("QI-Core")
            .build();
    Measure m3 =
        Measure.builder()
            .active(true)
            .measureName("Measure3")
            .cqlLibraryName("TestLib3")
            .createdBy("test-okta-user-id-999")
            .measureScoring("Proportion")
            .model("QI-Core")
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
            .measureScoring("Proportion")
            .model("QI-Core")
            .build();
    Measure m2 =
        Measure.builder()
            .active(true)
            .measureName("Measure2")
            .cqlLibraryName("TestLib2")
            .createdBy("test-okta-user-id-123")
            .measureScoring("Proportion")
            .model("QI-Core")
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
            .population(Map.of(MeasurePopulation.INITIAL_POPULATION, "Initial Population"))
            .build();

    final String groupJson =
        "{\"scoring\":\"Cohort\",\"population\":{\"initialPopulation\":\"Initial Population\"}}";
    when(measureService.createOrUpdateGroup(any(Group.class), any(String.class), any(String.class)))
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

    verify(measureService, times(1))
        .createOrUpdateGroup(
            groupCaptor.capture(), measureIdCaptor.capture(), usernameCaptor.capture());

    Group persistedGroup = groupCaptor.getValue();
    assertEquals(group.getScoring(), persistedGroup.getScoring());
    assertEquals(
        "Initial Population",
        persistedGroup.getPopulation().get(MeasurePopulation.INITIAL_POPULATION));
  }

  @Test
  public void testUpdateGroup() throws Exception {
    String updateIppDefinition = "FactorialOfFive";
    Group group =
        Group.builder()
            .scoring("Cohort")
            .id("test-id")
            .population(Map.of(MeasurePopulation.INITIAL_POPULATION, updateIppDefinition))
            .build();

    final String groupJson =
        "{\"id\":\"test-id\",\"scoring\":\"Cohort\",\"population\":{\"initialPopulation\":\"FactorialOfFive\"}}";
    when(measureService.createOrUpdateGroup(any(Group.class), any(String.class), any(String.class)))
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

    verify(measureService, times(1))
        .createOrUpdateGroup(
            groupCaptor.capture(), measureIdCaptor.capture(), usernameCaptor.capture());

    Group persistedGroup = groupCaptor.getValue();
    assertEquals(group.getScoring(), persistedGroup.getScoring());
    assertEquals(
        updateIppDefinition,
        persistedGroup.getPopulation().get(MeasurePopulation.INITIAL_POPULATION));
  }
}
