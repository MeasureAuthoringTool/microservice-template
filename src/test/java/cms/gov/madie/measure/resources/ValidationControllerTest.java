package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.services.FhirServicesClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationControllerTest {

  @Mock private FhirServicesClient fhirServicesClient;

  @InjectMocks private ValidationController validationController;

  @Captor ArgumentCaptor<String> testCaseJsonCaptor;

  @Captor ArgumentCaptor<String> accessTokenCaptor;

  @Test
  void testValidateBundleProxiesRequest() {
    final String accessToken = "Bearer TOKEN";
    final String testCaseJson = "{ \"resourceType\": \"GOOD JSON\" }";
    HttpHeaders headers = new HttpHeaders();
    final String goodOutcomeJson = "{ \"code\": 200, \"successful\": true }";
    HttpEntity<String> request = new HttpEntity<>(testCaseJson, headers);

    when(fhirServicesClient.validateBundle(anyString(), anyString()))
        .thenReturn(ResponseEntity.ok(goodOutcomeJson));

    ResponseEntity<String> output = validationController.validateBundle(request, accessToken);

    assertThat(output, is(notNullValue()));
    assertThat(output.getBody(), is(notNullValue()));
    assertThat(output.getBody(), is(equalTo(goodOutcomeJson)));
    verify(fhirServicesClient, times(1))
        .validateBundle(testCaseJsonCaptor.capture(), accessTokenCaptor.capture());
    assertThat(testCaseJsonCaptor.getValue(), is(equalTo(testCaseJson)));
    assertThat(accessTokenCaptor.getValue(), is(equalTo(accessToken)));
  }
}
