package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.TerminologyServiceConfig;
import cms.gov.madie.measure.dto.ValueSetsSearchCriteria;
import gov.cms.madie.models.cql.terminology.CqlCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TerminologyServiceClient {
  private final TerminologyServiceConfig terminologyServiceConfig;
  private final RestTemplate terminologyRestTemplate;

  public String fetchValueSets(ValueSetsSearchCriteria searchCriteria, String accessToken) {
    final String url =
        terminologyServiceConfig.getBaseUrl() + terminologyServiceConfig.getValueSetsUrl();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE));
    headers.set(HttpHeaders.AUTHORIZATION, accessToken);
    ResponseEntity<String> response =
        terminologyRestTemplate.exchange(
            new RequestEntity(searchCriteria, headers, HttpMethod.PUT, URI.create(url)),
            String.class);
    return response.getBody();
  }

  public List<CqlCode> validateCodes(List<CqlCode> cqlCodes, String accessToken) {
    final String url =
        terminologyServiceConfig.getBaseUrl() + terminologyServiceConfig.getValidateCodeUrl();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE));
    headers.set(HttpHeaders.AUTHORIZATION, accessToken);
    ResponseEntity<List<CqlCode>> response =
        terminologyRestTemplate.exchange(
            new RequestEntity(cqlCodes, headers, HttpMethod.PUT, URI.create(url)),
            new ParameterizedTypeReference<>() {});
    return response.getBody();
  }
}
