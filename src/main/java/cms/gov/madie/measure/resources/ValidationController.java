package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.models.TestCaseGroupPopulation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/_validations")
public class ValidationController {

  @Autowired
  public ValidationController() {}

  @PutMapping("/measures")
  public Measure validateMeasure(
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure) {
    return measure;
  }

  @PutMapping("/populations")
  public Object validateGroupPopulations(
      @RequestBody @Valid List<TestCaseGroupPopulation> groupPopulations) {
    return groupPopulations;
  }

  @PutMapping("/population")
  public Object validatePopulation(@RequestBody @Valid TestCaseGroupPopulation population) {
    return population;
  }

  @PutMapping("/populationx")
  public Object validatePopulation(@RequestBody @Valid TestPojo pojo) {
    return pojo;
  }

  @PutMapping("/test-cases")
  public TestCase validatedTestCase(
      @RequestBody @Validated(TestCase.ValidationSequence.class) TestCase testCase) {
    return testCase;
  }

  @PutMapping("/test-cases2")
  public TestCase validaTestCase(@RequestBody @Valid TestCase testCase) {
    return testCase;
  }

  @Data
  static class TestPojo {
    @Valid private List<TestCaseGroupPopulation> groupPopulations;
  }
}
