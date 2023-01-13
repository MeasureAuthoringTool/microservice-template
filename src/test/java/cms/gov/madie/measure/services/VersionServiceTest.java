package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cms.gov.madie.measure.exceptions.BadVersionRequestException;
import cms.gov.madie.measure.exceptions.InternalServerErrorException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.TestCase;

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
  public void testCreateVersionThrowsBadVersionRequestExceptionForInvalidResources() {
    Measure existingMeasure = Measure.builder().id("testMeasureId").createdBy("testUser").build();
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
  public void testCreateVersionThrowsInternalServerErrorException() {
    Measure existingMeasure =
        Measure.builder()
            .id("testMeasureId")
            .measureSetId("testMeasureSetId")
            .createdBy("testUser")
            .build();

    Exception cause = new RuntimeException("Internal Server Error!");
    when(measureRepository.findMaxVersionByMeasureSetId(anyString())).thenThrow(cause);

    assertThrows(
        InternalServerErrorException.class,
        () -> versionService.getNextVersion(existingMeasure, "MAJOR"),
        "Unable to version measure with id: testMeasureId");
    verify(measureRepository, times(1)).findMaxVersionByMeasureSetId(anyString());
  }

  @Test
  public void testGetNextVersionOtherException() {
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
  public void testCreateVersionMajorSuccess() {
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
  public void testCreateVersionMinorSuccess() {
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
  public void testCreateVersionPatchSuccess() {
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
}
