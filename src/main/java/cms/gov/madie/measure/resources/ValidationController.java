package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.services.FhirServicesClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/validations")
@AllArgsConstructor
public class ValidationController {

  private FhirServicesClient fhirServicesClient;

  @PostMapping(
      path = "/bundles",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> validateBundle(
      HttpEntity<String> request, @RequestHeader("Authorization") String accessToken) {
    return fhirServicesClient.validateBundle(request.getBody(), accessToken);
  }
}
