package cms.gov.madie.measure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class TerminologyServiceConfig {

  @Value("${madie.terminology.service.base-url}")
  private String baseUrl;

  @Value("${madie.terminology.service.fetch-value-sets}")
  private String valueSetsUrl;

  @Value("${madie.terminology.service.validate-codes}")
  private String validateCodeUrl;

  @Bean
  public RestTemplate terminologyRestTemplate() {
    return new RestTemplate();
  }
}
