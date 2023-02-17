package cms.gov.madie.measure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class FhirServicesConfig {

  @Value("${madie.fhir.service.base-url}")
  private String madieFhirServiceBaseUrl;

  @Value("${madie.fhir.service.hapi-fhir.measures.export-uri}")
  private String madieFhirServiceMeasureseExportUri;

  @Value("${madie.fhir.service.hapi-fhir.measures.bundle-uri}")
  private String madieFhirServiceMeasuresBundleUri;

  @Value("${madie.fhir.service.hapi-fhir.validation.bundle-uri}")
  private String madieFhirServiceValidateBundleUri;

  @Value("${madie.fhir.service.hapi-fhir.measures.save-measure-uri}")
  private String madieFhirServiceSaveMeasureUri;

  @Bean
  public RestTemplate fhirServicesRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
    return restTemplate;
  }

}
