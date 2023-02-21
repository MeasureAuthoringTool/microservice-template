package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.FhirServicesConfig;
import gov.cms.madie.models.measure.Measure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FhirServicesClientTest {

  @Mock private FhirServicesConfig fhirServicesConfig;

  @Mock private RestTemplate restTemplate;

  @InjectMocks private FhirServicesClient fhirServicesClient;

  @Captor ArgumentCaptor<HttpEntity> httpEntityCaptor;

  @BeforeEach
  void beforeEach() {
    lenient()
        .when(fhirServicesConfig.getMadieFhirServiceBaseUrl())
        .thenReturn("http://fhir-services");
    lenient()
        .when(fhirServicesConfig.getMadieFhirServiceMeasuresBundleUri())
        .thenReturn("/api/fhir/measures/bundles");
    lenient()
        .when(fhirServicesConfig.getMadieFhirServiceSaveMeasureUri())
        .thenReturn("/api/fhir/measures/save");
    lenient()
        .when(fhirServicesConfig.getMadieFhirServiceValidateBundleUri())
        .thenReturn("/api/fhir/validations/bundles");
  }

  @Test
  void testFhirServicesClientThrowsException() {
    Measure measure = Measure.builder().build();
    final String accessToken = "Bearer TOKEN";
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
    assertThrows(
        HttpClientErrorException.class,
        () -> fhirServicesClient.getMeasureBundle(measure, accessToken, "calculation"));
    verify(restTemplate, times(1))
        .exchange(any(URI.class), eq(HttpMethod.PUT), httpEntityCaptor.capture(), any(Class.class));
    HttpEntity httpEntity = httpEntityCaptor.getValue();
    assertThat(httpEntity.getHeaders(), is(notNullValue()));
    List<String> authorization = httpEntity.getHeaders().get(HttpHeaders.AUTHORIZATION);
    assertThat(authorization, is(notNullValue()));
    assertThat(authorization.size(), is(equalTo(1)));
    assertThat(authorization.get(0), is(equalTo(accessToken)));
  }

  @Test
  void testFhirServicesClientReturnsStringData() {
    Measure measure = Measure.builder().build();
    final String accessToken = "Bearer TOKEN";
    final String json = "{\"message\": \"GOOD JSON\"}";
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(json));
    String output = fhirServicesClient.getMeasureBundle(measure, accessToken, "calculation");
    assertThat(output, is(equalTo(json)));
    verify(restTemplate, times(1))
        .exchange(any(URI.class), eq(HttpMethod.PUT), httpEntityCaptor.capture(), any(Class.class));
    HttpEntity httpEntity = httpEntityCaptor.getValue();
    assertThat(httpEntity.getHeaders(), is(notNullValue()));
    List<String> authorization = httpEntity.getHeaders().get(HttpHeaders.AUTHORIZATION);
    assertThat(authorization, is(notNullValue()));
    assertThat(authorization.size(), is(equalTo(1)));
    assertThat(authorization.get(0), is(equalTo(accessToken)));
  }

  @Test
  void testValidateBundleThrowsException() {
    final String testCaseJson = "{ \"resourceType\": \"foo\" }";

    final String accessToken = "Bearer TOKEN";
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
    assertThrows(
        HttpClientErrorException.class,
        () -> fhirServicesClient.validateBundle(testCaseJson, accessToken));
    verify(restTemplate, times(1))
        .exchange(
            any(URI.class), eq(HttpMethod.POST), httpEntityCaptor.capture(), any(Class.class));
    HttpEntity httpEntity = httpEntityCaptor.getValue();
    assertThat(httpEntity.getHeaders(), is(notNullValue()));
    List<String> authorization = httpEntity.getHeaders().get(HttpHeaders.AUTHORIZATION);
    assertThat(authorization, is(notNullValue()));
    assertThat(authorization.size(), is(equalTo(1)));
    assertThat(authorization.get(0), is(equalTo(accessToken)));
  }

  @Test
  void testValidateBundleReturnsStringData() {
    final String accessToken = "Bearer TOKEN";
    final String testCaseJson = "{ \"resourceType\": \"GOOD JSON\" }";
    final String goodOutcomeJson = "{ \"code\": 200, \"successful\": true }";

    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(goodOutcomeJson));
    ResponseEntity<String> output = fhirServicesClient.validateBundle(testCaseJson, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getBody(), is(notNullValue()));
    assertThat(output.getBody(), is(equalTo(goodOutcomeJson)));
    verify(restTemplate, times(1))
        .exchange(
            any(URI.class), eq(HttpMethod.POST), httpEntityCaptor.capture(), any(Class.class));
    HttpEntity httpEntity = httpEntityCaptor.getValue();
    assertThat(httpEntity.getHeaders(), is(notNullValue()));
    List<String> authorization = httpEntity.getHeaders().get(HttpHeaders.AUTHORIZATION);
    assertThat(authorization, is(notNullValue()));
    assertThat(authorization.size(), is(equalTo(1)));
    assertThat(authorization.get(0), is(equalTo(accessToken)));
  }

  @Test
  void testSaveMeasureInHapiFhirsStringData() {
    final String accessToken = "Bearer TOKEN";
    final String goodOutcomeJson = "{ \"code\": 200, \"successful\": true }";
    Measure measure =
        Measure.builder()
            .id("testMeasureId")
            .measureSetId("testMeasureSetId")
            .createdBy("testUser")
            .cql("library Test1CQLLib version '2.3.001'")
            .build();

    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(goodOutcomeJson));
    ResponseEntity<String> output = fhirServicesClient.saveMeasureInHapiFhir(measure, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getBody(), is(notNullValue()));
    assertThat(output.getBody(), is(equalTo(goodOutcomeJson)));
    verify(restTemplate, times(1))
        .exchange(
            any(URI.class), eq(HttpMethod.POST), httpEntityCaptor.capture(), any(Class.class));
    HttpEntity httpEntity = httpEntityCaptor.getValue();
    assertThat(httpEntity.getHeaders(), is(notNullValue()));
    List<String> authorization = httpEntity.getHeaders().get(HttpHeaders.AUTHORIZATION);
    assertThat(authorization, is(notNullValue()));
    assertThat(authorization.size(), is(equalTo(1)));
    assertThat(authorization.get(0), is(equalTo(accessToken)));
  }
}
