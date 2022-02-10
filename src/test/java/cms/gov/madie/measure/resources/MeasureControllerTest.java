package cms.gov.madie.measure.resources;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    doReturn(measure).when(repository).save(ArgumentMatchers.any());

    Measure measures = new Measure();
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<Measure> response = controller.addMeasure(measures, principal);
    assertNotNull(response.getBody());
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
    doReturn(optionalMeasure).when(repository).findById(id);
    // measure found
    ResponseEntity<Measure> response = controller.getMeasure(id);
    assertEquals(
        measure.getMeasureName(), Objects.requireNonNull(response.getBody()).getMeasureName());

    // if measure not found
    Optional<Measure> empty = Optional.empty();
    doReturn(empty).when(repository).findById(id);
    response = controller.getMeasure(id);
    assertNull(response.getBody());
    assertEquals(response.getStatusCodeValue(), 404);
  }

  @Test
  void updateMeasureSuccessfully() {
    ArgumentCaptor<Measure> saveMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    Instant createdAt = Instant.now().minus(300, ChronoUnit.SECONDS);
    Measure originalMeasure =
        measure
            .toBuilder()
            .id("5399aba6e4b0ae375bfdca88")
            .createdAt(createdAt)
            .createdBy("test.user")
            .build();

    Instant original = Instant.now().minus(140, ChronoUnit.HOURS);

    Measure m1 =
        originalMeasure
            .toBuilder()
            .createdBy("test.user")
            .createdAt(original)
            .lastModifiedBy("test.user")
            .lastModifiedAt(original)
            .build();

    doReturn(Optional.of(originalMeasure))
        .when(repository)
        .findById(ArgumentMatchers.eq(originalMeasure.getId()));

    doAnswer((args) -> args.getArgument(0))
        .when(repository)
        .save(ArgumentMatchers.any(Measure.class));

    ResponseEntity<String> response = controller.updateMeasure(m1, principal);
    assertEquals("Measure updated successfully.", response.getBody());
    verify(repository, times(1)).save(saveMeasureArgCaptor.capture());
    Measure savedMeasure = saveMeasureArgCaptor.getValue();
    assertThat(savedMeasure.getCreatedAt(), is(notNullValue()));
    assertThat(savedMeasure.getCreatedBy(), is(equalTo("test.user")));
    assertThat(savedMeasure.getLastModifiedAt(), is(notNullValue()));
    assertThat(savedMeasure.getLastModifiedBy(), is(equalTo("test.user2")));
  }

  @Test
  void updateNonExistingMeasure() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    // no measure id specified
    ResponseEntity<String> response = controller.updateMeasure(measure, principal);
    assertEquals("Measure does not exist.", response.getBody());

    // non-existing measure or measure with fake id
    measure.setId("5399aba6e4b0ae375bfdca88");
    Optional<Measure> empty = Optional.empty();

    doReturn(empty).when(repository).findById(measure.getId());

    response = controller.updateMeasure(measure, principal);
    assertEquals("Measure does not exist.", response.getBody());
  }
}
