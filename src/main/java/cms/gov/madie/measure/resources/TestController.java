package cms.gov.madie.measure.resources;


import cms.gov.madie.measure.HapiFhirConfig;
import cms.gov.madie.measure.models.HapiOperationOutcome;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.services.TestCaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RestController
public class TestController {

  private TestCaseService testCaseService;
  private HapiFhirConfig hapiFhirConfig;

  @Autowired
  public TestController(TestCaseService testCaseService, HapiFhirConfig hapiFhirConfig) {
    this.testCaseService = testCaseService;
    this.hapiFhirConfig = hapiFhirConfig;
  }

  @PostMapping(value="/plain/patients", produces = MediaType.APPLICATION_JSON_VALUE)
  public TestCase create(@RequestBody TestCase testCase) {
    return testCaseService.upsertFhirPatient(testCase);
  }


  @PutMapping(value="/plain/patients/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public TestCase doThings(@PathVariable("id") String id, @RequestBody TestCase testCase) throws JsonProcessingException {
    log.info("doing things here");
    log.info("id [{}], testCaseId: [{}]", id, testCase.getId());
    if (id == null || !id.equals(testCase.getId())) {
      throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Resource path ID must be valid and must match resource body ID");
    }


    ObjectMapper mapper = new ObjectMapper();
    RestTemplate restTemplate = new RestTemplate();
    String json = testCase.getJson();
    if (json != null && !json.isEmpty()) {
      ObjectNode node = (ObjectNode) mapper.readTree(json);
      node.put("id", id);
      final String url = hapiFhirConfig.getHapiFhirUrl() + "/Patient/" + id;
      log.info("PUT to url [{}]", url);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
      HttpEntity<String> entity = new HttpEntity<>(node.toPrettyString(), headers);
      try {
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        log.info("got response: {}", response);
        testCase.setJson(response.getBody());
        testCase.setHapiOperationOutcome(HapiOperationOutcome.builder().code(200).build());
        return testCase;
      } catch (HttpClientErrorException ex) {
        Map<String, Object> map = mapper.readValue(ex.getResponseBodyAsString(), Map.class);
        log.info("map: {}", map);
        testCase.setHapiOperationOutcome(HapiOperationOutcome.builder()
            .code(ex.getRawStatusCode())
            .message("Unable to persist to HAPI FHIR due to errors")
            .outcomeResponse(map)
            .build());
        return testCase;
      } catch (Exception ex) {
        log.error("Exception occurred invoking PUT on HAPI FHIR:", ex);
        testCase.setHapiOperationOutcome(
            HapiOperationOutcome.builder()
                .code(500)
                .message("An unknown exception occurred with the HAPI FHIR server")
                .build());
        return testCase;
      }
    }
    return testCase;
  }

}
