package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import cms.gov.madie.measure.services.TestCaseService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

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
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    doReturn(testCase)
        .when(testCaseService)
        .persistTestCase(any(TestCase.class), any(String.class), any(String.class), anyString());

    TestCase newTestCase = new TestCase();

    ResponseEntity<TestCase> response =
        controller.addTestCase(newTestCase, measure.getId(), "TOKEN", principal);
    assertNotNull(response.getBody());
    assertNotNull(response.getBody());
    assertEquals("TESTID", response.getBody().getId());
  }

  @Test
  void getTestCases() {
    doReturn(List.of(testCase)).when(testCaseService).findTestCasesByMeasureId(any(String.class));

    ResponseEntity<List<TestCase>> response = controller.getTestCasesByMeasureId(measure.getId());
    assertEquals(1, Objects.requireNonNull(response.getBody()).size());
    assertEquals("IPPPass", response.getBody().get(0).getName());
    assertEquals("BloodPressure>124", response.getBody().get(0).getSeries());
  }

  @Test
  void getTestCase() {
    doReturn(testCase)
        .when(testCaseService)
        .getTestCase(any(String.class), any(String.class), anyBoolean(), anyString());
    ResponseEntity<TestCase> response =
        controller.getTestCase(measure.getId(), testCase.getId(), true, "TOKEN");
    assertNotNull(response.getBody());
    assertNotNull(response.getBody());
    assertEquals("IPPPass", response.getBody().getName());
    assertEquals("BloodPressure>124", response.getBody().getSeries());
  }

  @Test
  void updateTestCase() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    doReturn(testCase)
        .when(testCaseService)
        .updateTestCase(any(TestCase.class), any(String.class), any(String.class), anyString());

    ResponseEntity<TestCase> response =
        controller.updateTestCase(testCase, measure.getId(), testCase.getId(), "TOKEN", principal);
    assertNotNull(response.getBody());
    assertNotNull(response.getBody());
    assertEquals("IPPPass", response.getBody().getName());
    assertEquals("BloodPressure>124", response.getBody().getSeries());

    ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
    verify(testCaseService, times(1))
        .updateTestCase(any(TestCase.class), anyString(), usernameCaptor.capture(), anyString());
    assertEquals("test.user2", usernameCaptor.getValue());
  }

  @Test
  public void testGetTestCaseSeriesByMeasureIdReturnsEmptyList() {
    when(testCaseService.findTestCaseSeriesByMeasureId(anyString())).thenReturn(List.of());
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
    assertEquals(List.of("SeriesAAA", "SeriesBBB"), output.getBody());
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

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    doReturn(testCase)
        .when(testCaseService)
        .persistTestCase(any(TestCase.class), any(String.class), any(String.class), anyString());

    TestCase newTestCase = new TestCase();
    newTestCase.setDescription("TESTCASEDESCRIPTION<script>alert('Wufff!')</script>");

    ResponseEntity<TestCase> response =
        controller.addTestCase(newTestCase, measure.getId(), "TOKEN", principal);
    assertEquals("TESTID", response.getBody().getId());
    assertEquals("TESTCASEDESCRIPTION", response.getBody().getDescription());
  }

  @Test
  void updateTestCaseWithSanitizedDescription() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user2");

    doReturn(testCase)
        .when(testCaseService)
        .updateTestCase(any(TestCase.class), any(String.class), any(String.class), anyString());

    testCase.setDescription("TESTCASEDESCRIPTION<script>alert('Wufff!')</script>");

    ResponseEntity<TestCase> response =
        controller.updateTestCase(testCase, measure.getId(), testCase.getId(), "TOKEN", principal);
    assertNotNull(response.getBody());
    assertEquals("IPPPass", response.getBody().getName());
    assertEquals("BloodPressure>124", response.getBody().getSeries());
    assertEquals("TESTCASEDESCRIPTION", response.getBody().getDescription());

    ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
    verify(testCaseService, times(1))
        .updateTestCase(any(TestCase.class), anyString(), usernameCaptor.capture(), anyString());
    assertEquals("test.user2", usernameCaptor.getValue());
  }
}
