package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Measure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BundleServiceTest {

  @Mock
  private FhirServicesClient fhirServicesClient;

  @Mock private ElmTranslatorClient elmTranslatorClient;

  @InjectMocks
  private BundleService bundleService;

  @Test
  void testBundleMeasureReturnsNullForNullMeasure() {
    String output = bundleService.bundleMeasure(null, "Bearer TOKEN");
    assertThat(output, is(nullValue()));
  }

  @Test
  void testBundleMeasureThrowsOperationException() {
    final Measure measure = Measure.builder().createdBy("test.user").cql("CQL").build();
    when(elmTranslatorClient.getElmJson(anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    when(fhirServicesClient.getMeasureBundle(any(Measure.class), anyString()))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
    assertThrows(
        BundleOperationException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN"));
  }

  @Test
  void testBundleMeasureThrowsCqlElmTranslatorExceptionWithErrors() {
    final Measure measure = Measure.builder().createdBy("test.user").cql("CQL").build();
    when(elmTranslatorClient.getElmJson(anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    when(elmTranslatorClient.hasErrors(any(ElmJson.class))).thenReturn(true);
    assertThrows(
        CqlElmTranslationErrorException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN"));
  }

  @Test
  void testBundleMeasureReturnsBundleString() {
    final String json = "{\"message\": \"GOOD JSON\"}";
    final Measure measure = Measure.builder().createdBy("test.user").cql("CQL").build();
    when(fhirServicesClient.getMeasureBundle(any(Measure.class), anyString())).thenReturn(json);
    when(elmTranslatorClient.getElmJson(anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    String output = bundleService.bundleMeasure(measure, "Bearer TOKEN");
    assertThat(output, is(equalTo(json)));
  }

}