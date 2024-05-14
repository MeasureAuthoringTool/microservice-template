package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.dto.QrdaRequestDTO;
import gov.cms.madie.models.measure.Measure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QicorePackageServiceTest {
  @Mock private BundleService bundleService;
  @InjectMocks private QicorePackageService qicorePackageService;

  @Test
  void getMeasurePackage() {
    String measurePackageStr = "measure package";
    PackageDto packageDto =
        PackageDto.builder().fromStorage(false).exportPackage(measurePackageStr.getBytes()).build();
    when(bundleService.getMeasureExport(any(Measure.class), anyString())).thenReturn(packageDto);
    PackageDto measurePackage = qicorePackageService.getMeasurePackage(new Measure(), "token");
    byte[] rawPackage = measurePackage.getExportPackage();
    assertThat(new String(rawPackage), is(equalTo(measurePackageStr)));
  }

  @Test
  void testGetQRDA() {
    Exception ex =
        assertThrows(
            UnsupportedOperationException.class,
            () ->
                qicorePackageService.getQRDA(
                    QrdaRequestDTO.builder().measure(new Measure()).coveragePercentage("").build(),
                    "token"));
    assertThat(ex.getMessage(), containsString("method not yet implemented"));
  }
}
