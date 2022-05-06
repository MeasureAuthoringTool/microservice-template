package cms.gov.madie.measure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class EnvironmentConfig {

  @Value("${madie.fhir.service.base-url}")
  private String madieFhirServiceBaseUrl;

  @Value("${madie.fhir.service.hapi-fhir.measures.bundle-uri}")
  private String madieFhirServiceMeasuresBundleUri;
}
