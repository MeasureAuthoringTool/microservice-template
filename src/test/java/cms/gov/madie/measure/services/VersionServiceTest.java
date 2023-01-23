package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BadVersionRequestException;
import cms.gov.madie.measure.exceptions.MeasureNotDraftableException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VersionServiceTest {

  @Mock private MeasureRepository measureRepository;

  @Mock ActionLogService actionLogService;

  @InjectMocks VersionService versionService;

  @Captor private ArgumentCaptor<Measure> measureCaptor;

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
  public void testCreateVersionThrowsUnauthorizedExceptionForNonOwner() {
    Measure existingMeasure =
        Measure.builder().id("testMeasureId").createdBy("anotherUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(existingMeasure));

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
  public void testCreateVersionThrowsBadVersionRequestExceptionForInvalidResources() {
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

    assertThrows(
        BadVersionRequestException.class,
        () -> versionService.createVersion("testMeasureId", "MAJOR", "testUser", "accesstoken"));
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
    Measure existingMeasure =
        Measure.builder()
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

    versionService.createVersion("testMeasureId", "MAJOR", "testUser", "accesstoken");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    Measure savedValue = measureCaptor.getValue();
    assertEquals(savedValue.getVersion().getMajor(), 3);
    assertEquals(savedValue.getVersion().getMinor(), 0);
    assertEquals(savedValue.getVersion().getRevisionNumber(), 0);
    assertFalse(savedValue.getMeasureMetaData().isDraft());
  }

  @Test
  public void testCreateVersionMinorSuccess() throws Exception {
    Measure existingMeasure =
        Measure.builder()
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
  }

  @Test
  public void testCreateVersionPatchSuccess() throws Exception {
    Measure existingMeasure =
        Measure.builder()
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

    versionService.createVersion("testMeasureId", "PATCH", "testUser", "accesstoken");

    verify(measureRepository, times(1)).save(measureCaptor.capture());
    Measure savedValue = measureCaptor.getValue();
    assertEquals(savedValue.getVersion().getMajor(), 2);
    assertEquals(savedValue.getVersion().getMinor(), 3);
    assertEquals(savedValue.getVersion().getRevisionNumber(), 2);
    assertFalse(savedValue.getMeasureMetaData().isDraft());
  }

  @Test
  public void testCreateDraftSuccessfully() {
    Measure versionedMeasure = buildBasicMeasure();
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDraft(true);
    Measure versionedCopy =
        versionedMeasure
            .toBuilder()
            .id("2")
            .versionId("13-13-13-13")
            .measureName("Test")
            .measureMetaData(metaData)
            .groups(List.of())
            .testCases(List.of())
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
    // no groups and test cases
    assertThat(draft.getGroups().size(), is(equalTo(0)));
    assertThat(draft.getTestCases().size(), is(equalTo(0)));
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
        .testCases(List.of(new TestCase()))
        .groups(List.of(new Group()))
        .build();
  }
}
