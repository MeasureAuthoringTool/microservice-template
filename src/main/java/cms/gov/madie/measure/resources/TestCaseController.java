package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.models.TestCaseWrapper;
import cms.gov.madie.measure.services.TestCaseService;
import cms.gov.madie.measure.utils.ControllerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestCaseController {

  @Autowired private final TestCaseService testCaseService;

  @PostMapping(ControllerUtil.TEST_CASES)
  public ResponseEntity<TestCase> addTestCase(
      @RequestBody TestCase testCase, @PathVariable String measureId, Principal principal) {
//    return ResponseEntity.status(HttpStatus.CREATED)
//        .body(testCaseService.persistTestCase(testCase, measureId, principal.getName()));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(testCaseService.persistTestCase(testCase, measureId, principal.getName()));
  }

  @GetMapping(ControllerUtil.TEST_CASES)
  public ResponseEntity<List<TestCase>> getTestCasesByMeasureId(@PathVariable String measureId) {
    return ResponseEntity.ok(testCaseService.findTestCasesByMeasureId(measureId));
  }

  @GetMapping(ControllerUtil.TEST_CASES + "/{testCaseId}")
  public ResponseEntity<TestCase> getTestCase(
      @PathVariable String measureId, @PathVariable String testCaseId, @RequestParam(name = "validate", defaultValue = "true") boolean validate) {
    return ResponseEntity.ok(testCaseService.getTestCase(measureId, testCaseId, validate));
  }

  @PutMapping(ControllerUtil.TEST_CASES + "/{testCaseId}")
  public ResponseEntity<TestCase> updateTestCase(
      @RequestBody TestCase testCase,
      @PathVariable String measureId,
      @PathVariable String testCaseId,
      Principal principal) {
    if (testCase.getId() == null || !testCase.getId().equals(testCaseId)) {
      throw new ResourceNotFoundException("Test Case", testCaseId);
    }
    return ResponseEntity.ok(
        testCaseService.updateTestCase(testCase, measureId, principal.getName()));
  }

  @GetMapping(ControllerUtil.TEST_CASES + "/series")
  public ResponseEntity<List<String>> getTestCaseSeriesByMeasureId(@PathVariable String measureId) {
    return ResponseEntity.ok(testCaseService.findTestCaseSeriesByMeasureId(measureId));
  }
}
