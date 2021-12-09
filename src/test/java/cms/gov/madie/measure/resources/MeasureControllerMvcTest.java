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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeasureController.class)
public class MeasureControllerMvcTest {

  @MockBean private MeasureRepository measureRepository;
  @Autowired private MockMvc mockMvc;
  @Captor ArgumentCaptor<Measure> measureArgumentCaptor;

  @Test
  public void testUpdateSteward() throws Exception {
    String measureId = "f225481c-921e-4015-9e14-e5046bfac9ff";
    String steward = "d0cc18ce-63fd-4b94-b713-c1d9fd6b2329";

    when(measureRepository.findById(eq(measureId))).thenReturn(Optional.of(mock(Measure.class)));
    when(measureRepository.save(any(Measure.class))).thenReturn(mock(Measure.class));

    final String measureAsJson =
        "{\"id\": \"%s\", \"measureMetaData\": { \"measureSteward\" : \"%s\" }}"
            .formatted(measureId, steward);
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
    Assertions.assertEquals(steward, savedMeasure.getMeasureMetaData().getMeasureSteward());
  }
}
