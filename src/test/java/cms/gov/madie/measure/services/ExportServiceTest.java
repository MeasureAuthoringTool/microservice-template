package cms.gov.madie.measure.services;

import ca.uhn.fhir.context.FhirContext;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.measure.Measure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.time.Instant;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest implements ResourceUtil {

  @Mock private BundleService bundleService;
  @Mock private FhirContext fhirContext;

  @InjectMocks private ExportService exportService;

  private Measure measure;
  private String measureBundle;

  @BeforeEach
  public void setUp() {
    measure =
        Measure.builder()
            .active(true)
            .ecqmTitle("test-ecqm-title")
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
  void testGenerateExportsForMeasure() {
    when(bundleService.bundleMeasure(any(), anyString())).thenReturn(measureBundle);
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());
    exportService.generateExports(measure, "Bearer TOKEN", OutputStream.nullOutputStream());
    verify(bundleService, times(1)).bundleMeasure(any(), anyString());
  }
}
