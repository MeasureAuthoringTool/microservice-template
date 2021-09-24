package cms.gov.madie.measure.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.resources.MeasureController;

@ExtendWith(MockitoExtension.class)
class MeasureControllerTest {

  @Mock private MeasureRepository repository;

  @InjectMocks private MeasureController controller;

  @Test
  void saveMeasure() {
    Measure measure = new Measure();
    measure.setMeasureSetId("IDIDID");
    Mockito.doReturn(measure).when(repository).save(ArgumentMatchers.<Measure>any());

    Measure measures = new Measure();

    ResponseEntity<Measure> response = controller.addMeasure(measures);
    assertEquals("IDIDID", response.getBody().getMeasureSetId());
  }

  @Test
  void getMeasures() {
    Measure measure = new Measure();
    measure.setMeasureSetId("IDIDID");
    List<Measure> measures = Arrays.asList(measure);
    Mockito.doReturn(measures).when(repository).findAll();

    ResponseEntity<List<Measure>> response = controller.getMeasures();
    assertEquals("IDIDID", response.getBody().get(0).getMeasureSetId());
  }
}
