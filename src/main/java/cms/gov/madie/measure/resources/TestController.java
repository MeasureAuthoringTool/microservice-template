package cms.gov.madie.measure.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.validation.ValidationResult;
import cms.gov.madie.measure.services.TestCaseService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class TestController {

  private FhirContext fhirR4Context;
  private IGenericClient hapiFhirR4Client;
  private TestCaseService testCaseService;

  @Autowired
  public TestController(FhirContext fhirR4Context, IGenericClient hapiFhirR4Client, TestCaseService testCaseService) {
    this.fhirR4Context = fhirR4Context;
    this.hapiFhirR4Client = hapiFhirR4Client;
    this.testCaseService = testCaseService;
  }

  @GetMapping(value="/patients", produces = MediaType.APPLICATION_JSON_VALUE)
  public Object getPatients() {
//    hapiFhirR4Client.read().resource(Patient.class). ;
    List<Patient> patients = new ArrayList<>();

    Bundle bundle = hapiFhirR4Client.search().forResource(Patient.class).returnBundle(Bundle.class).execute();
    patients.addAll(BundleUtil.toListOfResourcesOfType(hapiFhirR4Client.getFhirContext(), bundle, Patient.class));
    log.info("Got [{}] patients!", patients.size());
    return hapiFhirR4Client.getFhirContext().newJsonParser().encodeResourceToString(bundle);
  }

  @GetMapping(value="/patients/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity getPatient(@PathVariable String patientId) {
    try {
      return ResponseEntity.ok(testCaseService.getFhirR4PatientStringById(patientId));
    } catch (Exception ex) {
      if (ex instanceof BaseServerResponseException) {
        log.info("HAPI FHIR exception occurred", ex);
        BaseServerResponseException hapiException = (BaseServerResponseException) ex;
//        hapiException.
        return ResponseEntity.status(hapiException.getStatusCode()).body(fhirR4Context.newJsonParser().encodeResourceToString(hapiException.getOperationOutcome()));
      }
      log.info("exception type: {}", ex.getClass());
      log.error("Error occurred while fetching patient from HAPI FHIR", ex);
      return ResponseEntity.internalServerError().body("something went wrong");
//      return new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong..");
    }
  }

  @PostMapping(value="/patients", produces = MediaType.APPLICATION_JSON_VALUE)
  public Object createPatient(@RequestBody Map<String,String> content) {
    final String patientStr = content.get("json");
    log.info("creating new patient");
    Patient patient = fhirR4Context.newJsonParser().parseResource(Patient.class, patientStr);
    log.info("parsed patient string");
    MethodOutcome outcome = hapiFhirR4Client.create().resource(patientStr).encodedJson().execute();
    return fhirR4Context.newJsonParser().encodeResourceToString(outcome.getResource());
  }

  @PostMapping(value="/patients/$validate", produces = MediaType.APPLICATION_JSON_VALUE)
  public Object validatePatient(@RequestBody Map<String,String> content) {
    final String patientStr = content.get("json");
    log.info("validating patient");
    Patient patient = fhirR4Context.newJsonParser().parseResource(Patient.class, patientStr);
    log.info("parsed patient string");
    ValidationResult validationResult = testCaseService.validatePatientResource(patient);
    return fhirR4Context.newJsonParser().encodeResourceToString(validationResult.toOperationOutcome());
  }

}
