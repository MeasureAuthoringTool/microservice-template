package cms.gov.madie.measure.services;

import cms.gov.madie.measure.HapiFhirConfig;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.HapiOperationOutcome;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.repositories.MeasureRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestCaseService {

  private final MeasureRepository measureRepository;
  private HapiFhirConfig hapiFhirConfig;
  private RestTemplate hapiFhirRestTemplate;

  @Autowired
  public TestCaseService(
      MeasureRepository measureRepository,
      HapiFhirConfig hapiFhirConfig,
      @Qualifier("hapiFhirRestTemplate") RestTemplate hapiFhirRestTemplate) {
    this.measureRepository = measureRepository;
    this.hapiFhirConfig = hapiFhirConfig;
    this.hapiFhirRestTemplate = hapiFhirRestTemplate;
  }

  public TestCase persistTestCase(TestCase testCase, String measureId, String username) {
    Measure measure = findMeasureById(measureId);

    Instant now = Instant.now();
    // mongo doesn't create object id for embedded objects, setting manually
    testCase.setId(ObjectId.get().toString());
    testCase.setCreatedAt(now);
    testCase.setCreatedBy(username);
    testCase.setLastModifiedAt(now);
    testCase.setLastModifiedBy(username);
    testCase.setHapiOperationOutcome(null);
    testCase.setResourceUri(null);

    TestCase upserted = upsertFhirPatient(testCase);

    if (measure.getTestCases() == null) {
      measure.setTestCases(List.of(upserted));
    } else {
      measure.getTestCases().add(upserted);
    }

    measureRepository.save(measure);
    return upserted;
  }

  public TestCase updateTestCase(TestCase testCase, String measureId, String username) {
    Measure measure = findMeasureById(measureId);
    if (measure.getTestCases() == null) {
      measure.setTestCases(new ArrayList<>());
    }

    Instant now = Instant.now();
    testCase.setLastModifiedAt(now);
    testCase.setLastModifiedBy(username);

    Optional<TestCase> existingOpt =
        measure.getTestCases().stream().filter(p -> p.getId().equals(testCase.getId())).findFirst();
    if (existingOpt.isPresent()) {
      TestCase existing = existingOpt.get();
      testCase.setCreatedAt(existing.getCreatedAt());
      testCase.setCreatedBy(existing.getCreatedBy());
      testCase.setResourceUri(existing.getResourceUri());
      measure.getTestCases().remove(existing);
    } else {
      // still allowing upsert
      testCase.setId(ObjectId.get().toString());
      testCase.setCreatedAt(now);
      testCase.setCreatedBy(username);
    }

    // try to persist the Patient to HAPI FHIR
    TestCase upsertedTestCase = upsertFhirPatient(testCase);
    measure.getTestCases().add(upsertedTestCase);

    measureRepository.save(measure);
    return testCase;
  }

  public TestCase getTestCase(String measureId, String testCaseId, boolean validate) {
    TestCase testCase =
        Optional.ofNullable(findMeasureById(measureId).getTestCases())
            .orElseThrow(() -> new ResourceNotFoundException("Test Case", testCaseId)).stream()
            .filter(tc -> tc.getId().equals(testCaseId))
            .findFirst()
            .orElse(null);
    if (testCase == null) {
      throw new ResourceNotFoundException("Test Case", testCaseId);
    } else if (validate && !testCase.isValidResource()) {
      testCase = upsertFhirPatient(testCase);
    }
    return testCase;
  }

  public List<TestCase> findTestCasesByMeasureId(String measureId) {
    return findMeasureById(measureId).getTestCases();
  }

  public Measure findMeasureById(String measureId) {
    Measure measure = measureRepository.findById(measureId).orElse(null);
    if (measure == null) {
      throw new ResourceNotFoundException("Measure", measureId);
    }
    return measure;
  }

  public List<String> findTestCaseSeriesByMeasureId(String measureId) {
    Measure measure =
        measureRepository
            .findAllTestCaseSeriesByMeasureId(measureId)
            .orElseThrow(() -> new ResourceNotFoundException("Measure", measureId));
    return Optional.ofNullable(measure.getTestCases()).orElse(List.of()).stream()
        .map(TestCase::getSeries)
        .filter(series -> series != null && !series.trim().isEmpty())
        .distinct()
        .collect(Collectors.toList());
  }

  public TestCase upsertFhirPatient(TestCase testCase) {
    if (testCase != null && testCase.getJson() != null && !testCase.getJson().isEmpty()) {
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
      HttpEntity<String> patientEntity = new HttpEntity<>(testCase.getJson(), headers);
      testCase.setValidResource(false);
      try {
        ResponseEntity<String> response =
            testCase.getResourceUri() == null
                ? hapiFhirRestTemplate.exchange(
                    hapiFhirConfig.getHapiFhirUrl() + hapiFhirConfig.getHapiFhirPatientUri(),
                    HttpMethod.POST,
                    patientEntity,
                    String.class)
                : hapiFhirRestTemplate.exchange(
                    hapiFhirConfig.getHapiFhirUrl() + testCase.getResourceUri(),
                    HttpMethod.PUT,
                    patientEntity,
                    String.class);
        testCase.setJson(response.getBody());
        List<String> contentLocation = response.getHeaders().get(HttpHeaders.CONTENT_LOCATION);
        if (contentLocation == null || contentLocation.size() != 1) {
          testCase.setHapiOperationOutcome(
              HapiOperationOutcome.builder()
                  .code(500)
                  .message("Unable to read HAPI response")
                  .build());
        } else {
          final String location = contentLocation.get(0);
          final String locationUri = location.substring(hapiFhirConfig.getHapiFhirUrl().length());
          final String resourceUri =
              locationUri.contains("/_history")
                  ? locationUri.substring(0, locationUri.indexOf("/_history"))
                  : locationUri;
          testCase.setHapiOperationOutcome(
              HapiOperationOutcome.builder().code(response.getStatusCodeValue()).build());
          testCase.setValidResource(true);
          testCase.setResourceUri(resourceUri);
        }
        return testCase;
      } catch (HttpClientErrorException ex) {
        log.info("HAPI FHIR returned response code [{}]", ex.getRawStatusCode());
        return handleHapiPatientClientErrorException(testCase, ex);
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

  private TestCase handleHapiPatientClientErrorException(
      TestCase testCase, HttpClientErrorException ex) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      testCase.setHapiOperationOutcome(
          HapiOperationOutcome.builder()
              .code(ex.getRawStatusCode())
              .message("Unable to persist to HAPI FHIR due to errors")
              .outcomeResponse(mapper.readValue(ex.getResponseBodyAsString(), Object.class))
              .build());
    } catch (JsonProcessingException e) {
      testCase.setHapiOperationOutcome(
          HapiOperationOutcome.builder()
              .code(500)
              .message(
                  "Unable to persist to HAPI FHIR due to errors, "
                      + "but HAPI outcome not able to be interpreted!")
              .build());
    }
    return testCase;
  }
}
