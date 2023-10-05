package cms.gov.madie.measure.services;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cms.gov.madie.measure.config.FhirServicesConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import gov.cms.madie.models.measure.Measure;
import java.net.URI;
import java.util.List;
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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class FhirServicesClientTest {

  private static final String accessToken = "Bearer TOKEN";

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
        .when(fhirServicesConfig.getMadieFhirServiceValidateBundleUri())
        .thenReturn("/api/fhir/validations/bundles");

    when(fhirServicesConfig.fhirServicesRestTemplate()).thenReturn(restTemplate);
  }

  @Test
  void testFhirServicesClientThrowsException() {
    Measure measure = Measure.builder().build();
    when(fhirServicesConfig
            .fhirServicesRestTemplate()
            .exchange(any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
    assertThrows(
        HttpClientErrorException.class,
        () -> fhirServicesClient.getMeasureBundle(measure, accessToken, "calculation"));
    verify(fhirServicesConfig.fhirServicesRestTemplate(), times(1))
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
    final String json = "{\"message\": \"GOOD JSON\"}";
    when(fhirServicesConfig
            .fhirServicesRestTemplate()
            .exchange(any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(json));
    String output = fhirServicesClient.getMeasureBundle(measure, accessToken, "calculation");
    assertThat(output, is(equalTo(json)));
    verify(fhirServicesConfig.fhirServicesRestTemplate(), times(1))
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

    when(fhirServicesConfig
            .fhirServicesRestTemplate()
            .exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
    assertThrows(
        HttpClientErrorException.class,
        () -> fhirServicesClient.validateBundle(testCaseJson, accessToken));
    verify(fhirServicesConfig.fhirServicesRestTemplate(), times(1))
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
  void testValidateBundleReturnsStringData() throws JsonProcessingException {
    final String testCaseJson = "{ \"resourceType\": \"GOOD JSON\" }";
    final HapiOperationOutcome goodOutcome =
        HapiOperationOutcome.builder().code(200).successful(true).build();

    when(fhirServicesConfig
            .fhirServicesRestTemplate()
            .exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(goodOutcome));
    ResponseEntity<HapiOperationOutcome> output =
        fhirServicesClient.validateBundle(testCaseJson, accessToken);
    assertThat(output, is(notNullValue()));
    assertThat(output.getBody(), is(notNullValue()));
    assertThat(output.getBody(), is(equalTo(goodOutcome)));
    verify(fhirServicesConfig.fhirServicesRestTemplate(), times(1))
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
  void testGetTestCaseExports() {
    Measure measure =
        Measure.builder()
            .id("testMeasureId")
            .measureSetId("testMeasureSetId")
            .createdBy("testUser")
            .cql("library Test1CQLLib version '2.3.001'")
            .build();
    when(fhirServicesConfig
            .fhirServicesRestTemplate()
            .exchange(any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(new byte[0]));
    byte[] output =
        fhirServicesClient
            .getTestCaseExports(
                measure, accessToken, asList("test-case-id-1", "test=case=id-2"), "COLLECTION")
            .getBody();
    assertNotNull(output);
  }

  @Test
  void testGetTestCaseExportsException() {
    Measure measure =
        Measure.builder()
            .id("testMeasureId")
            .measureSetId("testMeasureSetId")
            .createdBy("testUser")
            .cql("library Test1CQLLib version '2.3.001'")
            .build();
    when(fhirServicesConfig
            .fhirServicesRestTemplate()
            .exchange(any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenThrow(
            new RestClientResponseException(
                "error occured", HttpStatus.NOT_FOUND, null, null, null, null));
    ResponseEntity<byte[]> output =
        fhirServicesClient.getTestCaseExports(
            measure, accessToken, asList("test-case-id-1", "test=case=id-2"), "COLLECTION");
    assertThat(output.getStatusCode(), is(HttpStatus.NOT_FOUND));
  }
}
