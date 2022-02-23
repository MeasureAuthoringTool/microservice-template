package cms.gov.madie.measure;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HapiFhirConfig {
  @Getter
  @Value("${hapi-fhir.url}")
  private String hapiFhirUrl;

  @Getter
  @Value("${hapi-fhir.patient.uri")
  private String hapiFhirPatientUri;

  @Bean(name = "hapiFhirRestTemplate")
  public RestTemplate restTemplate() {
    // ToDo: add configuration like certs for HAPI FHIR
    return new RestTemplate();
  }
}
