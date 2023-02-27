package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.TerminologyServiceConfig;
import cms.gov.madie.measure.dto.ValueSetsSearchCriteria;
import cms.gov.madie.measure.exceptions.InvalidTerminologyException;
import gov.cms.madie.models.cql.terminology.CqlCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Slf4j
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
    try {
      return terminologyRestTemplate
          .exchange(
              new RequestEntity(searchCriteria, headers, HttpMethod.PUT, URI.create(url)),
              String.class)
          .getBody();
    } catch (HttpClientErrorException ex) {
      log.error("An issue occurred while validating ValueSets: " + ex.getMessage());
      throw new InvalidTerminologyException("Invalid ValueSet: ", ex.getMessage());
    }
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
