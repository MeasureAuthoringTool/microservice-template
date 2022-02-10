package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.repositories.MeasureRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TestCaseService {

  private final MeasureRepository measureRepository;

  public TestCaseService(MeasureRepository measureRepository) {
    this.measureRepository = measureRepository;
  }

  public TestCase persistTestCase(TestCase testCase, String measureId) {
    Measure measure = findMeasureById(measureId);

    // mongo doesn't create object id for embedded objects, setting manually
    testCase.setId(ObjectId.get().toString());
    if (measure.getTestCases() == null) {
      measure.setTestCases(List.of(testCase));
    } else {
      measure.getTestCases().add(testCase);
    }
    measureRepository.save(measure);
    return testCase;
  }

  public TestCase updateTestCase(TestCase testCase, String measureId) {
    Measure measure = findMeasureById(measureId);
    measure.getTestCases().removeIf(tc -> tc.getId().equals(testCase.getId()));
    measure.getTestCases().add(testCase);
    measureRepository.save(measure);
    return testCase;
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
}
