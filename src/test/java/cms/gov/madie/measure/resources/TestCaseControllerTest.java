package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.services.TestCaseService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestCaseControllerTest {
  @Mock private TestCaseService testCaseService;

  @InjectMocks private TestCaseController controller;

  private TestCase testCase;
  private Measure measure;

  @BeforeEach
  public void setUp() {
    testCase = new TestCase();
    testCase.setId("TESTID");
    testCase.setName("IPPPass");
    testCase.setSeries("BloodPressure>124");
    testCase.setCreatedBy("TestUser");
    testCase.setLastModifiedBy("TestUser2");
    testCase.setDescription("TESTCASEDESCRIPTION");

    measure = new Measure();
    measure.setId(ObjectId.get().toString());
    measure.setMeasureSetId("IDIDID");
    measure.setMeasureName("MSR01");
    measure.setVersion("0.001");
  }

  @Test
  void saveTestCase() {
    Mockito.doReturn(testCase)
        .when(testCaseService)
        .persistTestCase(any(TestCase.class), any(String.class));

    TestCase newTestCase = new TestCase();

    ResponseEntity<TestCase> response = controller.addTestCase(newTestCase, measure.getId());
    assertEquals("TESTID", response.getBody().getId());
  }

  @Test
  void getTestCases() {
    Mockito.doReturn(List.of(testCase))
        .when(testCaseService)
        .findTestCasesByMeasureId(any(String.class));

    ResponseEntity<List<TestCase>> response = controller.getTestCasesByMeasureId(measure.getId());
    assertEquals(1, Objects.requireNonNull(response.getBody()).size());
    assertEquals("IPPPass", response.getBody().get(0).getName());
    assertEquals("BloodPressure>124", response.getBody().get(0).getSeries());
  }

  @Test
  void getTestCase() {
    Mockito.doReturn(testCase)
        .when(testCaseService)
        .getTestCase(any(String.class), any(String.class));

    ResponseEntity<TestCase> response = controller.getTestCase(measure.getId(), testCase.getId());
    assertNotNull(response.getBody());
    assertEquals("IPPPass", response.getBody().getName());
    assertEquals("BloodPressure>124", response.getBody().getSeries());
  }

  @Test
  void updateTestCase() {
    Mockito.doReturn(testCase)
        .when(testCaseService)
        .updateTestCase(any(TestCase.class), any(String.class));

    ResponseEntity<TestCase> response =
        controller.updateTestCase(testCase, measure.getId(), testCase.getId());
    assertNotNull(response.getBody());
    assertEquals("IPPPass", response.getBody().getName());
    assertEquals("BloodPressure>124", response.getBody().getSeries());
  }

  @Test
  public void testGetTestCaseSeriesByMeasureIdReturnsEmptyList() {
    when(testCaseService.findTestCaseSeriesByMeasureId(anyString()))
            .thenReturn(List.of());
    ResponseEntity<List<String>> output = controller.getTestCaseSeriesByMeasureId(measure.getId());
    assertNotNull(output.getBody());
    assertEquals(List.of(), output.getBody());
  }

  @Test
  public void testGetTestCaseSeriesByMeasureIdReturnsSeries() {
    when(testCaseService.findTestCaseSeriesByMeasureId(anyString()))
            .thenReturn(List.of("SeriesAAA", "SeriesBBB"));
    ResponseEntity<List<String>> output = controller.getTestCaseSeriesByMeasureId(measure.getId());
    assertNotNull(output.getBody());
    assertEquals(List.of("SeriesAAA","SeriesBBB"), output.getBody());
  }

  @Test
  public void testGetTestCaseSeriesByMeasureIdBubblesUpExceptions() {
    when(testCaseService.findTestCaseSeriesByMeasureId(anyString()))
        .thenThrow(new ResourceNotFoundException("Measure", measure.getId()));
    assertThrows(
        ResourceNotFoundException.class,
        () -> controller.getTestCaseSeriesByMeasureId(measure.getId()));
  }

  @Test
  void saveTestCaseWithSanitizedDescription() {

    Mockito.doReturn(testCase)
        .when(testCaseService)
        .persistTestCase(any(TestCase.class), any(String.class));

    TestCase newTestCase = new TestCase();
    newTestCase.setDescription("TESTCASEDESCRIPTION<script>alert('Wufff!')</script>");

    ResponseEntity<TestCase> response = controller.addTestCase(newTestCase, measure.getId());
    assertEquals("TESTID", response.getBody().getId());
    assertEquals("TESTCASEDESCRIPTION", response.getBody().getDescription());
  }
}
