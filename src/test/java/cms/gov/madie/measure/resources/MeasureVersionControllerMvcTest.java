package cms.gov.madie.measure.resources;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import cms.gov.madie.measure.exceptions.BadVersionRequestException;
import cms.gov.madie.measure.exceptions.InternalServerErrorException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.services.VersionService;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;

@WebMvcTest({MeasureVersionController.class})
@ActiveProfiles("test")
public class MeasureVersionControllerMvcTest {

  @MockBean private VersionService versionService;

  @Captor private ArgumentCaptor<Measure> measureArgumentCaptor;

  @Autowired private MockMvc mockMvc;

  private static final String TEST_USER_ID = "test-user-id";

  @Test
  public void testCreateVersionReturnsResourceNotFoundException() throws Exception {
    when(versionService.createVersion(anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new ResourceNotFoundException("Measure", "testMeasureId"));
    mockMvc
        .perform(
            put("/measures/testMeasureId/version/?versionType=MAJOR")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta-token")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

    verify(versionService, times(1))
        .createVersion(eq("testMeasureId"), eq("MAJOR"), eq(TEST_USER_ID), eq("test-okta-token"));
  }

  @Test
  public void testCreateVersionReturnsBadVersionRequestException() throws Exception {
    doThrow(
            new BadVersionRequestException(
                "Measure", "testMeasureId", TEST_USER_ID, "Invalid version request."))
        .when(versionService)
        .createVersion(anyString(), anyString(), anyString(), anyString());

    mockMvc
        .perform(
            put("/measures/testMeasureId/version/?versionType=NOTVALIDVERSIONTYPE")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta-token")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

    verify(versionService, times(1))
        .createVersion(
            eq("testMeasureId"),
            eq("NOTVALIDVERSIONTYPE"),
            eq(TEST_USER_ID),
            eq("test-okta-token"));
  }

  @Test
  public void testCreateVersionReturnsUnauthorizedException() throws Exception {
    doThrow(new UnauthorizedException("Measure", "testMeasureId", TEST_USER_ID))
        .when(versionService)
        .createVersion(anyString(), anyString(), anyString(), anyString());

    mockMvc
        .perform(
            put("/measures/testMeasureId/version/?versionType=MAJOR")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta-token")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

    verify(versionService, times(1))
        .createVersion(eq("testMeasureId"), eq("MAJOR"), eq(TEST_USER_ID), eq("test-okta-token"));
  }

  @Test
  public void testCreateVersionReturnsInternalServerErrorException() throws Exception {
    doThrow(
            new InternalServerErrorException(
                "Internal server error!", new RuntimeException("Internal Server Error!")))
        .when(versionService)
        .createVersion(anyString(), anyString(), anyString(), anyString());

    mockMvc
        .perform(
            put("/measures/version/testMeasureId?versionType=MAJOR")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta-token")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

    verify(versionService, times(1))
        .createVersion(eq("testMeasureId"), eq("MAJOR"), eq(TEST_USER_ID), eq("test-okta-token"));
  }

  @Test
  public void testCreateVersionReturnsCreatedVersion() throws Exception {
    Measure updatedMeasure = Measure.builder().id("testMeasureId").createdBy("testUser").build();
    Version updatedVersion = Version.builder().major(3).minor(0).revisionNumber(0).build();
    updatedMeasure.setVersion(updatedVersion);
    MeasureMetaData updatedMetaData = new MeasureMetaData();
    updatedMetaData.setDraft(false);
    updatedMeasure.setMeasureMetaData(updatedMetaData);
    when(versionService.createVersion(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(updatedMeasure);

    mockMvc
        .perform(
            put("/measures/testMeasureId/version/?versionType=MAJOR")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header("Authorization", "test-okta-token")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

    verify(versionService, times(1))
        .createVersion(eq("testMeasureId"), eq("MAJOR"), eq(TEST_USER_ID), eq("test-okta-token"));
  }
}
