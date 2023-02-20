package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.BundleService;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureScoring;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BundleControllerTest {

  @Mock private MeasureRepository measureRepository;

  @Mock private BundleService bundleService;

  @InjectMocks private BundleController bundleController;

  @Test
  void testGetMeasureBundleForCalculationThrowsNotFoundException() {
    Principal principal = mock(Principal.class);
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () ->
            bundleController.getMeasureBundle(
                "MeasureID", principal, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testGetMeasureBundleForCalculationThrowsAccessException() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final Measure measure = Measure.builder().createdBy("OtherUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        UnauthorizedException.class,
        () ->
            bundleController.getMeasureBundle(
                "MeasureID", principal, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testGetMeasureBundleForCalculationThrowsAccessExceptionForSharedUsers() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user3");
    var acl = new AclSpecification();
    acl.setUserId("test.user2");
    acl.setRoles(List.of(RoleEnum.SHARED_WITH));
    final Measure measure = Measure.builder().createdBy("test.user").acls(List.of(acl)).build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        UnauthorizedException.class,
        () ->
            bundleController.getMeasureBundle(
                "MeasureID", principal, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testGetMeasureBundleForCalculationForSharedUsers() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");
    final String json = "{\"message\": \"GOOD JSON\"}";
    var acl = new AclSpecification();
    acl.setUserId("test.user2");
    acl.setRoles(List.of(RoleEnum.SHARED_WITH));
    final Measure measure = Measure.builder().createdBy("test.user").acls(List.of(acl)).build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(bundleService.getMeasureBundleForCalculation(any(Measure.class), anyString()))
        .thenReturn(json);
    ResponseEntity<String> output =
        bundleController.getMeasureBundle("MeasureID", principal, "Bearer TOKEN", "calculation");
    assertThat(output.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(output.getBody(), is(equalTo(json)));
  }

  @Test
  void testGetMeasureBundleForCalculationThrowsOperationException() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final String elmJson = "{\"text\": \"ELM JSON\"}";
    final Measure measure =
        Measure.builder()
            .createdBy("test.user")
            .groups(
                List.of(
                    Group.builder()
                        .groupDescription("Group1")
                        .scoring(MeasureScoring.RATIO.toString())
                        .build()))
            .elmJson(elmJson)
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(bundleService.getMeasureBundleForCalculation(any(Measure.class), anyString()))
        .thenThrow(
            new BundleOperationException("Measure", "MeasureID", new RuntimeException("cause")));
    assertThrows(
        BundleOperationException.class,
        () ->
            bundleController.getMeasureBundle(
                "MeasureID", principal, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testGetMeasureBundleForCalculationThrowsElmTranslationException() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final String elmJson = "{\"text\": \"ELM JSON\"}";
    final Measure measure =
        Measure.builder()
            .createdBy("test.user")
            .groups(
                List.of(
                    Group.builder()
                        .groupDescription("Group1")
                        .scoring(MeasureScoring.RATIO.toString())
                        .build()))
            .elmJson(elmJson)
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(bundleService.getMeasureBundleForCalculation(any(Measure.class), anyString()))
        .thenThrow(new CqlElmTranslationErrorException(measure.getMeasureName()));
    assertThrows(
        CqlElmTranslationErrorException.class,
        () ->
            bundleController.getMeasureBundle(
                "MeasureID", principal, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testGetMeasureBundleForCalculationReturnsBundleString() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final String elmJson = "{\"text\": \"ELM JSON\"}";
    final String json = "{\"message\": \"GOOD JSON\"}";
    final Measure measure =
        Measure.builder()
            .createdBy("test.user")
            .groups(
                List.of(
                    Group.builder()
                        .groupDescription("Group1")
                        .scoring(MeasureScoring.RATIO.toString())
                        .build()))
            .elmJson(elmJson)
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(bundleService.getMeasureBundleForCalculation(any(Measure.class), anyString()))
        .thenReturn(json);
    ResponseEntity<String> output =
        bundleController.getMeasureBundle("MeasureID", principal, "Bearer TOKEN", "calculation");
    assertThat(output, is(notNullValue()));
    assertThat(output.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(output.getBody(), is(equalTo(json)));
  }

  @Test
  void testGetMeasureBundleForExportThrowsNotFoundException() {
    Principal principal = mock(Principal.class);
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> bundleController.getMeasureBundle("MeasureID", principal, "Bearer TOKEN", "export"));
  }

  @Test
  void testGetMeasureBundleForExportThrowsUnAuthorizedException() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final Measure measure = Measure.builder().createdBy("OtherUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        UnauthorizedException.class,
        () -> bundleController.getMeasureBundle("MeasureID", principal, "Bearer TOKEN", "export"));
  }

  @Test
  void testGetMeasureBundleForExportThrowsAccessExceptionForSharedUsers() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user3");
    var acl = new AclSpecification();
    acl.setUserId("test.user2");
    acl.setRoles(List.of(RoleEnum.SHARED_WITH));
    final Measure measure = Measure.builder().createdBy("test.user").acls(List.of(acl)).build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        UnauthorizedException.class,
        () -> bundleController.getMeasureBundle("MeasureID", principal, "Bearer TOKEN", "export"));
  }

  @Test
  void testGetMeasureBundleForExportReturnsAResponse() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final Measure measure =
        Measure.builder()
            .id("MeasureID")
            .ecqmTitle("test_ecqm_title")
            .version(new Version(0, 0, 0))
            .model("QiCore 4.1.1")
            .createdBy("test.user")
            .build();

    ResponseEntity<String> respone = ResponseEntity.ok("test");
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(bundleService.getMeasureBundleForExport(eq(measure), anyString())).thenReturn(respone);

    ResponseEntity<String> output =
        bundleController.getMeasureBundle("MeasureID", principal, "test_id", "Bearer TOKEN");
    assertEquals(HttpStatus.OK, output.getStatusCode());
  }
}
