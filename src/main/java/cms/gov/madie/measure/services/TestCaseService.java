package cms.gov.madie.measure.services;

import cms.gov.madie.measure.HapiFhirConfig;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.HapiOperationOutcome;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.models.TestCaseWrapper;
import cms.gov.madie.measure.repositories.MeasureRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.gson.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestCaseService {

  private final MeasureRepository measureRepository;
//  private FhirContext fhirR4Context;
//  private IGenericClient hapiFhirR4Client;
  private HapiFhirConfig hapiFhirConfig;

  public TestCaseService(MeasureRepository measureRepository, HapiFhirConfig hapiFhirConfig) {
//  public TestCaseService(MeasureRepository measureRepository, FhirContext fhirR4Context, IGenericClient hapiFhirR4Client) {
    this.measureRepository = measureRepository;
    this.hapiFhirConfig = hapiFhirConfig;
//    this.fhirR4Context = fhirR4Context;
//    this.hapiFhirR4Client = hapiFhirR4Client;
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

//    TestCaseWrapper testCaseWrapper = upsertFhirR4Patient(testCase);
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

//    // try to persist the Patient to HAPI FHIR
//    TestCaseWrapper testCaseWrapper = upsertFhirR4Patient(testCase);
//    measure.getTestCases().add(testCaseWrapper.getTestCase());
    TestCase upsertedTestCase = upsertFhirPatient(testCase);
    measure.getTestCases().add(upsertedTestCase);

    measureRepository.save(measure);
//    return testCaseWrapper;
    return testCase;
  }

  public TestCase getTestCase(String measureId, String testCaseId, boolean validate) {
    TestCase testCase =
        findMeasureById(measureId).getTestCases().stream()
            .filter(tc -> tc.getId().equals(testCaseId))
            .findFirst()
            .orElse(null);
    if (testCase == null) {
      throw new ResourceNotFoundException("Test Case", testCaseId);
    } else if (validate && !testCase.isValidResource()) {
//      TestCaseWrapper testCaseWrapper = upsertFhirR4Patient(testCase);
      testCase = upsertFhirPatient(testCase);
//      testCaseWrapper.getTestCase().setHapiOperationOutcome(testCaseWrapper.getOutcome());
//      return testCaseWrapper;
    }
//    return new TestCaseWrapper(testCase, null);
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

//  public String getFhirR4PatientStringById(String patientId) {
//    Patient patient = hapiFhirR4Client.read().resource(Patient.class).withId(patientId).encodedJson().execute();
//    return fhirR4Context.newJsonParser().encodeResourceToString(patient);
//  }

  public TestCase upsertFhirPatient(TestCase testCase) {
    RestTemplate restTemplate = new RestTemplate();
    String json = testCase.getJson();
    ObjectMapper mapper = new ObjectMapper();
    if (json != null && !json.isEmpty()) {
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
      HttpEntity<String> entity = new HttpEntity<>(json, headers);
      try {
        log.info(testCase.getResourceUri() == null ? "POST" : "PUT");

        ResponseEntity<String> response = testCase.getResourceUri() == null
            ? restTemplate.exchange(hapiFhirConfig.getHapiFhirUrl() + "/Patient", HttpMethod.POST, entity, String.class)
            : restTemplate.exchange(hapiFhirConfig.getHapiFhirUrl() + testCase.getResourceUri(), HttpMethod.PUT, entity, String.class);
//        log.info("got response: {}", response);
        testCase.setJson(response.getBody());
        List<String> contentLocation = response.getHeaders().get(HttpHeaders.CONTENT_LOCATION);
        if (contentLocation == null || contentLocation.size() != 1) {
          // problem
          testCase.setHapiOperationOutcome(HapiOperationOutcome.builder().code(500).message("Unable to read HAPI response").build());
          testCase.setValidResource(false);
        } else {
          final String location = contentLocation.get(0);
          final String locationUri = location.substring(hapiFhirConfig.getHapiFhirUrl().length());
          final String resourceUri = locationUri.contains("/_history") ? locationUri.substring(0, locationUri.indexOf("/_history")) : locationUri;
          log.info("location: [{}]", locationUri);
          log.info("resourceUri: [{}]", resourceUri);
          testCase.setHapiOperationOutcome(HapiOperationOutcome.builder().code(response.getStatusCodeValue()).build());
          testCase.setValidResource(true);
          testCase.setResourceUri(resourceUri);
        }
        return testCase;
      } catch (HttpClientErrorException ex) {
        try {
          Map<String, Object> map = mapper.readValue(ex.getResponseBodyAsString(), Map.class);
          log.info("map: {}", map);
          testCase.setValidResource(false);
          testCase.setHapiOperationOutcome(HapiOperationOutcome.builder()
              .code(ex.getRawStatusCode())
              .message("Unable to persist to HAPI FHIR due to errors")
              .outcomeResponse(map)
              .build());
        } catch (JsonProcessingException e) {
          testCase.setHapiOperationOutcome(HapiOperationOutcome.builder()
              .code(500)
              .message("Unable to persist to HAPI FHIR due to errors, but HAPI outcome not able to be interpreted!")
              .build());
        }
        return testCase;
      } catch (Exception ex) {
        log.error("Exception occurred invoking PUT on HAPI FHIR:", ex);
        testCase.setValidResource(false);
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


//  public TestCase upsertFhirR4Patient(TestCase testCase) {
//    RestTemplate restTemplate = new RestTemplate();
//    String json = testCase.getJson();
//    final String id = testCase.getId();
//    ObjectMapper mapper = new ObjectMapper();
//    String resourceUri = testCase.getResourceUri();
//    if (json != null && !json.isEmpty()) {
//      final String url = hapiFhirConfig.getHapiFhirUrl() + resourceUri;
//      log.info("PUT to url [{}]", url);
//      HttpHeaders headers = new HttpHeaders();
//      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
//      headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
//      try {
//        ObjectNode node = (ObjectNode) mapper.readTree(json);
//        node.put("id", id);
//        HttpEntity<String> entity = new HttpEntity<>(node.toPrettyString(), headers);
//        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
//        log.info("got response: ", response);
//        testCase.setJson(response.getBody());
//        testCase.setHapiOperationOutcome(HapiOperationOutcome.builder().code(200).build());
//        return testCase;
//      } catch (HttpClientErrorException ex) {
//        try {
//          Map map = mapper.readValue(ex.getResponseBodyAsString(), Map.class);
//          log.info("map: {}", map);
//          testCase.setHapiOperationOutcome(HapiOperationOutcome.builder()
//              .code(ex.getRawStatusCode())
//              .message("Unable to persist to HAPI FHIR due to errors")
//              .outcomeResponse(map)
//              .build());
//        } catch (JsonProcessingException e) {
//          testCase.setHapiOperationOutcome(HapiOperationOutcome.builder()
//              .code(500)
//              .message("Unable to persist to HAPI FHIR due to errors, but HAPI outcome not able to be interpreted!")
//              .build());
//        }
//        return testCase;
//      } catch (Exception ex) {
//        log.error("Exception occurred invoking PUT on HAPI FHIR:", ex);
//        testCase.setHapiOperationOutcome(
//            HapiOperationOutcome.builder()
//                .code(500)
//                .message("An unknown exception occurred with the HAPI FHIR server")
//                .build());
//        return testCase;
//      }
//    }
//    return testCase;
//  }

//  public String upsertFhirR4Patient(String patientJson, String testCaseId) {
//    Patient patient = fhirR4Context
//        .newJsonParser()
//        .setParserErrorHandler(new LenientErrorHandler().setErrorOnInvalidValue(false))
//        .parseResource(Patient.class, patientJson);
//    patient.setId(testCaseId);
//    IBaseResource savedResource = hapiFhirR4Client.update().resource(patient).withId(testCaseId).encodedJson().prettyPrint().execute().getResource();
//    return fhirR4Context.newJsonParser().setPrettyPrint(true).encodeResourceToString(savedResource);
//  }
//
//  public TestCase upsertFhirR4Patient(TestCase testCase) {
//    if (testCase != null && testCase.getJson() != null && !testCase.getJson().isEmpty()) {
//      try {
//        String savedJson = upsertFhirR4Patient(testCase.getJson(), testCase.getId());
//        testCase.setJson(savedJson);
//        testCase.setHapiOperationOutcome(HapiOperationOutcome.builder().code(200).build());
//      } catch (Exception ex) {
//        log.error("An error occurred while persisting the HAPI FHIR Patient resource with ID {}: ", testCase.getId(), ex);
//        if (ex instanceof BaseServerResponseException) {
//          BaseServerResponseException hapiException = (BaseServerResponseException) ex;
//          HapiOperationOutcome.HapiOperationOutcomeBuilder builder = HapiOperationOutcome.builder();
//          builder.code(hapiException.getStatusCode()).message(hapiException.getMessage());
//          log.info("HAPI FHIR exception message: ", hapiException.getMessage());
//          IBaseOperationOutcome baseOperationOutcome = hapiException.getOperationOutcome();
//          if (baseOperationOutcome instanceof OperationOutcome) {
//            OperationOutcome operationOutcome = (OperationOutcome) baseOperationOutcome;
//            List<OperationOutcome.OperationOutcomeIssueComponent> issues = operationOutcome.getIssue();
//            builder.issues(issues.stream().map(issue ->
//                    HapiOperationOutcome.OutcomeIssue
//                        .builder()
//                        .code(issue.getCode().getDisplay())
//                        .severity(issue.getSeverity().getDisplay())
//                        .diagnostics(issue.getDiagnostics())
//                        .location(issue.getLocation() != null ? issue.getLocation().stream().map(st -> st.getValue()).collect(Collectors.toList()) : null)
//                        .build())
//                .collect(Collectors.toList())
//            );
//          }
//
//          testCase.setHapiOperationOutcome(
//              builder
//                  .outcome(fhirR4Context.newJsonParser().encodeResourceToString(hapiException.getOperationOutcome()))
//                  .build()
//          );
//        } else {
//          testCase.setHapiOperationOutcome(HapiOperationOutcome.builder()
//              .code(500)
//              .message("An error occurred while persisting the HAPI FHIR Patient resource")
//              .build());
//        }
//      }
//    }
//    return testCase;
//  }
//
//  public String createFhirR4PatientString(String patientJson) {
//    Patient patient = fhirR4Context.newJsonParser().parseResource(Patient.class, patientJson);
//    IBaseResource savedResource = hapiFhirR4Client.create().resource(patient).execute().getResource();
//    return fhirR4Context.newJsonParser().encodeResourceToString(savedResource);
//  }
//
//  public ValidationResult validatePatientResource(Patient resource) {
//    DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(fhirR4Context);
//
//
//    return fhirR4Context
//        .newValidator()
//        .validateWithResult(
//            resource,
//            new ValidationOptions().addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient")
//        );
//  }


}
