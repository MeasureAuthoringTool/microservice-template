package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import cms.gov.madie.measure.config.ValidationServiceClientConfig;
import cms.gov.madie.measure.exceptions.ValidationServiceException;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;

@ExtendWith(MockitoExtension.class)
public class ValidationServiceClientTest {

  @Mock private ValidationServiceClientConfig validationServiceClientConfig;
  @Mock private RestTemplate restTemplate;

  @InjectMocks private ValidationServiceClient validationServiceClient;

  private Measure measure;

  @BeforeEach
  public void beforeEach() {
    lenient()
        .when(validationServiceClientConfig.getMadieValidationServiceBaseUrl())
        .thenReturn("http://test");
    lenient()
        .when(validationServiceClientConfig.getMadieValidateReturnTypesAndObservation())
        .thenReturn("/validateReturnTypes");
    measure = Measure.builder().id("testMeasureId").model(ModelType.QI_CORE.getValue()).build();
  }

  @Test
  public void testRestTemplateHandlesClientErrorException() {
    String errorBody =
        "{\"timestamp\":\"2023-06-21T20:21:29.658+00:00\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Return type for the CQL definition selected for the Initial Population does not match with population basis.\"}";
    HttpClientErrorException exception =
        HttpClientErrorException.create(
            "\"message\":\"Invalid return type\"",
            HttpStatusCode.valueOf(400),
            "Bad Request",
            null,
            errorBody.getBytes(),
            null);
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
        .thenThrow(exception);
    assertThrows(
        ValidationServiceException.class,
        () -> validationServiceClient.validateReturnTypesAndObservation(measure, "TEST_TOKEN"));
  }

  @Test
  public void testValidateReturnTypesAndObservationThrowsJsonProcessingException() {
    String errorBody = "This is not valid response from validation service";
    HttpClientErrorException exception =
        HttpClientErrorException.create(
            "\"message\":\"Invalid\"",
            HttpStatusCode.valueOf(400),
            "Bad Request",
            null,
            errorBody.getBytes(),
            null);
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
        .thenThrow(exception);
    assertThrows(
        ValidationServiceException.class,
        () -> validationServiceClient.validateReturnTypesAndObservation(measure, "TEST_TOKEN"));
  }

  @Test
  public void testValidateReturnTypesAndObservationSuccess() {
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(measure));

    Measure validateMeasure =
        validationServiceClient.validateReturnTypesAndObservation(measure, "TEST_TOKEN");

    assertNotNull(validateMeasure);
  }

  @Test
  public void testGetMeasureHttpEntityNoAccessToken() {
    HttpEntity<Measure> measureEntity = validationServiceClient.getMeasureHttpEntity(measure, null);
    assertNotNull(measureEntity.getHeaders());
    assertNull(measureEntity.getHeaders().get("Authorization"));
  }
}
