package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.services.TestCaseService;
import cms.gov.madie.measure.utils.ControllerUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest({TestCaseController.class})
public class TestCaseControllerMvcTest {

  @MockBean private TestCaseService testCaseService;
  @Autowired private MockMvc mockMvc;
  @Captor ArgumentCaptor<TestCase> testCaseCaptor;
  @Captor ArgumentCaptor<String> measureIdCaptor;

  private TestCase testCase;
  private static final String TEST_ID = "TESTID";
  private static final String TEST_USER = "TestUser";
  private static final String TEST_USER_2 = "TestUser2";
  private static final String TEST_NAME = "TestName";
  private static final String TEST_DESCRIPTION = "Test Description";
  private static final String TEST_USER_ID = "test-okta-user-id-123";

  @BeforeEach
  public void setUp() {
    testCase = new TestCase();
    testCase.setId(TEST_ID);
    testCase.setDescription(TEST_DESCRIPTION);
    testCase.setCreatedBy(TEST_USER);
    testCase.setLastModifiedBy(TEST_USER_2);
    testCase.setName(TEST_NAME);
  }

  @Test
  public void testNewTestCase() throws Exception {
    when(testCaseService.persistTestCase(any(TestCase.class), any(String.class)))
        .thenReturn(testCase);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ControllerUtil.TEST_CASE + "/fooId")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(asJsonString(testCase))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(TEST_ID))
        .andExpect(MockMvcResultMatchers.jsonPath("$.createdBy").value(TEST_USER))
        .andExpect(MockMvcResultMatchers.jsonPath("$.lastModifiedBy").value(TEST_USER_2))
        .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(TEST_NAME));
    verify(testCaseService, times(1))
        .persistTestCase(testCaseCaptor.capture(), measureIdCaptor.capture());
    TestCase persistedTestCase = testCaseCaptor.getValue();
    assertEquals(TEST_DESCRIPTION, persistedTestCase.getDescription());
  }

  @Test
  public void testGetTestCases() throws Exception {
    when(testCaseService.findTestCasesByMeasureId(any(String.class))).thenReturn(List.of(testCase));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(ControllerUtil.TEST_CASES + "/testId")
                .with(user(TEST_USER_ID))
                .with(csrf()))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(
                    "[{\"id\":\"TESTID\",\"name\":\"TestName\",\"series\":null,"
                        + "\"description\":\"Test Description\",\"createdAt\":null,"
                        + "\"createdBy\":\"TestUser\",\"lastModifiedAt\":null,"
                        + "\"lastModifiedBy\":\"TestUser2\"}]"));
    verify(testCaseService, times(1)).findTestCasesByMeasureId(measureIdCaptor.capture());
    String measureId = measureIdCaptor.getValue();
    assertEquals(measureId, "testId");
  }

  private String asJsonString(final Object obj) throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(obj);
  }
}
