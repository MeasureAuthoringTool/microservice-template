package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BadVersionRequestException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.MeasureNotDraftableException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.ExportRepository;
import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.*;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static cms.gov.madie.measure.services.VersionService.VersionValidationResult.TEST_CASE_ERROR;
import static cms.gov.madie.measure.services.VersionService.VersionValidationResult.VALID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VersionServiceTest {

  @Mock private MeasureRepository measureRepository;

  @Mock ActionLogService actionLogService;

  @Mock ElmTranslatorClient elmTranslatorClient;

  @Mock FhirServicesClient fhirServicesClient;

  @Mock ExportRepository exportRepository;

  @Mock MeasureService measureService;

  @InjectMocks VersionService versionService;

  @Captor private ArgumentCaptor<Measure> measureCaptor;

  @Captor private ArgumentCaptor<Export> exportArgumentCaptor;

  private final String ELMJON_ERROR =
      "{\n" + "\"errorExceptions\" : \n" + "[ {\"error\":\"error translating cql\" } ]\n" + "}";
  private final String ELMJON_NO_ERROR = "{\n" + "\"errorExceptions\" : \n" + "[]\n" + "}";

  TestCaseGroupPopulation testCaseGroupPopulation =
      TestCaseGroupPopulation.builder()
          .groupId("groupId1")
          .scoring("Cohort")
          .populationBasis("boolean")
          .build();

  TestCase testCase =
      TestCase.builder()
          .id("testId1")
          .name("IPPPass")
          .series("BloodPressure>124")
          .createdBy("TestUser")
          .lastModifiedBy("TestUser2")
          .json("{\"resourceType\":\"Patient\"}")
          .title("Test1")
          .groupPopulations(List.of(testCaseGroupPopulation))
          .build();
  Group cvGroup =
      Group.builder()
          .id("xyz-p12r-12ert")
          .populationBasis("Encounter")
          .scoring("Continuous Variable")
          .populations(
              List.of(
                  new Population(
                      "id-1", PopulationType.INITIAL_POPULATION, "FactorialOfFive", null, null),
                  new Population(
                      "id-2", PopulationType.MEASURE_POPULATION, "Measure Population", null, null)))
          .measureObservations(
              List.of(
                  new MeasureObservation(
                      "id-1",
                      "fun",
                      "a description of fun",
                      "id-2",
                      AggregateMethodType.MAXIMUM.getValue())))
          .stratifications(List.of())
          .groupDescription("Description")
          .scoringUnit("test-scoring-unit")
          .build();

  private final Instant today = Instant.now();

  @Test
  public void testCheckValidVersioningThrowsResourceNotFoundException() {
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            versionService.checkValidVersioning(
                "testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCreateVersionThrowsResourceNotFoundException() {
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> versionService.createVersion("testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCreateVersionThrowsBadVersionRequestExceptionForInvalidVersionType() {
    Measure existingMeasure = Measure.builder().id("testMeasureId").createdBy("testUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        BadVersionRequestException.class,
        () ->
            versionService.createVersion(
                "testMeasureId", "NOTVALIDVERSIONTYPE", "testUser", "accesstoken"));
  }

  @Test
  public void testCheckValidVersioningThrowsBadVersionRequestExceptionForInvalidVersionType() {
    Measure existingMeasure = Measure.builder().id("testMeasureId").createdBy("testUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        BadVersionRequestException.class,
        () ->
            versionService.checkValidVersioning(
                "testMeasureId", "NOTVALIDVERSIONTYPE", "testUser", "accesstoken"));
  }

  @Test
  public void testCheckValidVersioningThrowsUnauthorizedExceptionForNonOwner() {
    Measure existingMeasure =
        Measure.builder().id("testMeasureId").createdBy("anotherUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));
    doThrow(new UnauthorizedException("Measure", "testMeasureId", "testUser"))
        .when(measureService)
        .verifyAuthorization(anyString(), any(Measure.class));

    assertThrows(
        UnauthorizedException.class,
        () ->
            versionService.checkValidVersioning(
                "testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCreateVersionThrowsUnauthorizedExceptionForNonOwner() {
    Measure existingMeasure =
        Measure.builder().id("testMeasureId").createdBy("anotherUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));
    doThrow(new UnauthorizedException("Measure", "testMeasureId", "testUser"))
        .when(measureService)
        .verifyAuthorization(anyString(), any(Measure.class));

    assertThrows(
        UnauthorizedException.class,
        () -> versionService.createVersion("testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCreateVersionThrowsBadVersionRequestExceptionForNonDraftMeasure() {
    Measure existingMeasure = Measure.builder().id("testMeasureId").createdBy("testUser").build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(false);
    existingMeasure.setMeasureMetaData(metaData);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        BadVersionRequestException.class,
        () -> versionService.createVersion("testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCCheckValidVersioningThrowsBadVersionRequestExceptionForNonDraftMeasure() {
    Measure existingMeasure = Measure.builder().id("testMeasureId").createdBy("testUser").build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(false);
    existingMeasure.setMeasureMetaData(metaData);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        BadVersionRequestException.class,
        () ->
            versionService.checkValidVersioning(
                "testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCreateVersionThrowsBadVersionRequestExceptionForCqlErrors() {
    Measure existingMeasure =
        Measure.builder().id("testMeasureId").createdBy("testUser").cqlErrors(true).build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        BadVersionRequestException.class,
        () -> versionService.createVersion("testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCheckValidVersioningThrowsBadVersionRequestExceptionForCqlErrors() {
    Measure existingMeasure =
        Measure.builder().id("testMeasureId").createdBy("testUser").cqlErrors(true).build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        BadVersionRequestException.class,
        () ->
            versionService.checkValidVersioning(
                "testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCreateVersionThrowsBadVersionRequestExceptionForEmptyCQL() {
    Measure existingMeasure =
        Measure.builder()
            .id("testMeasureId")
            .createdBy("testUser")
            .cqlErrors(false)
            .cql("")
            .build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    assertThrows(
        BadVersionRequestException.class,
        () -> versionService.createVersion("testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCreateVersionThrowsCqlElmTranslationErrorExceptionForInvalidCQL() {
    Measure existingMeasure =
        Measure.builder()
            .id("testMeasureId")
            .measureName("test measure")
            .createdBy("testUser")
            .cqlErrors(false)
            .cql("test cql")
            .build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    ElmJson elmJson = ElmJson.builder().json(ELMJON_ERROR).build();
    when(elmTranslatorClient.getElmJson(anyString(), anyString())).thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(any())).thenReturn(true);

    assertThrows(
        CqlElmTranslationErrorException.class,
        () -> versionService.createVersion("testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCheckValidVersioningThrowsCqlElmTranslationErrorExceptionForInvalidCQL() {
    Measure existingMeasure =
        Measure.builder()
            .id("testMeasureId")
            .measureName("test measure")
            .createdBy("testUser")
            .cqlErrors(false)
            .cql("test cql")
            .build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    ElmJson elmJson = ElmJson.builder().json(ELMJON_ERROR).build();
    when(elmTranslatorClient.getElmJson(anyString(), anyString())).thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(any())).thenReturn(true);

    assertThrows(
        CqlElmTranslationErrorException.class,
        () ->
            versionService.checkValidVersioning(
                "testMeasureId", "MAJOR", "testUser", "accesstoken"));
  }

  @Test
  public void testCheckVersionIdentifiesTestCaseErrors() {
    Measure existingMeasure =
        Measure.builder()
            .id("testMeasureId")
            .createdBy("testUser")
            .cql("library Test1CQLLib version '2.3.001")
            .build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);
    List<TestCase> testCases = List.of(TestCase.builder().validResource(false).build());
    existingMeasure.setTestCases(testCases);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    ElmJson elmJson = ElmJson.builder().json(ELMJON_NO_ERROR).build();
    when(elmTranslatorClient.getElmJson(anyString(), anyString())).thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(any())).thenReturn(false);
    var validationResult =
        versionService.checkValidVersioning("testMeasureId", "MAJOR", "testUser", "accesstoken");
    assertEquals(TEST_CASE_ERROR, validationResult);
  }

  @Test
  public void testCheckVersion() {
    Measure existingMeasure =
        Measure.builder()
            .id("testMeasureId")
            .createdBy("testUser")
            .cql("library Test1CQLLib version '2.3.001")
            .build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    ElmJson elmJson = ElmJson.builder().json(ELMJON_NO_ERROR).build();
    when(elmTranslatorClient.getElmJson(anyString(), anyString())).thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(any())).thenReturn(false);
    var validationResult =
        versionService.checkValidVersioning("testMeasureId", "MAJOR", "testUser", "accesstoken");
    assertEquals(VALID, validationResult);
  }

  @Test
  public void testGetNextVersionOtherException() throws Exception {
    Measure existingMeasure =
        Measure.builder()
            .id("testMeasureId")
            .measureSetId("testMeasureSetId")
            .createdBy("testUser")
            .cql("library Test1CQLLib version '2.3.001'")
            .build();
    Version version = versionService.getNextVersion(existingMeasure, "InvalidVersionType");
    assertEquals(version.getMajor(), 0);
    assertEquals(version.getMinor(), 0);
    assertEquals(version.getRevisionNumber(), 0);
  }

  @Test
  public void testCreateVersionMajorSuccess() throws Exception {
    FhirMeasure existingMeasure =
        FhirMeasure.builder()
            .id("testMeasureId")
            .measureSetId("testMeasureSetId")
            .createdBy("testUser")
            .cql("library Test1CQLLib version '2.3.001'")
            .build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);
    Version version = Version.builder().major(2).minor(3).revisionNumber(1).build();
    existingMeasure.setVersion(version);

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    ElmJson elmJson = ElmJson.builder().json(ELMJON_NO_ERROR).build();
    when(elmTranslatorClient.getElmJson(anyString(), anyString())).thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(any())).thenReturn(false);

    Version newVersion = Version.builder().major(2).minor(2).revisionNumber(2).build();
    when(measureRepository.findMaxVersionByMeasureSetId(anyString()))
        .thenReturn(Optional.of(newVersion));

    Measure updatedMeasure = existingMeasure.toBuilder().build();
    Version updatedVersion = Version.builder().major(3).minor(0).revisionNumber(0).build();
    updatedMeasure.setVersion(updatedVersion);
    MeasureMetaData updatedMetaData = new MeasureMetaData();
    updatedMetaData.setDraft(false);
    updatedMeasure.setMeasureMetaData(updatedMetaData);
    when(measureRepository.save(any(Measure.class))).thenReturn(updatedMeasure);

    Export measureExport =
        Export.builder()
            .id("testId")
            .measureId("testMeasureId")
            .measureBundleJson("test measure json")
            .build();
    when(exportRepository.save(any(Export.class))).thenReturn(measureExport);
    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn("test measure json");

    versionService.createVersion("testMeasureId", "MAJOR", "testUser", "accesstoken");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    Measure savedValue = measureCaptor.getValue();
    assertEquals(savedValue.getVersion().getMajor(), 3);
    assertEquals(savedValue.getVersion().getMinor(), 0);
    assertEquals(savedValue.getVersion().getRevisionNumber(), 0);
    assertFalse(savedValue.getMeasureMetaData().isDraft());
    assertThat(
        savedValue
            .getReviewMetaData()
            .getApprovalDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(today.truncatedTo(ChronoUnit.MINUTES)),
        is(0));
    assertThat(
        savedValue
            .getReviewMetaData()
            .getLastReviewDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(today.truncatedTo(ChronoUnit.MINUTES)),
        is(0));

    verify(exportRepository, times(1)).save(exportArgumentCaptor.capture());
    Export capturedExport = exportArgumentCaptor.getValue();
    assertEquals(savedValue.getId(), capturedExport.getMeasureId());
    assertEquals(measureExport.getMeasureBundleJson(), capturedExport.getMeasureBundleJson());
  }

  @Test
  public void testCreateVersionMinorSuccess() throws Exception {
    QdmMeasure existingMeasure =
        QdmMeasure.builder()
            .id("testMeasureId")
            .measureSetId("testMeasureSetId")
            .createdBy("testUser")
            .cql("library Test1CQLLib version '2.3.001'")
            .build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);
    Version version = Version.builder().major(2).minor(3).revisionNumber(1).build();
    existingMeasure.setVersion(version);
    List<TestCase> testCases = List.of(TestCase.builder().validResource(true).build());
    existingMeasure.setTestCases(testCases);
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    ElmJson elmJson = ElmJson.builder().json(ELMJON_NO_ERROR).build();
    when(elmTranslatorClient.getElmJson(anyString(), anyString())).thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(any())).thenReturn(false);

    Version newVersion = Version.builder().major(2).minor(3).revisionNumber(2).build();
    when(measureRepository.findMaxMinorVersionByMeasureSetIdAndVersionMajor(anyString(), anyInt()))
        .thenReturn(Optional.of(newVersion));

    Measure updatedMeasure = existingMeasure.toBuilder().build();
    Version updatedVersion = Version.builder().major(2).minor(4).revisionNumber(0).build();
    updatedMeasure.setVersion(updatedVersion);
    MeasureMetaData updatedMetaData = new MeasureMetaData();
    updatedMetaData.setDraft(false);
    updatedMeasure.setMeasureMetaData(updatedMetaData);
    when(measureRepository.save(any(Measure.class))).thenReturn(updatedMeasure);

    versionService.createVersion("testMeasureId", "MINOR", "testUser", "accesstoken");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    Measure savedValue = measureCaptor.getValue();
    assertEquals(savedValue.getVersion().getMajor(), 2);
    assertEquals(savedValue.getVersion().getMinor(), 4);
    assertEquals(savedValue.getVersion().getRevisionNumber(), 0);
    assertFalse(savedValue.getMeasureMetaData().isDraft());
    assertThat(
        savedValue
            .getReviewMetaData()
            .getApprovalDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(today.truncatedTo(ChronoUnit.MINUTES)),
        is(0));
    assertThat(
        savedValue
            .getReviewMetaData()
            .getLastReviewDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(today.truncatedTo(ChronoUnit.MINUTES)),
        is(0));
  }

  @Test
  public void testCreateFhirVersionPatchSuccess() throws Exception {
    FhirMeasure existingMeasure =
        FhirMeasure.builder()
            .id("testMeasureId")
            .measureSetId("testMeasureSetId")
            .createdBy("testUser")
            .cql("library Test1CQLLib version '2.3.001'")
            .build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    existingMeasure.setMeasureMetaData(metaData);
    Version version = Version.builder().major(2).minor(3).revisionNumber(1).build();
    existingMeasure.setVersion(version);
    List<TestCase> testCases = List.of(TestCase.builder().validResource(true).build());
    existingMeasure.setTestCases(testCases);
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

    ElmJson elmJson = ElmJson.builder().json(ELMJON_NO_ERROR).build();
    when(elmTranslatorClient.getElmJson(anyString(), anyString())).thenReturn(elmJson);
    when(elmTranslatorClient.hasErrors(any())).thenReturn(false);

    Version newVersion = Version.builder().major(2).minor(3).revisionNumber(1).build();
    when(measureRepository.findMaxRevisionNumberByMeasureSetIdAndVersionMajorAndMinor(
            anyString(), anyInt(), anyInt()))
        .thenReturn(Optional.of(newVersion));

    Measure updatedMeasure = existingMeasure.toBuilder().build();
    Version updatedVersion = Version.builder().major(2).minor(3).revisionNumber(2).build();
    updatedMeasure.setVersion(updatedVersion);
    MeasureMetaData updatedMetaData = new MeasureMetaData();
    updatedMetaData.setDraft(false);
    updatedMeasure.setMeasureMetaData(updatedMetaData);
    when(measureRepository.save(any(Measure.class))).thenReturn(updatedMeasure);

    Export measureExport =
        Export.builder()
            .id("testId")
            .measureId("testMeasureId")
            .measureBundleJson("test measure json")
            .build();
    when(exportRepository.save(any(Export.class))).thenReturn(measureExport);
    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn("test measure json");

    versionService.createVersion("testMeasureId", "PATCH", "testUser", "accesstoken");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    Measure savedValue = measureCaptor.getValue();
    assertEquals(savedValue.getVersion().getMajor(), 2);
    assertEquals(savedValue.getVersion().getMinor(), 3);
    assertEquals(savedValue.getVersion().getRevisionNumber(), 2);
    assertFalse(savedValue.getMeasureMetaData().isDraft());
    assertThat(
        savedValue
            .getReviewMetaData()
            .getApprovalDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(today.truncatedTo(ChronoUnit.MINUTES)),
        is(0));
    assertThat(
        savedValue
            .getReviewMetaData()
            .getLastReviewDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(today.truncatedTo(ChronoUnit.MINUTES)),
        is(0));

    verify(exportRepository, times(1)).save(exportArgumentCaptor.capture());
    Export savedExport = exportArgumentCaptor.getValue();
    assertEquals(savedValue.getId(), savedExport.getMeasureId());
    assertEquals(measureExport.getMeasureBundleJson(), savedExport.getMeasureBundleJson());
  }

  @Test
  public void testCreateDraftSuccessfully() {
    TestCaseGroupPopulation clonedTestCaseGroupPopulation =
        TestCaseGroupPopulation.builder()
            .groupId("clonedGroupId1")
            .scoring("Cohort")
            .populationBasis("boolean")
            .build();
    Measure versionedMeasure = buildBasicMeasure();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    ReviewMetaData reviewMetaData =
        ReviewMetaData.builder().approvalDate(today).lastReviewDate(today).build();
    Measure versionedCopy =
        versionedMeasure.toBuilder()
            .id("2")
            .versionId("13-13-13-13")
            .measureName("Test")
            .measureMetaData(metaData)
            .groups(List.of(cvGroup.toBuilder().id(ObjectId.get().toString()).build()))
            .testCases(
                List.of(
                    testCase.toBuilder()
                        .id(ObjectId.get().toString())
                        .groupPopulations(List.of(clonedTestCaseGroupPopulation))
                        .build()))
            .reviewMetaData(reviewMetaData)
            .build();

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(versionedMeasure));
    when(measureRepository.existsByMeasureSetIdAndActiveAndMeasureMetaDataDraft(
            anyString(), anyBoolean(), anyBoolean()))
        .thenReturn(false);
    when(measureRepository.save(any(Measure.class))).thenReturn(versionedCopy);
    when(actionLogService.logAction(anyString(), any(), any(), anyString())).thenReturn(true);

    Measure draft = versionService.createDraft(versionedMeasure.getId(), "Test", "test-user");

    assertThat(draft.getMeasureName(), is(equalTo("Test")));
    // draft flag to true
    assertThat(draft.getMeasureMetaData().isDraft(), is(equalTo(true)));
    // version remains same
    assertThat(draft.getVersion().getMajor(), is(equalTo(2)));
    assertThat(draft.getVersion().getMinor(), is(equalTo(3)));
    assertThat(draft.getVersion().getRevisionNumber(), is(equalTo(1)));
    assertThat(draft.getGroups().size(), is(equalTo(1)));
    assertFalse(draft.getGroups().stream().anyMatch(item -> "xyz-p12r-12ert".equals(item.getId())));
    assertThat(draft.getTestCases().size(), is(equalTo(1)));
    assertFalse(draft.getGroups().stream().anyMatch(item -> "testId1".equals(item.getId())));
    assertThat(
        draft.getTestCases().get(0).getGroupPopulations().get(0).getGroupId(),
        is(equalTo("clonedGroupId1")));
    assertEquals(draft.getReviewMetaData().getApprovalDate().toString(), today.toString());
    assertEquals(draft.getReviewMetaData().getLastReviewDate().toString(), today.toString());
  }

  @Test
  public void testCreateDraftSuccessfullyWithoutGroups() {

    Measure versionedMeasure =
        Measure.builder()
            .id("1")
            .measureSetId("1-1-1-1")
            .measureName("Test")
            .createdBy("test-user")
            .cql("library TestCQLLib version '2.3.001'")
            .cmsId("CMS12")
            .versionId("12-12-12-12")
            .version(Version.builder().major(2).minor(3).revisionNumber(1).build())
            .measureMetaData(new MeasureMetaData())
            .build();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    ReviewMetaData reviewMetaData =
        ReviewMetaData.builder().approvalDate(today).lastReviewDate(today).build();
    Measure versionedCopy =
        versionedMeasure.toBuilder()
            .id("2")
            .versionId("13-13-13-13")
            .measureName("Test")
            .measureMetaData(metaData)
            .groups(List.of())
            .testCases(List.of())
            .reviewMetaData(reviewMetaData)
            .build();

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(versionedMeasure));
    when(measureRepository.existsByMeasureSetIdAndActiveAndMeasureMetaDataDraft(
            anyString(), anyBoolean(), anyBoolean()))
        .thenReturn(false);
    when(measureRepository.save(any(Measure.class))).thenReturn(versionedCopy);
    when(actionLogService.logAction(anyString(), any(), any(), anyString())).thenReturn(true);

    Measure draft = versionService.createDraft(versionedMeasure.getId(), "Test", "test-user");

    assertThat(draft.getMeasureName(), is(equalTo("Test")));
    // draft flag to true
    assertThat(draft.getMeasureMetaData().isDraft(), is(equalTo(true)));
    // version remains same
    assertThat(draft.getVersion().getMajor(), is(equalTo(2)));
    assertThat(draft.getVersion().getMinor(), is(equalTo(3)));
    assertThat(draft.getVersion().getRevisionNumber(), is(equalTo(1)));
    assertThat(draft.getGroups().size(), is(equalTo(0)));
    assertThat(draft.getTestCases().size(), is(equalTo(0)));
    assertEquals(draft.getReviewMetaData().getApprovalDate().toString(), today.toString());
    assertEquals(draft.getReviewMetaData().getLastReviewDate().toString(), today.toString());
  }

  @Test
  public void testCreateDraftWhenMeasureDoesNotExists() {
    String measureId = "nonExistent";
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());
    Exception ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> versionService.createDraft(measureId, "Test", "test-user"));
    assertThat(ex.getMessage(), is(equalTo("Could not find Measure with id: " + measureId)));
  }

  @Test
  public void testCreateDraftWhenDraftUserUnAuthorized() {
    String user = "bad guy";
    Measure measure = buildBasicMeasure();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    doThrow(new UnauthorizedException("Measure", "1", user))
        .when(measureService)
        .verifyAuthorization(anyString(), any(Measure.class));

    Exception ex =
        assertThrows(
            UnauthorizedException.class,
            () -> versionService.createDraft(measure.getId(), "Test", user));
    assertThat(
        ex.getMessage(), is(equalTo("User " + user + " is not authorized for Measure with ID 1")));
  }

  @Test
  public void testCreateDraftWhenDraftAlreadyExists() {
    Measure measure = buildBasicMeasure();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureRepository.existsByMeasureSetIdAndActiveAndMeasureMetaDataDraft(
            anyString(), anyBoolean(), anyBoolean()))
        .thenReturn(true);

    Exception ex =
        assertThrows(
            MeasureNotDraftableException.class,
            () -> versionService.createDraft(measure.getId(), "Test", "test-user"));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "Can not create a draft for the measure \"Test\". Only one draft is permitted per measure.")));
  }

  private Measure buildBasicMeasure() {
    return Measure.builder()
        .id("1")
        .measureSetId("1-1-1-1")
        .measureName("Test")
        .createdBy("test-user")
        .cql("library TestCQLLib version '2.3.001'")
        .cmsId("CMS12")
        .versionId("12-12-12-12")
        .version(Version.builder().major(2).minor(3).revisionNumber(1).build())
        .measureMetaData(new MeasureMetaData())
        .groups(List.of(cvGroup))
        .testCases(List.of(testCase))
        .build();
  }

  @Test
  public void testSetMeasureReviewMetaDataForDraft() {
    Measure measure =
        Measure.builder()
            .reviewMetaData(
                ReviewMetaData.builder().approvalDate(today).lastReviewDate(today).build())
            .build();
    versionService.setMeasureReviewMetaDataForDraft(measure);
    assertNull(measure.getReviewMetaData().getApprovalDate());
    assertNull(measure.getReviewMetaData().getLastReviewDate());
  }

  @Test
  public void testSetMeasureReviewMetaData() {
    Measure measure = Measure.builder().build();
    Instant now = Instant.now();
    versionService.setMeasureReviewMetaData(measure);

    assertThat(
        measure
            .getReviewMetaData()
            .getLastReviewDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(now.truncatedTo(ChronoUnit.MINUTES)),
        is(0));

    assertThat(
        measure
            .getReviewMetaData()
            .getApprovalDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(now.truncatedTo(ChronoUnit.MINUTES)),
        is(0));
  }

  @Test
  public void testReplaceMeasureReviewMetaData() {
    Measure measure = Measure.builder().build();
    Instant now = Instant.now();
    measure.setReviewMetaData(ReviewMetaData.builder().build());
    versionService.setMeasureReviewMetaData(measure);
    assertThat(
        measure
            .getReviewMetaData()
            .getLastReviewDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(now.truncatedTo(ChronoUnit.MINUTES)),
        is(0));

    assertThat(
        measure
            .getReviewMetaData()
            .getApprovalDate()
            .truncatedTo(ChronoUnit.MINUTES)
            .compareTo(now.truncatedTo(ChronoUnit.MINUTES)),
        is(0));

    assertThat(
        measure.getReviewMetaData().getLastReviewDate(),
        is(measure.getReviewMetaData().getApprovalDate()));
  }
}
