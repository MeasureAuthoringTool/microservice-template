package cms.gov.madie.measure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class ElmTranslatorClientConfig {

  @Value("${madie.cql-elm.service.base-url}")
  private String cqlElmServiceBaseUrl;

  @Value("${madie.cql-elm.service.elm-json-uri}")
  private String cqlElmServiceElmJsonUri;

  @Value("${madie.cql-elm.service.elm-json-uri-for-mat-transferred-measure}")
  private String cqlElmServiceUriForMatTransferredMeasure;

  @Bean
  public RestTemplate elmTranslatorRestTemplate() {
    return new RestTemplate();
  }
}
