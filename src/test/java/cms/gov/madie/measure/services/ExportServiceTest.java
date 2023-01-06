package cms.gov.madie.measure.services;

import ca.uhn.fhir.context.FhirContext;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.measure.Measure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest implements ResourceUtil {

  @Mock private BundleService bundleService;
  @Mock private FhirContext fhirContext;

  @Spy
  @InjectMocks private ExportService exportService;

  private Measure measure;
  private String measureBundle;

  @BeforeEach
  public void setUp() {
    measure =
        Measure.builder()
            .active(true)
            .ecqmTitle("ExportTest")
            .id("xyz-p13r-13ert")
            .cql("test cql")
            .cqlErrors(false)
            .measureSetId("IDIDID")
            .measureName("MSR01")
            .version("1.0.000")
            .createdAt(Instant.now())
            .createdBy("test user")
            .lastModifiedAt(Instant.now())
            .lastModifiedBy("test user")
            .model("QI-Core v4.1.1")
            .build();
    measureBundle = getData("/bundles/export_test.json");
  }

  @Test
  void testGenerateExportsForMeasure() throws IOException {
    when(bundleService.bundleMeasure(any(), anyString())).thenReturn(measureBundle);
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    exportService.generateExports(measure, "Bearer TOKEN", out);
    verify(bundleService, times(1)).bundleMeasure(any(), anyString());

    // expected files in export zip
    List<String> expectedFilesInZip = List.of(
      "ExportTest-v1.0.000-QI-Core v4.1.1.json",
      "/cql/ExportTest0.0.000.cql",
      "/cql/FHIRHelpers4.1.000.cql"
    );

    ZipInputStream zipInputStream = new ZipInputStream( new ByteArrayInputStream( out.toByteArray() ) );
    List<String> actualFilesInZip = getFilesInZip(zipInputStream);
    assertThat(expectedFilesInZip.size(), is(equalTo(actualFilesInZip.size())));
    assertThat(expectedFilesInZip, is(equalTo(actualFilesInZip)));
  }

  @Test
  void testGenerateExportsWhenWritingFileToZipFailed() throws IOException {
    when(bundleService.bundleMeasure(any(), anyString())).thenReturn(measureBundle);

    doThrow(new IOException()).when(exportService).
      addMeasureBundleToExport(any(ZipOutputStream.class), anyString(), anyString());
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());

    assertThrows(
      RuntimeException.class,
      () -> exportService.generateExports(measure, "Bearer TOKEN", OutputStream.nullOutputStream()),
      "Unexpected error while generating exports for measureID: " + measure.getId());
  }

  private List<String> getFilesInZip(ZipInputStream zipInputStream) throws IOException {
    ZipEntry entry;
    List<String> actualFilesInZip = new ArrayList<>();
    while ((entry = zipInputStream.getNextEntry()) != null) {
      actualFilesInZip.add(entry.getName());
    }
    return actualFilesInZip;
  }
}
