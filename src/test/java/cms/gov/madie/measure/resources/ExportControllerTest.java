package cms.gov.madie.measure.resources;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.BundleService;
import cms.gov.madie.measure.services.FhirServicesClient;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
  void getTestCaseExportAll() {
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
    when(fhirServicesClient.getTestCaseExports(any(Measure.class), anyString(), anyList()))
        .thenReturn(new byte[0]);
    ResponseEntity<byte[]> output =
        exportController.getTestCaseExport(
            principal,
            "access-token",
            "example-measure-id",
            asList("example-test-case-id-1", "example-test-case-id-2"));
    assertEquals(HttpStatus.OK, output.getStatusCode());
  }

  @Test
  void getTestCaseExportAllThrowsResourceNotFoundException() {
    Principal principal = mock(Principal.class);
    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () ->
            exportController.getTestCaseExport(
                principal,
                "access-token",
                "example-measure-id",
                asList("example-test-case-id-1", "example-test-case-id-2")));
  }
}
