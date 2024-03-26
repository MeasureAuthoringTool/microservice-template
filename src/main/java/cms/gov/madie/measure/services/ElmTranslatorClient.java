package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.ElmTranslatorClientConfig;
import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import gov.cms.madie.models.measure.ElmJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Iterator;

@Slf4j
@Service
@AllArgsConstructor
public class ElmTranslatorClient {

  private ElmTranslatorClientConfig elmTranslatorClientConfig;
  private RestTemplate elmTranslatorRestTemplate;

  public ElmJson getElmJson(final String cql, String accessToken) {
    try {
      URI uri = getElmJsonURI(false);
      HttpEntity<String> cqlEntity = getCqlHttpEntity(cql, accessToken, null, null);
      return elmTranslatorRestTemplate
          .exchange(uri, HttpMethod.PUT, cqlEntity, ElmJson.class)
          .getBody();
    } catch (Exception ex) {
      log.error("An error occurred calling the CQL to ELM translation service", ex);
      throw new CqlElmTranslationServiceException(
          "There was an error calling CQL-ELM translation service", ex);
    }
  }

  public boolean hasErrors(ElmJson elmJson) {
    if (elmJson == null) {
      return true;
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonNode = mapper.readTree(elmJson.getJson());
      boolean parsingErrors = false;
      if (jsonNode.has("errorExceptions")) {
        JsonNode errorExceptionsNode = jsonNode.get("errorExceptions");
        Iterator<JsonNode> entyIter = errorExceptionsNode.iterator();
        while (entyIter.hasNext()) {
          JsonNode errorNode = entyIter.next();
          if (errorNode.get("type") != null
              && errorNode.get("type").asText().equalsIgnoreCase("parsing")) {
            System.out.println(
                "hasErrors(): errorExceptions = " + errorNode.toPrettyString() + "\n\n");
            parsingErrors = true;
            break;
          }
        }
      }
      return (jsonNode.has("externalErrors") && jsonNode.get("externalErrors").size() > 0)
          || parsingErrors;
    } catch (Exception ex) {
      log.error("An error occurred parsing the response from the CQL-ELM translation service", ex);
      throw new CqlElmTranslationServiceException(
          "There was an error calling CQL-ELM translation service", ex);
    }
  }

  public ElmJson getElmJsonForMatMeasure(final String cql, String apiKey, String harpId) {
    try {
      URI uri = getElmJsonURI(true);
      HttpEntity<String> cqlEntity = getCqlHttpEntity(cql, null, apiKey, harpId);

      return elmTranslatorRestTemplate
          .exchange(uri, HttpMethod.PUT, cqlEntity, ElmJson.class)
          .getBody();
    } catch (Exception ex) {
      throw new CqlElmTranslationServiceException(
          "There was an error calling CQL-ELM translation service for MAT transferred measure", ex);
    }
  }

  protected URI getElmJsonURI(boolean isForMatTransferred) {
    return URI.create(
        elmTranslatorClientConfig.getCqlElmServiceBaseUrl()
            + (isForMatTransferred
                ? elmTranslatorClientConfig.getCqlElmServiceUriForMatTransferredMeasure()
                : elmTranslatorClientConfig.getCqlElmServiceElmJsonUri()));
  }

  protected HttpEntity<String> getCqlHttpEntity(
      final String cql, String accessToken, String apiKey, String harpId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    if (accessToken != null) {
      headers.set(HttpHeaders.AUTHORIZATION, accessToken);
    } else if (apiKey != null && harpId != null) {
      headers.set("api-key", apiKey);
      headers.set("harp-id", harpId);
    }
    return new HttpEntity<>(cql, headers);
  }
}
