package cms.gov.madie.measure;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HapiFhirConfig {
  @Getter
  @Value("${hapi-fhir.url}")
  private String hapiFhirUrl;

  /**
   * Create a FHIR Context for R4. According to the HAPI FHIR documentation this is an expensive operation
   * so the context should be created once and reused for the lifetime of the application.
   * @return
   */
  @Bean
  public FhirContext fhirR4Context() {
    return FhirContext.forR4();
  }

  @Bean
  public IGenericClient hapiFhirR4Client(@Autowired FhirContext fhirR4Context) {
    return fhirR4Context.newRestfulGenericClient(hapiFhirUrl);
  }
}
