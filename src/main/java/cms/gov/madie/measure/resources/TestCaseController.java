package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.dto.ValidList;
import cms.gov.madie.measure.exceptions.InvalidRequestException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.MeasureService;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import cms.gov.madie.measure.services.TestCaseService;
import cms.gov.madie.measure.services.TestCaseShiftDatesService;
import cms.gov.madie.measure.utils.ControllerUtil;
import cms.gov.madie.measure.utils.UserInputSanitizeUtil;
import gov.cms.madie.models.measure.TestCaseImportOutcome;
import gov.cms.madie.models.measure.TestCaseImportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestCaseController {

  private final TestCaseService testCaseService;
  private final MeasureRepository measureRepository;
  private final MeasureService measureService;
  private final TestCaseShiftDatesService testCaseShiftDatesService;

  @PostMapping(ControllerUtil.TEST_CASES)
  public ResponseEntity<TestCase> addTestCase(
      @RequestBody @Validated(TestCase.ValidationSequence.class) TestCase testCase,
      @PathVariable String measureId,
      @RequestHeader("Authorization") String accessToken,
      Principal principal) {

    sanitizeTestCase(testCase);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            testCaseService.persistTestCase(testCase, measureId, principal.getName(), accessToken));
  }

  @PostMapping(ControllerUtil.TEST_CASES + "/list")
  public ResponseEntity<List<TestCase>> addTestCases(
      @RequestBody @Validated(TestCase.ValidationSequence.class) ValidList<TestCase> testCases,
      @PathVariable String measureId,
      @RequestHeader("Authorization") String accessToken,
      Principal principal) {
    final String username = principal.getName();
    Optional<Measure> measureOptional = measureRepository.findById(measureId);
    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", measureId);
    }
    Measure measure = measureOptional.get();
    measureService.verifyAuthorization(username, measure);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            testCaseService.persistTestCases(
                testCases, measureId, principal.getName(), accessToken));
  }

  @GetMapping(ControllerUtil.TEST_CASES)
  public ResponseEntity<List<TestCase>> getTestCasesByMeasureId(@PathVariable String measureId) {
    return ResponseEntity.ok(testCaseService.findTestCasesByMeasureId(measureId));
  }

  @GetMapping(ControllerUtil.TEST_CASES + "/{testCaseId}")
  public ResponseEntity<TestCase> getTestCase(
      @PathVariable String measureId,
      @PathVariable String testCaseId,
      @RequestParam(name = "validate", defaultValue = "true") boolean validate,
      @RequestHeader("Authorization") String accessToken) {
    return ResponseEntity.ok(
        testCaseService.getTestCase(measureId, testCaseId, validate, accessToken));
  }

  @PutMapping(ControllerUtil.TEST_CASES + "/{testCaseId}")
  public ResponseEntity<TestCase> updateTestCase(
      @RequestBody @Validated(TestCase.ValidationSequence.class) TestCase testCase,
      @PathVariable String measureId,
      @PathVariable String testCaseId,
      @RequestHeader("Authorization") String accessToken,
      Principal principal) {
    if (testCase.getId() == null || !testCase.getId().equals(testCaseId)) {
      throw new ResourceNotFoundException("Test Case", testCaseId);
    }
    sanitizeTestCase(testCase);

    return ResponseEntity.ok(
        testCaseService.updateTestCase(testCase, measureId, principal.getName(), accessToken));
  }

  @GetMapping(ControllerUtil.TEST_CASES + "/series")
  public ResponseEntity<List<String>> getTestCaseSeriesByMeasureId(@PathVariable String measureId) {
    return ResponseEntity.ok(testCaseService.findTestCaseSeriesByMeasureId(measureId));
  }

  @DeleteMapping(ControllerUtil.TEST_CASES + "/{testCaseId}")
  public ResponseEntity<String> deleteTestCase(
      @RequestBody @PathVariable String measureId,
      @PathVariable String testCaseId,
      Principal principal) {

    log.info(
        "User [{}] is attempting to delete a test case with Id [{}] from measure [{}]",
        principal.getName(),
        testCaseId,
        measureId);
    return ResponseEntity.ok(
        testCaseService.deleteTestCase(measureId, testCaseId, principal.getName()));
  }

  @DeleteMapping(ControllerUtil.TEST_CASES)
  public ResponseEntity<String> deleteTestCases(
      @PathVariable String measureId, @RequestBody List<String> testCaseIds, Principal principal) {

    log.info(
        "User [{}] is attempting to delete following test cases with Ids [{}] from measure [{}]",
        principal.getName(),
        String.join(", ", testCaseIds),
        measureId);
    return ResponseEntity.ok(
        testCaseService.deleteTestCases(measureId, testCaseIds, principal.getName()));
  }

  @PutMapping(ControllerUtil.TEST_CASES + "/imports")
  public ResponseEntity<List<TestCaseImportOutcome>> importTestCases(
      @RequestBody List<TestCaseImportRequest> testCaseImportRequests,
      @PathVariable String measureId,
      @RequestHeader("Authorization") String accessToken,
      Principal principal) {
    final String userName = principal.getName();
    var testCaseImportOutcomes =
        testCaseService.importTestCases(
            testCaseImportRequests, measureId, userName, accessToken, ModelType.QI_CORE.getValue());
    return ResponseEntity.ok().body(testCaseImportOutcomes);
  }

  @PutMapping(ControllerUtil.TEST_CASES + "/imports/qdm")
  public ResponseEntity<List<TestCaseImportOutcome>> importTestCasesQdm(
      @RequestBody List<TestCaseImportRequest> testCaseImportRequests,
      @PathVariable String measureId,
      @RequestHeader("Authorization") String accessToken,
      Principal principal) {
    final String userName = principal.getName();

    for (TestCaseImportRequest request : testCaseImportRequests) {
      request.setPatientId(UUID.randomUUID());
      // append given and family name to the import object to report to outcome
      try {
        String familyName =
            testCaseService.getPatientFamilyName(ModelType.QDM_5_6.getValue(), request.getJson());
        request.setFamilyName(familyName);
        String givenName =
            testCaseService.getPatientGivenName(ModelType.QDM_5_6.getValue(), request.getJson());
        request.setGivenNames(Collections.singletonList(givenName));
      } catch (JsonProcessingException ex) {
        log.error(
            "User {} is unable to import test case with patient id : "
                + "{} because of JsonProcessingException: "
                + ex,
            userName,
            request.getPatientId());
        throw new InvalidRequestException(ex.getMessage());
      }
    }
    var testCaseImportOutcomes =
        testCaseService.importTestCases(
            testCaseImportRequests, measureId, userName, accessToken, ModelType.QDM_5_6.getValue());
    return ResponseEntity.ok().body(testCaseImportOutcomes);
  }

  private TestCase sanitizeTestCase(TestCase testCase) {
    testCase.setDescription(UserInputSanitizeUtil.sanitizeUserInput(testCase.getDescription()));
    testCase.setTitle(UserInputSanitizeUtil.sanitizeUserInput(testCase.getTitle()));
    testCase.setSeries(UserInputSanitizeUtil.sanitizeUserInput(testCase.getSeries()));
    return testCase;
  }

  @PutMapping(ControllerUtil.TEST_CASES + "/{testCaseId}/qdm/shiftDates")
  public ResponseEntity<TestCase> shiftTestCaseDates(
      @PathVariable String measureId,
      @PathVariable String testCaseId,
      @RequestParam(name = "shifted", defaultValue = "0") int shifted,
      @RequestHeader("Authorization") String accessToken,
      Principal principal) {
    return ResponseEntity.ok(
        testCaseShiftDatesService.shiftTestCaseDates(
            measureId, testCaseId, shifted, principal.getName(), accessToken));
  }

  @GetMapping(ControllerUtil.TEST_CASES + "/qdm/shiftAllDates")
  public ResponseEntity<List<TestCase>> shiftAllTestCaseDates(
      @PathVariable String measureId,
      @RequestParam(name = "shifted", defaultValue = "0") int shifted,
      @RequestHeader("Authorization") String accessToken,
      Principal principal) {
    return ResponseEntity.ok(
        testCaseShiftDatesService.shiftAllTestCaseDates(
            measureId, shifted, principal.getName(), accessToken));
  }
}
