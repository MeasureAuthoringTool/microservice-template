package cms.gov.madie.measure.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
class MeasureControllerTest {

  @Mock private MeasureRepository repository;

  @InjectMocks private MeasureController controller;

  private Measure measure;

  @BeforeEach
  public void setUp() {
    measure = new Measure();
    measure.setMeasureSetId("IDIDID");
    measure.setMeasureName("MSR01");
    measure.setVersion("0.001");
  }

  @Test
  void saveMeasure() {
    Mockito.doReturn(measure).when(repository).save(ArgumentMatchers.<Measure>any());

    Measure measures = new Measure();

    ResponseEntity<Measure> response = controller.addMeasure(measures);
    assertEquals("IDIDID", response.getBody().getMeasureSetId());
  }

  @Test
  void getMeasures() {
    List<Measure> measures = Arrays.asList(measure);
    Mockito.doReturn(measures).when(repository).findAll();

    ResponseEntity<List<Measure>> response = controller.getMeasures();
    assertEquals("IDIDID", response.getBody().get(0).getMeasureSetId());
  }

  @Test
  void getMeasure() {
    String id = "testid";
    Optional<Measure> optionalMeasure = Optional.of(measure);
    Mockito.doReturn(optionalMeasure).when(repository).findById(id);
    // measure found
    ResponseEntity<Measure> response = controller.getMeasure(id);
    assertEquals(
        measure.getMeasureName(), Objects.requireNonNull(response.getBody()).getMeasureName());

    // if measure not found
    Optional<Measure> empty = Optional.empty();
    Mockito.doReturn(empty).when(repository).findById(id);
    response = controller.getMeasure(id);
    assertNull(response.getBody());
    assertEquals(response.getStatusCodeValue(), 404);
  }

  @Test
  void updateMeasureSuccessfully() {
    measure.setId(new ObjectId());
    Optional<Measure> persistedMeasure = Optional.of(measure);
    Mockito.doReturn(persistedMeasure).when(repository).findById(measure.getId().toString());

    Mockito.doReturn(measure).when(repository).save(measure);

    ResponseEntity<String> response = controller.updateMeasure(measure);
    assertEquals("Measure updated successfully.", response.getBody());
  }

  @Test
  void updateNonExistingMeasure() {
    // no measure id specified
    ResponseEntity<String> response = controller.updateMeasure(measure);
    assertEquals("Measure does not exist.", response.getBody());

    // non-existing measure or measure with fake id
    measure.setId(new ObjectId("5399aba6e4b0ae375bfdca88"));
    Optional<Measure> empty = Optional.empty();

    Mockito.doReturn(empty).when(repository).findById(measure.getId().toString());

    response = controller.updateMeasure(measure);
    assertEquals("Measure does not exist.", response.getBody());
  }
}
