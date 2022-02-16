package cms.gov.madie.measure.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.HapiOperationOutcome;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.models.TestCaseWrapper;
import cms.gov.madie.measure.repositories.MeasureRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestCaseService {

  private final MeasureRepository measureRepository;
  private FhirContext fhirR4Context;
  private IGenericClient hapiFhirR4Client;

  public TestCaseService(MeasureRepository measureRepository, FhirContext fhirR4Context, IGenericClient hapiFhirR4Client) {
    this.measureRepository = measureRepository;
    this.fhirR4Context = fhirR4Context;
    this.hapiFhirR4Client = hapiFhirR4Client;
  }

  public TestCaseWrapper persistTestCase(TestCase testCase, String measureId, String username) {
    Measure measure = findMeasureById(measureId);

    Instant now = Instant.now();
    // mongo doesn't create object id for embedded objects, setting manually
    testCase.setId(ObjectId.get().toString());
    testCase.setCreatedAt(now);
    testCase.setCreatedBy(username);
    testCase.setLastModifiedAt(now);
    testCase.setLastModifiedBy(username);

    HapiOperationOutcome outcome = new HapiOperationOutcome();
    if (testCase.getJson() != null && !testCase.getJson().isEmpty()) {
      try {
        String savedJson = upsertFhirR4Patient(testCase.getJson(), testCase.getId());
        testCase.setJson(savedJson);
        outcome.setCode(200);
      } catch (Exception ex) {
        log.error("An error occurred while persisting the HAPI FHIR Patient resource with ID {}: ", testCase.getId(), ex);
        if (ex instanceof BaseServerResponseException) {
          BaseServerResponseException hapiException = (BaseServerResponseException) ex;
          outcome.setCode(hapiException.getStatusCode());
          log.info("HAPI FHIR exception message: ", hapiException.getMessage());
          outcome.setMessage(hapiException.getMessage());
          outcome.setOutcome(fhirR4Context.newJsonParser().encodeResourceToString(hapiException.getOperationOutcome()));
        } else {
          outcome.setCode(500);
          outcome.setMessage("An error occurred while persisting the HAPI FHIR Patient resource");
        }
      }
    }

    if (measure.getTestCases() == null) {
      measure.setTestCases(List.of(testCase));
    } else {
      measure.getTestCases().add(testCase);
    }

    measureRepository.save(measure);
    return new TestCaseWrapper(testCase, outcome);
  }

  public TestCaseWrapper updateTestCase(TestCase testCase, String measureId, String username) {
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
      measure.getTestCases().remove(existing);
    } else {
      // still allowing upsert
      testCase.setId(ObjectId.get().toString());
      testCase.setCreatedAt(now);
      testCase.setCreatedBy(username);
    }

    // try to persist the Patient to HAPI FHIR
    HapiOperationOutcome outcome = new HapiOperationOutcome();
    if (testCase.getJson() != null && !testCase.getJson().isEmpty()) {
      try {
        String savedJson = upsertFhirR4Patient(testCase.getJson(), testCase.getId());
        testCase.setJson(savedJson);
        outcome.setCode(200);
      } catch (Exception ex) {
        log.error("An error occurred while persisting the HAPI FHIR Patient resource with ID {}: ", testCase.getId(), ex);
        if (ex instanceof BaseServerResponseException) {
          BaseServerResponseException hapiException = (BaseServerResponseException) ex;
          outcome.setCode(hapiException.getStatusCode());
          log.info("HAPI FHIR exception message: ", hapiException.getMessage());
          outcome.setMessage(hapiException.getMessage());
          IBaseOperationOutcome baseOperationOutcome = hapiException.getOperationOutcome();
          if (baseOperationOutcome instanceof OperationOutcome) {
            OperationOutcome operationOutcome = (OperationOutcome) baseOperationOutcome;
            List<OperationOutcome.OperationOutcomeIssueComponent> issues = operationOutcome.getIssue();
            outcome.setIssues(issues.stream().map(issue ->
                HapiOperationOutcome.OutcomeIssue
                    .builder()
                    .code(issue.getCode().getDisplay())
                    .severity(issue.getSeverity().getDisplay())
                    .diagnostics(issue.getDiagnostics())
                    .location(issue.getLocation() != null ? issue.getLocation().stream().map(st -> st.getValue()).collect(Collectors.toList()) : null)
                    .build())
                .collect(Collectors.toList())
            );
          }

          outcome.setOutcome(fhirR4Context.newJsonParser().encodeResourceToString(hapiException.getOperationOutcome()));
        } else {
          outcome.setCode(500);
          outcome.setMessage("An error occurred while persisting the HAPI FHIR Patient resource");
        }
      }
    }

    measure.getTestCases().add(testCase);

    measureRepository.save(measure);
    return new TestCaseWrapper(testCase, outcome);
  }

  public TestCase getTestCase(String measureId, String testCaseId) {
    TestCase testCase =
        findMeasureById(measureId).getTestCases().stream()
            .filter(tc -> tc.getId().equals(testCaseId))
            .findFirst()
            .orElse(null);
    if (testCase == null) {
      throw new ResourceNotFoundException("Test Case", testCaseId);
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

  public String getFhirR4PatientStringById(String patientId) {
    Patient patient = hapiFhirR4Client.read().resource(Patient.class).withId(patientId).encodedJson().execute();
    return fhirR4Context.newJsonParser().encodeResourceToString(patient);
  }

  public String upsertFhirR4Patient(String patientJson, String testCaseId) {
    Patient patient = fhirR4Context
        .newJsonParser()
        .setParserErrorHandler(new LenientErrorHandler().setErrorOnInvalidValue(false))
        .parseResource(Patient.class, patientJson);
    patient.setId(testCaseId);
    IBaseResource savedResource = hapiFhirR4Client.update().resource(patient).withId(testCaseId).encodedJson().prettyPrint().execute().getResource();
    return fhirR4Context.newJsonParser().setPrettyPrint(true).encodeResourceToString(savedResource);
  }

  public String createFhirR4PatientString(String patientJson) {
    Patient patient = fhirR4Context.newJsonParser().parseResource(Patient.class, patientJson);
    IBaseResource savedResource = hapiFhirR4Client.create().resource(patient).execute().getResource();
    return fhirR4Context.newJsonParser().encodeResourceToString(savedResource);
  }

  public ValidationResult validatePatientResource(Patient resource) {
    DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(fhirR4Context);


    return fhirR4Context
        .newValidator()
        .validateWithResult(
            resource,
            new ValidationOptions().addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient")
        );
  }


}
