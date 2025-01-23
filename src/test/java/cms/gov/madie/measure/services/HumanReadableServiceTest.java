package cms.gov.madie.measure.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import cms.gov.madie.measure.factories.PackageServiceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnsupportedTypeException;
import gov.cms.madie.models.measure.Measure;

@ExtendWith(MockitoExtension.class)
public class HumanReadableServiceTest {
  @Mock MeasureService measureService;
  @Mock private PackageServiceFactory packageServiceFactory;
  @Mock private QicorePackageService qicorePackageService;
  @Mock private QdmPackageService qdmPackageService;
  @InjectMocks HumanReadableService humanReadableService;

  private static final String TEST_USER = "test-user";
  private static final String TEST_ACCESS_TOKEN = "test-access-token";
  private static final String TEST_MEASURE_ID = "testMeasureId";

  @Test
  public void testGetHumanReadableWithCSSThrowsResourceNotFoundException() {
    when(measureService.findMeasureById(anyString())).thenReturn(null);

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            humanReadableService.getHumanReadableWithCSS(
                TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN));
  }

  @Test
  public void testGetHumanReadableWithCSSThrowsUnsupportedTypeException() {
    Measure existingMeasure = Measure.builder().id(TEST_MEASURE_ID).model("invalid model").build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);
    when(packageServiceFactory.getPackageService(any())).thenThrow(UnsupportedTypeException.class);

    assertThrows(
        UnsupportedTypeException.class,
        () ->
            humanReadableService.getHumanReadableWithCSS(
                TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN));
  }

  @Test
  void testGetQdmMeasurePackage() {
    Measure existingMeasure = Measure.builder().id(TEST_MEASURE_ID).model("QDM v5.6").build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);
    when(packageServiceFactory.getPackageService(any())).thenReturn(qdmPackageService);
    when(qdmPackageService.getHumanReadable(any(Measure.class), anyString(), anyString()))
        .thenReturn("valid QDM Human Readable");
    String output =
        humanReadableService.getHumanReadableWithCSS(TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN);
    assertThat(output, is(equalTo("valid QDM Human Readable")));
  }

  @Test
  void testGetQiCoreMeasurePackage() {
    Measure existingMeasure = Measure.builder().id(TEST_MEASURE_ID).model("QI-Core v4.1.1").build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);
    when(packageServiceFactory.getPackageService(any())).thenReturn(qicorePackageService);
    when(qicorePackageService.getHumanReadable(any(Measure.class), anyString(), anyString()))
        .thenReturn("valid QICore Human Readable");
    String output =
        humanReadableService.getHumanReadableWithCSS(TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN);
    assertThat(output, is(equalTo("valid QICore Human Readable")));
  }
}
