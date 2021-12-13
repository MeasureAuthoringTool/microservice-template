package cms.gov.madie.measure.resources;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.repositories.MeasureRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeasureController.class)
public class MeasureControllerMvcTest {

  @MockBean private MeasureRepository measureRepository;
  @Autowired private MockMvc mockMvc;
  @Captor ArgumentCaptor<Measure> measureArgumentCaptor;

  @Test
  public void testUpdatePassed() throws Exception {
    String measureId = "f225481c-921e-4015-9e14-e5046bfac9ff";
    String measureName = "TestMeasure";
    String steward = "d0cc18ce-63fd-4b94-b713-c1d9fd6b2329";

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(mock(Measure.class)));
    when(measureRepository.save(any(Measure.class))).thenReturn(mock(Measure.class));

    final String measureAsJson =
        "{\"id\": \"%s\", \"measureName\": \"%s\", \"measureMetaData\": { \"measureSteward\" : \"%s\" }}"
            .formatted(measureId, measureName, steward);
    mockMvc
        .perform(
            put("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().string("Measure updated successfully."));

    verify(measureRepository, times(1)).findById(eq(measureId));
    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository);
    Measure savedMeasure = measureArgumentCaptor.getValue();
    Assertions.assertNotNull(savedMeasure.getMeasureMetaData());
    Assertions.assertEquals(measureName, savedMeasure.getMeasureName());
    Assertions.assertEquals(steward, savedMeasure.getMeasureMetaData().getMeasureSteward());
  }

  @Test
  public void testNewMeasureNameMustNotBeNull() throws Exception {
    final String measureAsJson = "{  }";
    mockMvc
        .perform(
            post("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.measureName").value("Measure Name is Required"));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureNameMustNotBeNull() throws Exception {
    final String measureAsJson = "{  }";
    mockMvc
        .perform(
            put("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.measureName").value("Measure Name is Required"));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureNameMustNotBeEmpty() throws Exception {
    final String measureAsJson = "{ \"measureName\":\"\" }";
    mockMvc
        .perform(
            post("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.measureName").value("Measure Name is Required"));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureNameMustNotBeEmpty() throws Exception {
    final String measureAsJson = "{ \"measureName\":\"\" }";
    mockMvc
        .perform(
            put("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.measureName").value("Measure Name is Required"));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureNoUnderscore() throws Exception {
    final String measureAsJson = "{ \"measureName\":\"A_Name\" }";
    mockMvc
        .perform(
            post("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.measureName").value("Measure Name can not contain underscores"));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureNoUnderscore() throws Exception {
    final String measureAsJson = "{ \"measureName\":\"A_Name\" }";
    mockMvc
        .perform(
            put("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.measureName").value("Measure Name can not contain underscores"));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasureNameMaxLength() throws Exception {
    final String measureName = "A".repeat(501);
    final String measureAsJson = "{ \"measureName\":\"%s\"  }".formatted(measureName);
    mockMvc
        .perform(
            post("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.measureName")
                .value(
                    "Measure Name contains at least one letter and can not be more than 500 characters"));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testUpdateMeasureNameMaxLength() throws Exception {
    final String measureName = "A".repeat(501);
    final String measureAsJson = "{ \"measureName\":\"%s\"  }".formatted(measureName);
    mockMvc
        .perform(
            put("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.measureName")
                .value(
                    "Measure Name contains at least one letter and can not be more than 500 characters"));
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testNewMeasurePassed() throws Exception {
    Measure saved = new Measure();
    saved.setId("id123");
    saved.setMeasureName("SavedMeasure");
    when(measureRepository.save(any(Measure.class))).thenReturn(saved);

    final String measureAsJson = "{\"measureName\": \"SavedMeasure\"}";
    mockMvc
        .perform(
            post("/measure").content(measureAsJson).contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.measureName").value("SavedMeasure"))
        .andExpect(jsonPath("$.id").value("id123"));

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository);
    Measure savedMeasure = measureArgumentCaptor.getValue();
    Assertions.assertEquals("SavedMeasure", savedMeasure.getMeasureName());
  }
}
