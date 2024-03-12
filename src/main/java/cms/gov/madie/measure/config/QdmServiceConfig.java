package cms.gov.madie.measure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class QdmServiceConfig {
  @Value("${madie.qdm-service.base-url}")
  private String baseUrl;

  @Value("${madie.qdm-service.create-package-urn}")
  private String createPackageUrn;

  @Value("${madie.qdm-service.create-qrda-urn}")
  private String createQrdaUrn;

  @Bean
  public RestTemplate qdmServiceRestTemplate() {
    return new RestTemplate();
  }
}
