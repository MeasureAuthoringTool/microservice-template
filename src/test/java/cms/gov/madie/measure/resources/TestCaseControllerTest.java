package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.common.Version;
import cms.gov.madie.measure.services.TestCaseService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TestCaseControllerTest {
  @Mock private TestCaseService testCaseService;
  @Mock private MeasureRepository repository;

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
    measure.setVersion(new Version(0, 0, 1));
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
  void setTestCaseList() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    List<TestCase> savedTestCases =
        List.of(
            TestCase.builder()
                .id("ID1")
                .title("Test1")
                .json("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}")
                .validResource(true)
                .build(),
            TestCase.builder()
                .id("ID2")
                .title("Test2")
                .json("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}")
                .validResource(true)
                .build());

    when(testCaseService.persistTestCases(anyList(), anyString(), anyString(), anyString()))
        .thenReturn(savedTestCases);

    doReturn(Optional.of(new Measure().toBuilder().createdBy("test.user").build()))
        .when(repository)
        .findById(any());
    List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .title("Test1")
                .json("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}")
                .build(),
            TestCase.builder()
                .title("Test2")
                .json("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}")
                .build());
    ResponseEntity<List<TestCase>> output =
        controller.addTestCases(testCases, "MeasureID", "Bearer Token", principal);
    assertThat(output, is(notNullValue()));
    assertThat(output.getStatusCode(), is(equalTo(HttpStatus.CREATED)));
    assertThat(output.getBody(), is(equalTo(savedTestCases)));
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
  void testSuccessfulDeleteTestCase() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    String returnOutput = "Test case deleted successfully: TC1_ID";
    doReturn(returnOutput)
        .when(testCaseService)
        .deleteTestCase(any(String.class), any(String.class), any(String.class));

    ResponseEntity<String> output = controller.deleteTestCase("measure-id", "TC1_ID", principal);

    assertThat(output.getBody(), is(equalTo("Test case deleted successfully: TC1_ID")));
    assertThat(output.getStatusCode(), is(equalTo(HttpStatus.OK)));
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
