package cms.gov.madie.measure.resources;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.models.MeasurePopulation;
import cms.gov.madie.measure.services.MeasureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.MeasureMetaData;
import cms.gov.madie.measure.repositories.MeasureRepository;

@ExtendWith(MockitoExtension.class)
class MeasureControllerTest {

  @Mock private MeasureRepository repository;
  @Mock private MeasureService measureService;

  @InjectMocks private MeasureController controller;

  private Measure measure;

  @BeforeEach
  public void setUp() {
    measure = new Measure();
    measure.setActive(true);
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
    Page<Measure> measures = new PageImpl<>(List.of(measure));
    when(repository.findAllByActive(any(Boolean.class), any(Pageable.class))).thenReturn(measures);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<Page<Measure>> response = controller.getMeasures(principal, false, 10, 0);
    verify(repository, times(1)).findAllByActive(any(Boolean.class), any(Pageable.class));
    verifyNoMoreInteractions(repository);
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getContent());
    assertNotNull(response.getBody().getContent().get(0));
    assertEquals("IDIDID", response.getBody().getContent().get(0).getMeasureSetId());
  }

  @Test
  void getMeasuresWithCurrentUserFilter() {
    Page<Measure> measures = new PageImpl<>(List.of(measure));
    when(repository.findAllByCreatedByAndActive(
            anyString(), any(Boolean.class), any(Pageable.class)))
        .thenReturn(measures);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<Page<Measure>> response = controller.getMeasures(principal, true, 10, 0);
    verify(repository, times(1))
        .findAllByCreatedByAndActive(eq("test.user"), any(Boolean.class), any(Pageable.class));
    verifyNoMoreInteractions(repository);
    assertNotNull(response.getBody().getContent());
    assertNotNull(response.getBody().getContent().get(0));
    assertEquals("IDIDID", response.getBody().getContent().get(0).getMeasureSetId());
  }

  @Test
  void getMeasure() {
    String id = "testid";
    Optional<Measure> optionalMeasure = Optional.of(measure);
    doReturn(optionalMeasure).when(repository).findByIdAndActive(id, true);
    // measure found
    ResponseEntity<Measure> response = controller.getMeasure(id);
    assertEquals(
        measure.getMeasureName(), Objects.requireNonNull(response.getBody()).getMeasureName());

    // if measure not found
    Optional<Measure> empty = Optional.empty();
    doReturn(empty).when(repository).findByIdAndActive(id, true);
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
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDescription("TestDescription");
    metaData.setCopyright("TestCopyright");
    metaData.setDisclaimer("TestDisclaimer");
    metaData.setRationale("TestRationale");
    measure.setMeasureMetaData(metaData);
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

    ResponseEntity<String> response = controller.updateMeasure(m1.getId(), m1, principal);
    assertEquals("Measure updated successfully.", response.getBody());
    verify(repository, times(1)).save(saveMeasureArgCaptor.capture());
    Measure savedMeasure = saveMeasureArgCaptor.getValue();
    assertThat(savedMeasure.getCreatedAt(), is(notNullValue()));
    assertThat(savedMeasure.getCreatedBy(), is(equalTo("test.user")));
    assertThat(savedMeasure.getLastModifiedAt(), is(notNullValue()));
    assertThat(savedMeasure.getLastModifiedBy(), is(equalTo("test.user2")));
    assertThat(savedMeasure.getMeasureMetaData().getDescription(), is(equalTo("TestDescription")));
    assertThat(savedMeasure.getMeasureMetaData().getCopyright(), is(equalTo("TestCopyright")));
    assertThat(savedMeasure.getMeasureMetaData().getDisclaimer(), is(equalTo("TestDisclaimer")));
    assertThat(savedMeasure.getMeasureMetaData().getRationale(), is(equalTo("TestRationale")));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForNullId() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    assertThrows(
        InvalidIdException.class, () -> controller.updateMeasure(null, measure, principal));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForInvalidCredentials() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("aninvalidUser@gmail.com");

    Measure testMeasure;
    testMeasure = new Measure();
    testMeasure.setActive(false);
    testMeasure.setCreatedBy("anotheruser");
    testMeasure.setId("testid");
    testMeasure.setMeasureName("MSR01");
    testMeasure.setVersion("0.001");
    doThrow(new InvalidDeletionCredentialsException("invalidUser@gmail.com"))
        .when(measureService)
        .checkDeletionCredentials(anyString(), anyString());
    assertThrows(
        InvalidDeletionCredentialsException.class,
        () -> controller.updateMeasure("testid", testMeasure, principal));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForEmptyStringId() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    assertThrows(InvalidIdException.class, () -> controller.updateMeasure("", measure, principal));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForNonMatchingIds() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");
    Measure m1234 = measure.toBuilder().id("ID1234").build();

    assertThrows(
        InvalidIdException.class, () -> controller.updateMeasure("ID5678", m1234, principal));
  }

  @Test
  void updateNonExistingMeasure() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    // no measure id specified
    assertThrows(
        InvalidIdException.class,
        () -> controller.updateMeasure(measure.getId(), measure, principal));
    // non-existing measure or measure with fake id
    measure.setId("5399aba6e4b0ae375bfdca88");
    Optional<Measure> empty = Optional.empty();

    doReturn(empty).when(repository).findById(measure.getId());

    ResponseEntity<String> response = controller.updateMeasure(measure.getId(), measure, principal);
    assertEquals("Measure does not exist.", response.getBody());
  }

  @Test
  void createGroup() {
    Group group =
        Group.builder()
            .scoring("Cohort")
            .population(Map.of(MeasurePopulation.INITIAL_POPULATION, "Initial Population"))
            .build();
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    doReturn(group)
        .when(measureService)
        .createOrUpdateGroup(any(Group.class), any(String.class), any(String.class));

    Group newGroup = new Group();

    ResponseEntity<Group> response = controller.createGroup(newGroup, "measure-id", principal);
    assertNotNull(response.getBody());
    assertEquals(group.getId(), response.getBody().getId());
    assertEquals(group.getScoring(), response.getBody().getScoring());
    assertEquals(group.getPopulation(), response.getBody().getPopulation());
  }

  @Test
  void updateGroup() {
    Group group =
        Group.builder()
            .scoring("Cohort")
            .population(Map.of(MeasurePopulation.INITIAL_POPULATION, "Initial Population"))
            .build();
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    doReturn(group)
        .when(measureService)
        .createOrUpdateGroup(any(Group.class), any(String.class), any(String.class));

    Group newGroup = new Group();

    ResponseEntity<Group> response = controller.updateGroup(newGroup, "measure-id", principal);
    assertNotNull(response.getBody());
    assertEquals(group.getId(), response.getBody().getId());
    assertEquals(group.getScoring(), response.getBody().getScoring());
    assertEquals(group.getPopulation(), response.getBody().getPopulation());
  }

  @Test
  void testBundleMeasureThrowsNotFoundException() {
    Principal principal = mock(Principal.class);
    when(repository.findById(anyString())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> controller.getMeasureBundle("MeasureID", principal, "Bearer TOKEN"));
  }

  @Test
  void testBundleMeasureThrowsAccessException() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final Measure measure = Measure.builder().createdBy("OtherUser").build();
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        UnauthorizedException.class,
        () -> controller.getMeasureBundle("MeasureID", principal, "Bearer TOKEN"));
  }

  @Test
  void testBundleMeasureThrowsOperationException() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final Measure measure = Measure.builder().createdBy("test.user").build();
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureService.bundleMeasure(any(Measure.class), anyString()))
        .thenThrow(
            new BundleOperationException("Measure", "MeasureID", new RuntimeException("cause")));
    assertThrows(
        BundleOperationException.class,
        () -> controller.getMeasureBundle("MeasureID", principal, "Bearer TOKEN"));
  }

  @Test
  void testBundleMeasureReturnsBundleString() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final String json = "{\"message\": \"GOOD JSON\"}";
    final Measure measure = Measure.builder().createdBy("test.user").build();
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureService.bundleMeasure(any(Measure.class), anyString())).thenReturn(json);
    ResponseEntity<String> output =
        controller.getMeasureBundle("MeasureID", principal, "Bearer TOKEN");
    assertThat(output, is(notNullValue()));
    assertThat(output.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(output.getBody(), is(equalTo(json)));
  }
}
