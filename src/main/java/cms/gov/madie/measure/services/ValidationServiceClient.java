package cms.gov.madie.measure.services;

import java.net.URI;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import cms.gov.madie.measure.config.ValidationServiceClientConfig;
import cms.gov.madie.measure.exceptions.ValidationServiceException;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class ValidationServiceClient {

  private ValidationServiceClientConfig validationServiceClientConfig;
  private RestTemplate validationServiceRestTemplate;
  private final JsonMapper jsonMapper = new JsonMapper();

  public Measure validateReturnTypesAndObservation(final Measure measure, String accessToken) {

    URI uri = getReturnTypeValidationURI();
    HttpEntity<Measure> measureEntity = getMeasureHttpEntity(measure, accessToken);
    Measure validateMeasure = null;
    try {
      validateMeasure =
          validationServiceRestTemplate
              .exchange(uri, HttpMethod.POST, measureEntity, Measure.class)
              .getBody();
    } catch (HttpClientErrorException e) {
      String errorBody = e.getResponseBodyAsString();
      log.error("validateReturnTypes(): errorBody = " + errorBody);
      try {
        JsonNode node = jsonMapper.readTree(errorBody);
        JsonNode message = node.get("message");
        throw new ValidationServiceException(message.asText());
      } catch (JsonProcessingException ex) {
        log.error(
            "An error occurred while calling validation service " + "for measure {}",
            measure.getId(),
            ex);
        throw new ValidationServiceException(
            "An error occurred while calling validation service for measure {}" + measure.getId());
      }
    }
    return validateMeasure;
  }

  protected URI getReturnTypeValidationURI() {
    return URI.create(
        validationServiceClientConfig.getMadieValidationServiceBaseUrl()
            + validationServiceClientConfig.getMadieValidateReturnTypesAndObservation());
  }

  protected HttpEntity<Measure> getMeasureHttpEntity(final Measure measure, String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (accessToken != null) {
      headers.set(HttpHeaders.AUTHORIZATION, accessToken);
    }
    return new HttpEntity<>(measure, headers);
  }
}
