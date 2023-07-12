package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.BundleService;
import cms.gov.madie.measure.services.FhirServicesClient;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.common.Version;
import java.io.OutputStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.reporting.ReportEntry;
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

  @Mock private BundleService bundleService;

  @Mock private FhirServicesClient fhirServicesClient;

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
  void getZipReturnsAResponse() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final Measure measure =
        Measure.builder()
            .ecqmTitle("test_ecqm_title")
            .version(new Version(0, 0, 0))
            .model("QiCore 4.1.1")
            .createdBy("test.user")
            .build();

    byte[] response = new byte[0];
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(bundleService.exportBundleMeasure(eq(measure), anyString())).thenReturn(response);
    ResponseEntity<byte[]> output = exportController.getZip(principal, "test_id", "Bearer TOKEN");
    assertEquals(HttpStatus.OK, output.getStatusCode());
  }

  @Test
  void getTestCaseExport() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    final Measure measure =
        Measure.builder()
            .ecqmTitle("test_ecqm_title")
            .version(new Version(0, 0, 0))
            .model("QiCore 4.1.1")
            .createdBy("test.user")
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(fhirServicesClient.getTestCaseExport(any(Measure.class), anyString(), anyString())).thenReturn(new byte[0]);
    ResponseEntity<byte[]> output = exportController.getTestCaseExport(
        principal, "access-token", "example-measure-id", "example-test-case-id");
    assertEquals(HttpStatus.OK, output.getStatusCode());
  }
}
