package cms.gov.madie.measure.resources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import cms.gov.madie.measure.exceptions.BadVersionRequestException;
import cms.gov.madie.measure.exceptions.InternalServerErrorException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.services.VersionService;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;

@ExtendWith(MockitoExtension.class)
public class MeasureVersionControllerTest {

  @Mock VersionService versionService;

  @InjectMocks MeasureVersionController measureVersionController;

  @Mock Principal principal;

  private Measure measure;

  @BeforeEach
  public void setUp() {
    measure = new Measure();
    measure.setId("testMeasureId");
  }

  @Test
  public void testCreateVersionReturnsResourceNotFoundException()
      throws InternalServerErrorException {
    when(principal.getName()).thenReturn("testUser");
    when(versionService.createVersion(anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new ResourceNotFoundException("Measure", measure.getId()));
    assertThrows(
        ResourceNotFoundException.class,
        () ->
            measureVersionController.createVersion(
                "testMeasureId", "MAJOR", principal, "accesstoken"));
  }

  @Test
  public void testCreateVersionReturnsBadVersionRequestException()
      throws InternalServerErrorException {
    when(principal.getName()).thenReturn("testUser");

    doThrow(
            new BadVersionRequestException(
                "Measure", measure.getId(), principal.getName(), "Invalid version request."))
        .when(versionService)
        .createVersion(anyString(), anyString(), anyString(), anyString());
    assertThrows(
        BadVersionRequestException.class,
        () ->
            measureVersionController.createVersion(
                "testMeasureId", "NOTVALIDVERSIONTYPE", principal, "accesstoken"));
  }

  @Test
  public void testCreateVersionReturnsUnauthorizedException() throws InternalServerErrorException {
    when(principal.getName()).thenReturn("testUser");

    doThrow(new UnauthorizedException("Measure", measure.getId(), principal.getName()))
        .when(versionService)
        .createVersion(anyString(), anyString(), anyString(), anyString());
    assertThrows(
        UnauthorizedException.class,
        () ->
            measureVersionController.createVersion(
                "testMeasureId", "MAJOR", principal, "accesstoken"));
  }

  @Test
  public void testCreateVersionReturnsInternalServerErrorException()
      throws InternalServerErrorException {
    when(principal.getName()).thenReturn("testUser");

    Exception cause = new RuntimeException("Internal Server Error!");
    InternalServerErrorException excpetion =
        new InternalServerErrorException("Internal server error!", cause);
    doThrow(excpetion)
        .when(versionService)
        .createVersion(anyString(), anyString(), anyString(), anyString());
    assertThrows(
        InternalServerErrorException.class,
        () ->
            measureVersionController.createVersion(
                "testMeasureId", "MAJOR", principal, "accesstoken"));
  }

  @Test
  public void testCreateVersionSuccess() throws InternalServerErrorException {
    when(principal.getName()).thenReturn("testUser");
    Measure updatedMeasure = Measure.builder().id("testMeasureId").createdBy("testUser").build();
    Version updatedVersion = Version.builder().major(3).minor(0).revisionNumber(0).build();
    updatedMeasure.setVersion(updatedVersion);
    MeasureMetaData updatedMetaData = new MeasureMetaData();
    updatedMetaData.setDraft(false);
    updatedMeasure.setMeasureMetaData(updatedMetaData);
    when(versionService.createVersion(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(updatedMeasure);

    ResponseEntity<Measure> entity =
        measureVersionController.createVersion("testMeasureId", "MAJOR", principal, "accesstoken");
    assertThat(entity, is(notNullValue()));
    assertThat(entity.getStatusCode(), is(HttpStatus.OK));
    assertThat(entity.getBody(), is(equalTo(updatedMeasure)));
  }
}
