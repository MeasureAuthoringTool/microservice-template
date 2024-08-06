package cms.gov.madie.measure.resources;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.dto.ImpactedMeasureValidationReport;
import cms.gov.madie.measure.dto.MeasureTestCaseValidationReport;
import cms.gov.madie.measure.dto.MeasureTestCaseValidationReportSummary;
import cms.gov.madie.measure.dto.TestCaseValidationReport;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.MeasureService;
import cms.gov.madie.measure.services.MeasureSetService;
import cms.gov.madie.measure.services.TestCaseService;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
  private final MeasureService measureService;
  private final TestCaseService testCaseService;
  private final MeasureSetService measureSetService;
  private final ActionLogService actionLogService;

  private final MeasureRepository measureRepository;

  @Value("${madie.admin.concurrency-limit}")
  private int concurrencyLimit;

  @PutMapping("/measures/test-cases/validations")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<MeasureTestCaseValidationReportSummary> validateAllMeasureTestCases(
      HttpServletRequest request,
      @Value("${admin-api-key}") String apiKey,
      Principal principal,
      @RequestHeader("Authorization") String accessToken,
      @RequestParam(name = "draftOnly", defaultValue = "true") boolean draftOnly)
      throws InterruptedException, ExecutionException {
    log.info("User [{}] - Starting admin task [validateAllMeasureTestCases]", principal.getName());
    StopWatch timer = new StopWatch();
    timer.start();
    List<String> measureIds = measureService.getAllActiveMeasureIds(draftOnly);
    List<Callable<MeasureTestCaseValidationReport>> tasks = new ArrayList<>();
    List<MeasureTestCaseValidationReport> reports = new ArrayList<>();
    List<ImpactedMeasureValidationReport> impactedMeasures = new ArrayList<>();

    log.info("User [{}] - Building callable tasks for [] measures", measureIds.size());
    for (String measureId : measureIds) {
      tasks.add(buildCallableForMeasureId(measureId, accessToken));
    }

    ExecutorService executorService = Executors.newFixedThreadPool(concurrencyLimit);
    List<Future<MeasureTestCaseValidationReport>> futures = executorService.invokeAll(tasks);
    executorService.shutdown();

    for (Future<MeasureTestCaseValidationReport> f : futures) {
      MeasureTestCaseValidationReport measureTestCaseValidationReport = f.get();
      reports.add(measureTestCaseValidationReport);

      int diffCount = 0;
      for (TestCaseValidationReport tcValReport :
          measureTestCaseValidationReport.getTestCaseValidationReports()) {
        if (tcValReport.isPreviousValidResource() && !tcValReport.isCurrentValidResource()) {
          diffCount++;
        }
      }
      if (diffCount > 0) {
        MeasureSet measureSet =
            measureSetService.findByMeasureSetId(measureTestCaseValidationReport.getMeasureSetId());

        impactedMeasures.add(
            ImpactedMeasureValidationReport.builder()
                .measureId(measureTestCaseValidationReport.getMeasureId())
                .measureSetId(measureTestCaseValidationReport.getMeasureSetId())
                .measureVersionId(measureTestCaseValidationReport.getMeasureVersionId())
                .measureName(measureTestCaseValidationReport.getMeasureName())
                .measureOwner(measureSet.getOwner())
                .impactedTestCasesCount(diffCount)
                .build());
      }
    }

    timer.stop();
    log.info(
        "User [{}] - Admin task [validateAllMeasureTestCases] took [{}s] to complete",
        principal.getName(),
        timer.getTotalTimeSeconds());

    return ResponseEntity.ok(
        MeasureTestCaseValidationReportSummary.builder()
            .reports(reports)
            .impactedMeasures(impactedMeasures)
            .build());
  }

  @DeleteMapping("/measures/{id}")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<Measure> permDeleteMeasure(
      HttpServletRequest request,
      @Value("${admin-api-key}") String apiKey,
      Principal principal,
      @RequestHeader("Authorization") String accessToken,
      @PathVariable String id) {

    Measure measureToDelete = measureService.findMeasureById(id);
    if (measureToDelete != null) {
      measureRepository.delete(measureToDelete);
      actionLogService.logAction(id, Measure.class, ActionType.DELETED, principal.getName());
      return ResponseEntity.ok(measureToDelete);
    }
    throw new ResourceNotFoundException(id);
  }

  @GetMapping("/measures/sharedWith")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<List<Map<String, Object>>> getMeasureSharedWith(
      HttpServletRequest request,
      @Value("${admin-api-key}") String apiKey,
      Principal principal,
      @RequestHeader("Authorization") String accessToken,
      @RequestParam(required = true, name = "measureids") String measureids) {

    List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
    String[] ids = StringUtils.split(measureids, ",");
    for (String id : ids) {
      Measure measureToGet = measureService.findMeasureById(id);
      if (measureToGet != null) {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("measureName", measureToGet.getMeasureName());
        result.put("measureId", measureToGet.getId());
        result.put("measureOwner", measureToGet.getMeasureSet().getOwner());
        result.put("sharedWith", measureToGet.getMeasureSet().getAcls());

        results.add(result);
      } else {
        throw new ResourceNotFoundException(id);
      }
    }
    return ResponseEntity.ok(results);
  }

  private Callable<MeasureTestCaseValidationReport> buildCallableForMeasureId(
      final String measureId, final String accessToken) {
    return () -> testCaseService.updateTestCaseValidResourcesWithReport(measureId, accessToken);
  }
}
