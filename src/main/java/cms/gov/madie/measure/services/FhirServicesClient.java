package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.EnvironmentConfig;
import cms.gov.madie.measure.models.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Service
@AllArgsConstructor
public class FhirServicesClient {

  private EnvironmentConfig environmentConfig;
  private RestTemplate restTemplate;

  public String getMeasureBundle(Measure measure, String accessToken) {
    URI uri =
        URI.create(
            environmentConfig.getMadieFhirServiceBaseUrl()
                + environmentConfig.getMadieFhirServiceMeasuresBundleUri());
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, accessToken);
    HttpEntity<Measure> measureEntity = new HttpEntity<>(measure, headers);
    return restTemplate.exchange(uri, HttpMethod.PUT, measureEntity, String.class).getBody();
  }
}
