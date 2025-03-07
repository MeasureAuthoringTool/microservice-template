package cms.gov.madie.measure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class ElmTranslatorClientConfig {

  @Value("${madie.cql-elm.service.qdm-base-url}")
  private String qdmCqlElmServiceBaseUrl;

  @Value("${madie.cql-elm.service.fhir-base-url}")
  private String fhirCqlElmServiceBaseUrl;

  @Value("${madie.cql-elm.service.elm-json-uri}")
  private String cqlElmServiceElmJsonUri;

  @Bean
  public RestTemplate elmTranslatorRestTemplate() {
    return new RestTemplate();
  }
}
