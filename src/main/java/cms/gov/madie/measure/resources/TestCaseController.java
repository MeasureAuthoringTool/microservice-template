package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.services.TestCaseService;
import cms.gov.madie.measure.utils.ControllerUtil;
import cms.gov.madie.measure.utils.UserInputSanitizeUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestCaseController {

  @Autowired private final TestCaseService testCaseService;

  @PostMapping(ControllerUtil.TEST_CASES)
  public ResponseEntity<TestCase> addTestCase(
      @RequestBody @Validated(TestCase.ValidationSequence.class) TestCase testCase,
      @PathVariable String measureId,
      Principal principal) {

    sanitizeTestCase(testCase);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(testCaseService.persistTestCase(testCase, measureId, principal.getName()));
  }

  @GetMapping(ControllerUtil.TEST_CASES)
  public ResponseEntity<List<TestCase>> getTestCasesByMeasureId(@PathVariable String measureId) {
    return ResponseEntity.ok(testCaseService.findTestCasesByMeasureId(measureId));
  }

  @GetMapping(ControllerUtil.TEST_CASES + "/{testCaseId}")
  public ResponseEntity<TestCase> getTestCase(
      @PathVariable String measureId, @PathVariable String testCaseId) {
    return ResponseEntity.ok(testCaseService.getTestCase(measureId, testCaseId));
  }

  @PutMapping(ControllerUtil.TEST_CASES + "/{testCaseId}")
  public ResponseEntity<TestCase> updateTestCase(
      @RequestBody @Validated(TestCase.ValidationSequence.class) TestCase testCase,
      @PathVariable String measureId,
      @PathVariable String testCaseId,
      Principal principal) {
    if (testCase.getId() == null || !testCase.getId().equals(testCaseId)) {
      throw new ResourceNotFoundException("Test Case", testCaseId);
    }
    sanitizeTestCase(testCase);

    return ResponseEntity.ok(
        testCaseService.updateTestCase(testCase, measureId, principal.getName()));
  }

  @GetMapping(ControllerUtil.TEST_CASES + "/series")
  public ResponseEntity<List<String>> getTestCaseSeriesByMeasureId(@PathVariable String measureId) {
    return ResponseEntity.ok(testCaseService.findTestCaseSeriesByMeasureId(measureId));
  }

  private TestCase sanitizeTestCase(TestCase testCase) {
    testCase.setDescription(UserInputSanitizeUtil.sanitizeUserInput(testCase.getDescription()));
    testCase.setTitle(UserInputSanitizeUtil.sanitizeUserInput(testCase.getTitle()));
    testCase.setSeries(UserInputSanitizeUtil.sanitizeUserInput(testCase.getSeries()));
    return testCase;
  }
}
