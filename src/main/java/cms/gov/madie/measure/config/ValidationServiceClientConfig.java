package cms.gov.madie.measure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;

@Getter
@Configuration
public class ValidationServiceClientConfig {

  @Value("${madie.validation.service.base-url}")
  private String madieValidationServiceBaseUrl;

  @Value("${madie.validation.service.return-types}")
  private String madieValidateReturnTypesAndObservation;

  @Bean
  public RestTemplate validationServiceRestTemplate() {
    return new RestTemplate();
  }
}
