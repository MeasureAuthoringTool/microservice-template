package cms.gov.madie.measure.resources;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
    ArgumentCaptor<Measure> saveMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    Mockito.doReturn(measure).when(repository).save(ArgumentMatchers.any());

    Measure measures = new Measure();
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<Measure> response = controller.addMeasure(measures, principal);
    assertEquals("IDIDID", response.getBody().getMeasureSetId());

    verify(repository, times(1)).save(saveMeasureArgCaptor.capture());
    Measure savedMeasure = saveMeasureArgCaptor.getValue();
    assertThat(savedMeasure.getCreatedBy(), is(equalTo("test.user")));
    assertThat(savedMeasure.getLastModifiedBy(), is(equalTo("test.user")));
    assertThat(savedMeasure.getCreatedAt(), is(notNullValue()));
    assertThat(savedMeasure.getLastModifiedAt(), is(notNullValue()));
  }

  @Test
  void getMeasuresWithoutCurrentUserFilter() {
    List<Measure> measures = List.of(measure);
    when(repository.findAll()).thenReturn(measures);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<List<Measure>> response = controller.getMeasures(principal, false);
    verify(repository, times(1)).findAll();
    verifyNoMoreInteractions(repository);
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().get(0));
    assertEquals("IDIDID", response.getBody().get(0).getMeasureSetId());
  }

  @Test
  void getMeasuresWithCurrentUserFilter() {
    List<Measure> measures = List.of(measure);
    when(repository.findAllByCreatedBy(anyString())).thenReturn(measures);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<List<Measure>> response = controller.getMeasures(principal, true);
    verify(repository, times(1)).findAllByCreatedBy(eq("test.user"));
    verifyNoMoreInteractions(repository);
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().get(0));
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
    Optional<Measure> persistedMeasure = Optional.of(measure);
    measure.setId("5399aba6e4b0ae375bfdca88");

    Mockito.doReturn(persistedMeasure)
        .when(repository)
        .findById(ArgumentMatchers.eq(measure.getId()));

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
    measure.setId("5399aba6e4b0ae375bfdca88");
    Optional<Measure> empty = Optional.empty();

    Mockito.doReturn(empty).when(repository).findById(measure.getId());

    response = controller.updateMeasure(measure);
    assertEquals("Measure does not exist.", response.getBody());
  }
}
