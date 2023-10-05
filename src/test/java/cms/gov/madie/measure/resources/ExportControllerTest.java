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
import gov.cms.madie.models.measure.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
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
  void getTestCaseExportAll() throws IOException {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    List<TestCase> testCases = new ArrayList<>();
    File jsonFile =
        new File(this.getClass().getResource("/test_case_exported_json.json").getFile());

    String jsonData = new String(Files.readAllBytes(jsonFile.toPath()));

    testCases.add(TestCase.builder().json(jsonData).build());
    final Measure measure =
        Measure.builder()
            .ecqmTitle("test_ecqm_title")
            .version(new Version(0, 0, 0))
            .testCases(testCases)
            .model("QiCore 4.1.1")
            .createdBy("test.user")
            .build();
    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(fhirServicesClient.getTestCaseExports(any(Measure.class), anyString(), anyList()))
        .thenReturn(new ResponseEntity<byte[]>(HttpStatus.OK));
    ResponseEntity<byte[]> output =
        exportController.getTestCaseExport(
            principal,
            "access-token",
            "example-measure-id",
            Optional.of("COLLECTION"),
            asList("example-test-case-id-1", "example-test-case-id-2"));
    assertEquals(HttpStatus.OK, output.getStatusCode());
  }

  @Test
  void getTestCaseExportAllPartialContent() {
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
        .thenReturn(new ResponseEntity<byte[]>(HttpStatus.PARTIAL_CONTENT));
    ResponseEntity<byte[]> output =
        exportController.getTestCaseExport(
            principal,
            "access-token",
            "example-measure-id",
            Optional.of("COLLECTION"),
            asList("example-test-case-id-1", "example-test-case-id-2"));
    assertEquals(HttpStatus.PARTIAL_CONTENT, output.getStatusCode());
  }

  @Test
  void getTestCaseExportAllPartialContentWithDefaultBundleType() {
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
        .thenReturn(new ResponseEntity<byte[]>(HttpStatus.PARTIAL_CONTENT));
    ResponseEntity<byte[]> output =
        exportController.getTestCaseExport(
            principal,
            "access-token",
            "example-measure-id",
            Optional.empty(),
            asList("example-test-case-id-1", "example-test-case-id-2"));
    assertEquals(HttpStatus.PARTIAL_CONTENT, output.getStatusCode());
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
                Optional.of("COLLECTION"),
                asList("example-test-case-id-1", "example-test-case-id-2")));
  }
}
