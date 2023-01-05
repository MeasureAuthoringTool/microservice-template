package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.Measure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

  @Mock private MeasureRepository measureRepository;

  @InjectMocks private ExportController exportController;

  @Test
  void getZipThrowsNotFoundException() {
    Principal principal = mock(Principal.class);
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> exportController.getZip(principal, "test_id", "Bearer TOKEN"));
  }

  @Test
  void getZipThrowsUnAuthorizedException() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final Measure measure = Measure.builder().createdBy("OtherUser").build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        UnauthorizedException.class,
        () -> exportController.getZip(principal, "test_id", "Bearer TOKEN"));
  }

  @Test
  void getZipThrowsAccessExceptionForSharedUsers() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user3");
    var acl = new AclSpecification();
    acl.setUserId("test.user2");
    acl.setRoles(List.of(RoleEnum.SHARED_WITH));
    final Measure measure = Measure.builder().createdBy("test.user").acls(List.of(acl)).build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    assertThrows(
        UnauthorizedException.class,
        () -> exportController.getZip(principal, "test_id", "Bearer TOKEN"));
  }

  @Test
  void getZipReturnsAResponse() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final Measure measure =
        Measure.builder()
            .ecqmTitle("test_ecqm_title")
            .version("0.0.000")
            .model("QiCore 4.1.1")
            .createdBy("test.user")
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    ResponseEntity<StreamingResponseBody> output =
        exportController.getZip(principal, "test_id", "Bearer TOKEN");
    assertEquals(HttpStatus.OK, output.getStatusCode());
    String zipFileName = output.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0);
    assertEquals("attachment;filename=\"test_ecqm_title-v0.0.000-QiCore 4.1.1.zip\"", zipFileName);
  }
}
