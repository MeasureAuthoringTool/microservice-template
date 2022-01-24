package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.services.TestCaseService;
import cms.gov.madie.measure.utils.ControllerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestCaseController {

  @Autowired private final TestCaseService testCaseService;

  @PostMapping(ControllerUtil.TEST_CASE + "/{measureId}")
  public ResponseEntity<TestCase> addTestCase(
      @RequestBody TestCase testCase, @PathVariable String measureId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(testCaseService.persistTestCase(testCase, measureId));
  }

  @GetMapping(ControllerUtil.TEST_CASES + "/{measureId}")
  public ResponseEntity<List<TestCase>> getTestCases(@PathVariable String measureId) {
    return ResponseEntity.ok(testCaseService.findTestCasesByMeasureId(measureId));
  }
}
