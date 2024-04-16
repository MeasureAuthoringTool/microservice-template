package cms.gov.madie.measure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class VirusScanConfig {

  @Value("${madie.virus-scan.service.base-url}")
  private String baseUrl;

  @Value("${madie.virus-scan.service.scan-file-uri}")
  private String scanFileUri;

  @Value("${madie.virus-scan.service.api-key}")
  private String apiKey;

  @Value("${madie.virus-scan.disable-scan}")
  private boolean scanDisabled;

  @Bean
  public RestTemplate virusScanRestTemplate() {
    return new RestTemplate();
  }
}
