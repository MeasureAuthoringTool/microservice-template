package cms.gov.madie.measure.resources;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.io.UnsupportedEncodingException;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import cms.gov.madie.measure.exceptions.*;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.*;
import gov.cms.madie.models.common.Version;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.GroupService;
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

import cms.gov.madie.measure.repositories.MeasureRepository;

@ExtendWith(MockitoExtension.class)
class MeasureControllerTest {

  @Mock private MeasureRepository repository;
  @Mock private MeasureService measureService;
  @Mock private GroupService groupService;
  @Mock private ActionLogService actionLogService;

  @InjectMocks private MeasureController controller;

  private Measure measure;

  @Captor private ArgumentCaptor<ActionType> actionTypeArgumentCaptor;
  @Captor private ArgumentCaptor<Class> targetClassArgumentCaptor;
  @Captor private ArgumentCaptor<String> targetIdArgumentCaptor;
  @Captor private ArgumentCaptor<String> performedByArgumentCaptor;

  @BeforeEach
  public void setUp() {
    measure = new Measure();
    measure.setActive(true);
    measure.setMeasureSetId("IDIDID");
    measure.setMeasureName("MSR01");
    measure.setVersion(new Version(0, 0, 1));
  }

  @Test
  void saveMeasure() {
    ArgumentCaptor<Measure> saveMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    measure.setId("testId");
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
    assertTrue(savedMeasure.getMeasureMetaData().isDraft());

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(actionTypeArgumentCaptor.getValue(), is(equalTo(ActionType.CREATED)));
    assertThat(performedByArgumentCaptor.getValue(), is(equalTo("test.user")));
  }

  @Test
  void saveMeasureAndDefaultToDraft() {
    ArgumentCaptor<Measure> saveMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    measure.setId("testId");
    measure.setMeasureMetaData(null);
    doReturn(measure).when(repository).save(ArgumentMatchers.any());

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<Measure> response = controller.addMeasure(measure, principal);
    assertNotNull(response.getBody());
    assertEquals("IDIDID", response.getBody().getMeasureSetId());

    verify(repository, times(1)).save(saveMeasureArgCaptor.capture());
    Measure savedMeasure = saveMeasureArgCaptor.getValue();
    assertThat(savedMeasure.getCreatedBy(), is(equalTo("test.user")));
    assertThat(savedMeasure.getLastModifiedBy(), is(equalTo("test.user")));
    assertThat(savedMeasure.getCreatedAt(), is(notNullValue()));
    assertThat(savedMeasure.getLastModifiedAt(), is(notNullValue()));
    assertTrue(savedMeasure.getMeasureMetaData().isDraft());
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
    when(repository.findAllByCreatedByAndActiveOrShared(
            anyString(), any(Boolean.class), anyString(), any(Pageable.class)))
        .thenReturn(measures);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<Page<Measure>> response = controller.getMeasures(principal, true, 10, 0);
    verify(repository, times(1))
        .findAllByCreatedByAndActiveOrShared(
            eq("test.user"),
            any(Boolean.class),
            eq(RoleEnum.SHARED_WITH.toString()),
            any(Pageable.class));
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
    metaData.setDraft(true);
    measure.setMeasureMetaData(metaData);
    measure.setMeasurementPeriodStart(new Date("12/02/2020"));
    measure.setMeasurementPeriodEnd(new Date("12/02/2021"));
    Measure originalMeasure =
        measure
            .toBuilder()
            .id("5399aba6e4b0ae375bfdca88")
            .createdAt(createdAt)
            .createdBy("test.user2")
            .build();

    Instant original = Instant.now().minus(140, ChronoUnit.HOURS);

    Measure m1 =
        originalMeasure
            .toBuilder()
            .createdBy("test.user")
            .createdAt(original)
            .measurementPeriodStart(new Date("12/02/2021"))
            .measurementPeriodEnd(new Date("12/02/2022"))
            .lastModifiedBy("test.user")
            .lastModifiedAt(original)
            .build();

    when(measureService.updateMeasure(
            any(Measure.class), anyString(), any(Measure.class), anyString()))
        .thenReturn(m1);

    when(repository.findById(anyString())).thenReturn(Optional.of(originalMeasure));

    ResponseEntity<Measure> response =
        controller.updateMeasure(m1.getId(), m1, principal, "Bearer TOKEN");
    assertThat(response.getBody(), is(notNullValue()));
    assertThat(response.getBody(), is(equalTo(m1)));
    assertEquals(m1, response.getBody());
    verify(measureService, times(1))
        .updateMeasure(
            any(Measure.class), anyString(), saveMeasureArgCaptor.capture(), anyString());
    assertThat(saveMeasureArgCaptor.getValue(), is(equalTo(m1)));

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(actionTypeArgumentCaptor.getValue(), is(equalTo(ActionType.UPDATED)));
    assertThat(performedByArgumentCaptor.getValue(), is(equalTo("test.user2")));
  }

  @Test
  void updateMeasureSuccessfullyLogDeleted() {
    ArgumentCaptor<Measure> saveMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    Instant createdAt = Instant.now().minus(300, ChronoUnit.SECONDS);
    MeasureMetaData metaData = new MeasureMetaData();
    metaData.setDescription("TestDescription");
    metaData.setCopyright("TestCopyright");
    metaData.setDisclaimer("TestDisclaimer");
    metaData.setRationale("TestRationale");
    metaData.setDraft(true);
    measure.setMeasureMetaData(metaData);
    measure.setMeasurementPeriodStart(new Date("12/02/2020"));
    measure.setMeasurementPeriodEnd(new Date("12/02/2021"));
    Measure originalMeasure =
        measure
            .toBuilder()
            .id("5399aba6e4b0ae375bfdca88")
            .active(true)
            .createdAt(createdAt)
            .createdBy("test.user2")
            .build();

    Instant original = Instant.now().minus(140, ChronoUnit.HOURS);

    Measure m1 =
        originalMeasure
            .toBuilder()
            .createdBy("test.user")
            .createdAt(original)
            .measurementPeriodStart(new Date("12/02/2021"))
            .measurementPeriodEnd(new Date("12/02/2022"))
            .lastModifiedBy("test.user")
            .lastModifiedAt(original)
            .active(false)
            .build();

    doReturn(Optional.of(originalMeasure))
        .when(repository)
        .findById(ArgumentMatchers.eq(originalMeasure.getId()));

    when(measureService.updateMeasure(
            any(Measure.class), anyString(), any(Measure.class), anyString()))
        .thenReturn(m1);

    ResponseEntity<Measure> response =
        controller.updateMeasure(m1.getId(), m1, principal, "Bearer TOKEN");

    assertEquals(m1, response.getBody());
    verify(measureService, times(1))
        .updateMeasure(
            any(Measure.class), anyString(), saveMeasureArgCaptor.capture(), anyString());
    assertThat(saveMeasureArgCaptor.getValue(), is(equalTo(m1)));

    verify(actionLogService, times(1))
        .logAction(
            targetIdArgumentCaptor.capture(),
            targetClassArgumentCaptor.capture(),
            actionTypeArgumentCaptor.capture(),
            performedByArgumentCaptor.capture());
    assertNotNull(targetIdArgumentCaptor.getValue());
    assertThat(targetClassArgumentCaptor.getValue(), is(equalTo(Measure.class)));
    assertThat(actionTypeArgumentCaptor.getValue(), is(equalTo(ActionType.DELETED)));
    assertThat(performedByArgumentCaptor.getValue(), is(equalTo("test.user2")));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForNullId() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    assertThrows(
        InvalidIdException.class,
        () -> controller.updateMeasure(null, measure, principal, "Bearer TOKEN"));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForInvalidCredentials() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("aninvalidUser@gmail.com");
    measure.setCreatedBy("MSR01");
    measure.setActive(true);
    measure.setAcls(null);
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));

    var testMeasure = new Measure();
    testMeasure.setActive(false);
    testMeasure.setCreatedBy("anotheruser");
    testMeasure.setId("testid");
    testMeasure.setMeasureName("MSR01");
    testMeasure.setVersion(new Version(0, 0, 1));

    assertThrows(
        UnauthorizedException.class,
        () -> controller.updateMeasure("testid", testMeasure, principal, "Bearer TOKEN"));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForUpdatingSoftDeletedMeasure() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("validuser@gmail.com");
    measure.setCreatedBy("validuser@gmail.com");
    measure.setActive(false);
    measure.setAcls(null);
    measure.setMeasureMetaData(MeasureMetaData.builder().draft(true).build());
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));

    var testMeasure = new Measure();
    testMeasure.setActive(false);
    testMeasure.setCreatedBy("validuser@gmail.com");
    testMeasure.setId("testid");
    testMeasure.setMeasureName("MSR01");
    testMeasure.setVersion(new Version(0, 0, 1));

    assertThrows(
        UnauthorizedException.class,
        () -> controller.updateMeasure("testid", testMeasure, principal, "Bearer TOKEN"));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForSoftDeletedMeasure() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("validUser@gmail.com");
    measure.setCreatedBy("MSR01");
    measure.setActive(false);
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));

    var testMeasure = new Measure();
    testMeasure.setActive(true);
    testMeasure.setCreatedBy("validUser@gmail.com");
    testMeasure.setId("testid");
    testMeasure.setMeasureName("MSR01");
    testMeasure.setVersion(new Version(0, 0, 1));

    assertThrows(
        UnauthorizedException.class,
        () -> controller.updateMeasure("testid", testMeasure, principal, "Bearer TOKEN"));
  }

  @Test
  void testUpdateMeasureReturnsInvalidDeletionCredentialsException() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("sharedUser@gmail.com");
    measure.setCreatedBy("MSR01");
    measure.setActive(true);
    measure.setMeasureMetaData(MeasureMetaData.builder().draft(true).build());
    AclSpecification acl = new AclSpecification();
    acl.setUserId("sharedUser@gmail.com");
    acl.setRoles(List.of(RoleEnum.SHARED_WITH));
    measure.setAcls(List.of(acl));
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));

    var testMeasure = new Measure();
    testMeasure.setActive(false);
    testMeasure.setCreatedBy("anotheruser");
    testMeasure.setId("testid");
    testMeasure.setMeasureName("MSR01");
    testMeasure.setVersion(new Version(0, 0, 1));
    testMeasure.setActive(false);
    doThrow(new InvalidDeletionCredentialsException("invalidUser@gmail.com"))
        .when(measureService)
        .checkDeletionCredentials(anyString(), anyString());
    assertThrows(
        InvalidDeletionCredentialsException.class,
        () -> controller.updateMeasure("testid", testMeasure, principal, "Bearer TOKEN"));
  }

  @Test
  void testUpdateMeasureReturnsInvalidDraftStatusException() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("sharedUser@gmail.com");
    measure.setCreatedBy("MSR01");
    measure.setActive(true);
    measure.setMeasureMetaData(MeasureMetaData.builder().draft(false).build());
    AclSpecification acl = new AclSpecification();
    acl.setUserId("sharedUser@gmail.com");
    acl.setRoles(List.of(RoleEnum.SHARED_WITH));
    measure.setAcls(List.of(acl));
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));

    var testMeasure = new Measure();
    testMeasure.setActive(false);
    testMeasure.setCreatedBy("anotheruser");
    testMeasure.setId("testid");
    testMeasure.setMeasureName("MSR01");
    testMeasure.setVersion(new Version(0, 0, 1));
    testMeasure.setActive(false);
    assertThrows(
        InvalidDraftStatusException.class,
        () -> controller.updateMeasure("testid", testMeasure, principal, "Bearer TOKEN"));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForEmptyStringId() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    assertThrows(
        InvalidIdException.class,
        () -> controller.updateMeasure("", measure, principal, "Bearer TOKEN"));
  }

  @Test
  void testUpdateMeasureReturnsExceptionForNonMatchingIds() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");
    Measure m1234 = measure.toBuilder().id("ID1234").build();

    assertThrows(
        InvalidIdException.class,
        () -> controller.updateMeasure("ID5678", m1234, principal, "Bearer TOKEN"));
  }

  @Test
  void updateNonExistingMeasure() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    // no measure id specified
    assertThrows(
        InvalidIdException.class,
        () -> controller.updateMeasure(measure.getId(), measure, principal, "Bearer TOKEN"));
    // non-existing measure or measure with fake id
    measure.setId("5399aba6e4b0ae375bfdca88");
    Optional<Measure> empty = Optional.empty();

    doReturn(empty).when(repository).findById(measure.getId());

    assertThrows(
        ResourceNotFoundException.class,
        () -> controller.updateMeasure(measure.getId(), measure, principal, "Bearer TOKEN"));
  }

  @Test
  void updateUnAuthorizedMeasure() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("unAuthorizedUser@gmail.com");
    measure.setCreatedBy("actualOwner@gmail.com");
    measure.setActive(true);
    measure.setMeasurementPeriodStart(new Date());
    measure.setId("testid");
    when(repository.findById(anyString())).thenReturn(Optional.of(measure));

    var testMeasure = new Measure();
    testMeasure.setActive(true);
    testMeasure.setId("testid");
    assertThrows(
        UnauthorizedException.class,
        () -> controller.updateMeasure("testid", testMeasure, principal, "Bearer TOKEN"));
  }

  @Test
  void createGroup() {
    Group group =
        Group.builder()
            .scoring("Cohort")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .build();
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    doReturn(group)
        .when(groupService)
        .createOrUpdateGroup(any(Group.class), any(String.class), any(String.class));

    Group newGroup = new Group();

    ResponseEntity<Group> response = controller.createGroup(newGroup, "measure-id", principal);
    assertNotNull(response.getBody());
    assertEquals(group.getId(), response.getBody().getId());
    assertEquals(group.getScoring(), response.getBody().getScoring());
    assertEquals(group.getPopulations(), response.getBody().getPopulations());
  }

  @Test
  void deleteGroup() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    Measure updatedMeasure =
        Measure.builder().id("measure-id").createdBy("test.user").groups(null).build();
    doReturn(updatedMeasure)
        .when(groupService)
        .deleteMeasureGroup(any(String.class), any(String.class), any(String.class));

    ResponseEntity<Measure> output =
        controller.deleteMeasureGroup("measure-id", "testgroupid", principal);

    assertThat(output.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertNull(output.getBody().getGroups());
  }

  @Test
  void updateGroup() {
    Group group =
        Group.builder()
            .scoring("Cohort")
            .populations(
                List.of(
                    new Population(
                        "id-2",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .build();
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    doReturn(group)
        .when(groupService)
        .createOrUpdateGroup(any(Group.class), any(String.class), any(String.class));

    Group newGroup = new Group();

    ResponseEntity<Group> response = controller.updateGroup(newGroup, "measure-id", principal);
    assertNotNull(response.getBody());
    assertEquals(group.getId(), response.getBody().getId());
    assertEquals(group.getScoring(), response.getBody().getScoring());
    assertEquals(group.getPopulations(), response.getBody().getPopulations());
  }

  @Test
  void searchMeasuresByNameOrEcqmTitleWithoutCurrentUserFilter()
      throws UnsupportedEncodingException {
    Page<Measure> measures = new PageImpl<>(List.of(measure));
    when(repository.findAllByMeasureNameOrEcqmTitle(any(String.class), any(Pageable.class)))
        .thenReturn(measures);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<Page<Measure>> response =
        controller.findAllByMeasureNameOrEcqmTitle(principal, false, "test criteria", 10, 0);
    verify(repository, times(1))
        .findAllByMeasureNameOrEcqmTitle(any(String.class), any(Pageable.class));
    verifyNoMoreInteractions(repository);
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getContent());
    assertNotNull(response.getBody().getContent().get(0));
    assertEquals("IDIDID", response.getBody().getContent().get(0).getMeasureSetId());
  }

  @Test
  void searchMeasuresByNameOrEcqmTitleWithCurrentUserFilter() throws UnsupportedEncodingException {
    Page<Measure> measures = new PageImpl<>(List.of(measure));
    when(repository.findAllByMeasureNameOrEcqmTitleForCurrentUser(
            any(String.class), any(Pageable.class), anyString()))
        .thenReturn(measures);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    ResponseEntity<Page<Measure>> response =
        controller.findAllByMeasureNameOrEcqmTitle(principal, true, "test criteria", 10, 0);
    verify(repository, times(1))
        .findAllByMeasureNameOrEcqmTitleForCurrentUser(
            eq("test criteria"), any(Pageable.class), eq("test.user"));
    verifyNoMoreInteractions(repository);
    assertNotNull(response.getBody().getContent());
    assertNotNull(response.getBody().getContent().get(0));
    assertEquals("IDIDID", response.getBody().getContent().get(0).getMeasureSetId());
  }
}
