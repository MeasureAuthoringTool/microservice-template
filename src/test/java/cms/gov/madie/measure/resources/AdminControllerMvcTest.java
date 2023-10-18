package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.SecurityConfig;
import cms.gov.madie.measure.dto.JobStatus;
import cms.gov.madie.measure.dto.MeasureTestCaseValidationReport;
import cms.gov.madie.measure.dto.TestCaseValidationReport;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.MeasureService;
import cms.gov.madie.measure.services.MeasureSetService;
import cms.gov.madie.measure.services.TestCaseService;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminController.class})
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class AdminControllerMvcTest {
  private static final String ADMIN_TEST_API_KEY_HEADER = "api-key";
  private static final String ADMIN_TEST_API_KEY_HEADER_VALUE = "0a51991c";
  private static final String TEST_USER_ID = "test-okta-user-id-123";

  @MockBean private MeasureService measureService;
  @MockBean private MeasureSetService measureSetService;
  @MockBean private TestCaseService testCaseService;
  @MockBean private ActionLogService actionLogService;

  @MockBean private MeasureRepository measureRepository;

  @Autowired private MockMvc mockMvc;

  @Test
  public void testValidateAllMeasureTestCasesNoMeasuresFoundDefaultDraftOnly() throws Exception {
    when(measureService.getAllActiveMeasureIds(eq(true))).thenReturn(List.of());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/admin/measures/test-cases/validations")
                .with(csrf())
                .with(user(TEST_USER_ID))
                .header(ADMIN_TEST_API_KEY_HEADER, ADMIN_TEST_API_KEY_HEADER_VALUE)
                .header("Authorization", "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reports", empty()))
        .andExpect(jsonPath("$.impactedMeasures", empty()));
    verifyNoInteractions(testCaseService);
  }

  @Test
  public void testValidateAllMeasureTestCasesNoMeasuresFoundProvidedDraftOnly() throws Exception {
    when(measureService.getAllActiveMeasureIds(eq(true))).thenReturn(List.of());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/admin/measures/test-cases/validations?draftOnly=true")
                .with(csrf())
                .with(user(TEST_USER_ID))
                .header(ADMIN_TEST_API_KEY_HEADER, ADMIN_TEST_API_KEY_HEADER_VALUE)
                .header("Authorization", "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reports", empty()))
        .andExpect(jsonPath("$.impactedMeasures", empty()));
    verifyNoInteractions(testCaseService);
  }

  @Test
  public void testValidateAllMeasureTestCasesNoMeasuresFoundProvidedNotDraftOnly()
      throws Exception {
    when(measureService.getAllActiveMeasureIds(eq(false))).thenReturn(List.of());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/admin/measures/test-cases/validations?draftOnly=false")
                .with(csrf())
                .with(user(TEST_USER_ID))
                .header(ADMIN_TEST_API_KEY_HEADER, ADMIN_TEST_API_KEY_HEADER_VALUE)
                .header("Authorization", "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reports", empty()))
        .andExpect(jsonPath("$.impactedMeasures", empty()));
    verifyNoInteractions(testCaseService);
  }

  @Test
  public void testValidateAllMeasureTestCasesNoImpactedMeasureDefaultDraftOnly() throws Exception {
    when(measureService.getAllActiveMeasureIds(eq(true))).thenReturn(List.of("M1", "M2"));
    MeasureTestCaseValidationReport report1 =
        MeasureTestCaseValidationReport.builder()
            .measureId("M1")
            .jobStatus(JobStatus.COMPLETED)
            .measureSetId("MSet1")
            .testCaseValidationReport(
                TestCaseValidationReport.builder()
                    .testCaseId("TC1")
                    .previousValidResource(true)
                    .currentValidResource(true)
                    .build())
            .testCaseValidationReport(
                TestCaseValidationReport.builder()
                    .testCaseId("TC2")
                    .previousValidResource(false)
                    .currentValidResource(false)
                    .build())
            .build();
    when(testCaseService.updateTestCaseValidResourcesWithReport(eq("M1"), anyString()))
        .thenReturn(report1);

    MeasureTestCaseValidationReport report2 =
        MeasureTestCaseValidationReport.builder()
            .measureId("M2")
            .jobStatus(JobStatus.COMPLETED)
            .measureSetId("MSet2")
            .testCaseValidationReport(
                TestCaseValidationReport.builder()
                    .testCaseId("TC3")
                    .previousValidResource(true)
                    .currentValidResource(true)
                    .build())
            .testCaseValidationReport(
                TestCaseValidationReport.builder()
                    .testCaseId("TC4")
                    .previousValidResource(true)
                    .currentValidResource(true)
                    .build())
            .build();
    when(testCaseService.updateTestCaseValidResourcesWithReport(eq("M2"), anyString()))
        .thenReturn(report2);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/admin/measures/test-cases/validations")
                .with(csrf())
                .with(user(TEST_USER_ID))
                .header(ADMIN_TEST_API_KEY_HEADER, ADMIN_TEST_API_KEY_HEADER_VALUE)
                .header("Authorization", "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reports[0].measureId").value("M1"))
        .andExpect(jsonPath("$.reports[0].measureSetId").value("MSet1"))
        .andExpect(jsonPath("$.reports[0].jobStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.reports[0].testCaseValidationReports[0]").exists())
        .andExpect(jsonPath("$.reports[0].testCaseValidationReports[0].testCaseId").value("TC1"))
        .andExpect(
            jsonPath("$.reports[0].testCaseValidationReports[0].previousValidResource").value(true))
        .andExpect(
            jsonPath("$.reports[0].testCaseValidationReports[0].currentValidResource").value(true))
        .andExpect(jsonPath("$.reports[0].testCaseValidationReports[1]").exists())
        .andExpect(jsonPath("$.reports[0].testCaseValidationReports[1].testCaseId").value("TC2"))
        .andExpect(
            jsonPath("$.reports[0].testCaseValidationReports[1].previousValidResource")
                .value(false))
        .andExpect(
            jsonPath("$.reports[0].testCaseValidationReports[1].currentValidResource").value(false))
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[0]").exists())
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[0].testCaseId").value("TC3"))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[0].previousValidResource").value(true))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[0].currentValidResource").value(true))
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[1]").exists())
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[1].testCaseId").value("TC4"))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[1].previousValidResource").value(true))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[1].currentValidResource").value(true))
        .andExpect(jsonPath("$.impactedMeasures", empty()));

    verify(testCaseService, times(1)).updateTestCaseValidResourcesWithReport(eq("M1"), anyString());
    verify(testCaseService, times(1)).updateTestCaseValidResourcesWithReport(eq("M2"), anyString());
    verifyNoInteractions(measureSetService);
  }

  @Test
  public void testValidateAllMeasureTestCasesOneImpactedMeasureDefaultDraftOnly() throws Exception {
    when(measureService.getAllActiveMeasureIds(eq(true))).thenReturn(List.of("M1", "M2"));
    MeasureTestCaseValidationReport report1 =
        MeasureTestCaseValidationReport.builder()
            .measureId("M1")
            .jobStatus(JobStatus.COMPLETED)
            .measureSetId("MSet1")
            .testCaseValidationReport(
                TestCaseValidationReport.builder()
                    .testCaseId("TC1")
                    .previousValidResource(true)
                    .currentValidResource(true)
                    .build())
            .testCaseValidationReport(
                TestCaseValidationReport.builder()
                    .testCaseId("TC2")
                    .previousValidResource(false)
                    .currentValidResource(false)
                    .build())
            .build();
    when(testCaseService.updateTestCaseValidResourcesWithReport(eq("M1"), anyString()))
        .thenReturn(report1);

    MeasureTestCaseValidationReport report2 =
        MeasureTestCaseValidationReport.builder()
            .measureId("M2")
            .jobStatus(JobStatus.COMPLETED)
            .measureSetId("MSet2")
            .testCaseValidationReport(
                TestCaseValidationReport.builder()
                    .testCaseId("TC3")
                    .previousValidResource(true)
                    .currentValidResource(false)
                    .build())
            .testCaseValidationReport(
                TestCaseValidationReport.builder()
                    .testCaseId("TC4")
                    .previousValidResource(true)
                    .currentValidResource(true)
                    .build())
            .testCaseValidationReport(
                TestCaseValidationReport.builder()
                    .testCaseId("TC5")
                    .previousValidResource(true)
                    .currentValidResource(false)
                    .build())
            .build();
    when(testCaseService.updateTestCaseValidResourcesWithReport(eq("M2"), anyString()))
        .thenReturn(report2);
    when(measureSetService.findByMeasureSetId(eq("MSet2")))
        .thenReturn(MeasureSet.builder().owner("Owner12").build());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/admin/measures/test-cases/validations")
                .with(csrf())
                .with(user(TEST_USER_ID))
                .header(ADMIN_TEST_API_KEY_HEADER, ADMIN_TEST_API_KEY_HEADER_VALUE)
                .header("Authorization", "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reports[0].measureId").value("M1"))
        .andExpect(jsonPath("$.reports[0].measureSetId").value("MSet1"))
        .andExpect(jsonPath("$.reports[0].jobStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.reports[0].testCaseValidationReports[0]").exists())
        .andExpect(jsonPath("$.reports[0].testCaseValidationReports[0].testCaseId").value("TC1"))
        .andExpect(
            jsonPath("$.reports[0].testCaseValidationReports[0].previousValidResource").value(true))
        .andExpect(
            jsonPath("$.reports[0].testCaseValidationReports[0].currentValidResource").value(true))
        .andExpect(jsonPath("$.reports[0].testCaseValidationReports[1]").exists())
        .andExpect(jsonPath("$.reports[0].testCaseValidationReports[1].testCaseId").value("TC2"))
        .andExpect(
            jsonPath("$.reports[0].testCaseValidationReports[1].previousValidResource")
                .value(false))
        .andExpect(
            jsonPath("$.reports[0].testCaseValidationReports[1].currentValidResource").value(false))
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[0]").exists())
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[0].testCaseId").value("TC3"))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[0].previousValidResource").value(true))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[0].currentValidResource").value(false))
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[1]").exists())
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[1].testCaseId").value("TC4"))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[1].previousValidResource").value(true))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[1].currentValidResource").value(true))
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[2]").exists())
        .andExpect(jsonPath("$.reports[1].testCaseValidationReports[2].testCaseId").value("TC5"))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[2].previousValidResource").value(true))
        .andExpect(
            jsonPath("$.reports[1].testCaseValidationReports[2].currentValidResource").value(false))
        .andExpect(jsonPath("$.impactedMeasures[0]").exists())
        .andExpect(jsonPath("$.impactedMeasures[0].measureId").value("M2"))
        .andExpect(jsonPath("$.impactedMeasures[0].measureSetId").value("MSet2"))
        .andExpect(jsonPath("$.impactedMeasures[0].measureOwner").value("Owner12"))
        .andExpect(jsonPath("$.impactedMeasures[0].impactedTestCasesCount").value(2))
        .andExpect(jsonPath("$.impactedMeasures[1]").doesNotExist());

    verify(testCaseService, times(1)).updateTestCaseValidResourcesWithReport(eq("M1"), anyString());
    verify(testCaseService, times(1)).updateTestCaseValidResourcesWithReport(eq("M2"), anyString());
  }

  @Test
  public void testAdminMeasurePermaDelete() throws Exception {
    Measure testMsr = Measure.builder().id("12345").build();
    when(measureService.findMeasureById(anyString())).thenReturn(testMsr);
    doNothing().when(measureRepository).delete(any(Measure.class));

    mockMvc.perform(
        MockMvcRequestBuilders.delete("/admin/measures/{id}", "12345")
            .with(csrf())
            .with(user(TEST_USER_ID))
            .header(ADMIN_TEST_API_KEY_HEADER, ADMIN_TEST_API_KEY_HEADER_VALUE)
            .header("Authorization", "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo("12345")));
  }

  @Test
  public void testAdminMeasureDeleteThrowsWhenMeasureNotFound() throws Exception {
    when(measureService.findMeasureById(anyString())).thenReturn(null);

    mockMvc.perform(
            MockMvcRequestBuilders.delete("/admin/measures/{id}", "12345")
                .with(csrf())
                .with(user(TEST_USER_ID))
                .header(ADMIN_TEST_API_KEY_HEADER, ADMIN_TEST_API_KEY_HEADER_VALUE)
                .header("Authorization", "test-okta"))
        .andExpect(status().isNotFound());
  }

  @Test
  public void testBlocksNonAuthorizedDeleteRequests() throws Exception {
    mockMvc.perform(
        MockMvcRequestBuilders.delete("/admin/measures/{id}", "12345")
            .with(csrf())
            .with(user(TEST_USER_ID))
            .header("Authorization", "test-okta"))
        .andExpect(status().isForbidden());
  }
}
