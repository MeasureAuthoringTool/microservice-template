package cms.gov.madie.measure.resources;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cms.gov.madie.measure.exceptions.HarpIdMismatchException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.InvalidRequestException;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import cms.gov.madie.measure.exceptions.MeasureNotDraftableException;
import cms.gov.madie.measure.repositories.CqmMeasureRepository;
import cms.gov.madie.measure.repositories.ExportRepository;
import cms.gov.madie.measure.services.*;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.cqm.CqmMeasure;
import gov.cms.madie.models.measure.Export;
import org.apache.commons.collections4.CollectionUtils;
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
import cms.gov.madie.measure.repositories.MeasureRepository;
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
  private final VersionService versionService;

  private final MeasureRepository measureRepository;
  private final ExportRepository exportRepository;
  private final CqmMeasureRepository cqmMeasureRepository;

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
      @RequestHeader(name = "harpId") String harpId,
      @PathVariable String id) {

    Measure measureToDelete = measureService.findMeasureById(id);

    if (measureToDelete != null) {
      if (!measureToDelete.getMeasureSet().getOwner().equals(harpId)) {
        throw new HarpIdMismatchException(
            harpId, measureToDelete.getMeasureSet().getOwner(), measureToDelete.getId());
      }

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
      @RequestHeader(name = "harpId") String harpId,
      @RequestParam(required = true, name = "measureids") String measureids) {

    List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
    String[] ids = StringUtils.split(measureids, ",");
    for (String id : ids) {
      Measure measureToGet = measureService.findMeasureById(id);
      if (measureToGet != null) {
        if (!measureToGet.getMeasureSet().getOwner().equals(harpId)) {
          throw new HarpIdMismatchException(
              harpId, measureToGet.getMeasureSet().getOwner(), measureToGet.getId());
        }

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

  @PutMapping("/measures/{id}")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<Measure> correctMeasureVersion(
      HttpServletRequest request,
      @Value("${admin-api-key}") String apiKey,
      @RequestHeader(name = "harpId") String harpId,
      Principal principal,
      @PathVariable String id,
      @RequestParam String inCorrectVersion,
      @RequestParam String correctVersion,
      @RequestParam String draftVersion) {
    Measure measureToCorrectVersion = measureService.findMeasureById(id);
    if (measureToCorrectVersion == null
        || !measureToCorrectVersion.getVersion().toString().equals(inCorrectVersion)) {
      String error =
          String.format(
              "Could not find Measure with id of %s and / or a version of %s",
              id, inCorrectVersion);
      throw new ResourceNotFoundException(error);
    }

    if (!measureToCorrectVersion.getMeasureSet().getOwner().equals(harpId)) {
      throw new HarpIdMismatchException(
          harpId,
          measureToCorrectVersion.getMeasureSet().getOwner(),
          measureToCorrectVersion.getId());
    }

    // check if the associated measure set already has a draft
    List<Measure> relatedMeasures =
        measureRepository.findAllByMeasureSetIdInAndActiveAndMeasureMetaDataDraft(
            List.of(measureToCorrectVersion.getMeasureSetId()), true, true);

    if (!CollectionUtils.isEmpty(relatedMeasures)
        && !relatedMeasures.get(0).getId().equals(measureToCorrectVersion.getId())) {
      throw new MeasureNotDraftableException(measureToCorrectVersion.getId());
    }

    // check if the draftVersion is less than correctVersion
    if (!isLessThan(correctVersion, draftVersion)) {
      throw new InvalidRequestException("Draft version should be always less than correct version");
    }

    // check if the given version is already associated
    if (!checkIfVersionIsAlreadyAssociated(
        measureToCorrectVersion.getMeasureSetId(), correctVersion, draftVersion)) {
      throw new InvalidResourceStateException(
          "Version number cannot be corrected. "
              + "The given draft or correct version number is already associated");
    }

    Version newDraftVersion = Version.parse(draftVersion);
    String newCql =
        measureToCorrectVersion
            .getCql()
            .replace(
                versionService.generateLibraryContentLine(
                    measureToCorrectVersion.getCqlLibraryName(),
                    measureToCorrectVersion.getVersion()),
                versionService.generateLibraryContentLine(
                    measureToCorrectVersion.getCqlLibraryName(), newDraftVersion));

    measureToCorrectVersion.setCql(newCql);
    measureToCorrectVersion.setVersion(newDraftVersion);
    measureToCorrectVersion.getMeasureMetaData().setDraft(true);

    deleteRelevantPackageData(id, measureToCorrectVersion);

    Measure correctedVersionMeasure = measureRepository.save(measureToCorrectVersion);
    actionLogService.logAction(id, Measure.class, ActionType.UPDATED, principal.getName());
    return ResponseEntity.ok(correctedVersionMeasure);
  }

  private void deleteRelevantPackageData(String id, Measure measureToCorrectVersion) {
    // QI-Core measure: delete the export
    // QDM Measures: delete the export and cqmMeasure
    Export export = exportRepository.findByMeasureId(id).orElse(null);
    if (export != null) {
      exportRepository.delete(export);
    }

    if (ModelType.QDM_5_6.getValue().equals(measureToCorrectVersion.getModel())) {
      CqmMeasure cqmMeasure =
          cqmMeasureRepository.findByHqmfSetIdAndHqmfVersionNumber(
              measureToCorrectVersion.getMeasureSetId(), measureToCorrectVersion.getVersionId());
      if (cqmMeasure != null) {
        cqmMeasureRepository.delete(cqmMeasure);
      }
    }
  }

  private boolean checkIfVersionIsAlreadyAssociated(
      String measureSetId, String correctVersion, String draftVersion) {
    List<Measure> allByMeasureSetIdAndActive =
        measureRepository.findAllByMeasureSetIdAndActive(measureSetId, true);
    List<Measure> measureStream =
        allByMeasureSetIdAndActive.stream()
            .filter(
                measure ->
                    measure.getVersion().toString().equals(correctVersion)
                        || measure.getVersion().toString().equals(draftVersion))
            .toList();
    return CollectionUtils.isEmpty(measureStream);
  }

  private boolean isLessThan(String correctVersion, String draftVersion) {
    String[] correctVersionParts = correctVersion.split("\\.");
    String[] draftVersionParts = draftVersion.split("\\.");

    int length = Math.max(correctVersionParts.length, draftVersionParts.length);

    for (int i = 0; i < length; i++) {
      // parse the parts as integers for comparison. If a part is missing, treat it as zero.
      int correctVersionPart =
          i < correctVersionParts.length ? Integer.parseInt(correctVersionParts[i]) : 0;
      int draftVersionPart =
          i < draftVersionParts.length ? Integer.parseInt(draftVersionParts[i]) : 0;

      //  compare corresponding parts of the version strings
      if (draftVersionPart < correctVersionPart) {
        return true;
      } else if (draftVersionPart > correctVersionPart) {
        return false;
      }
    }
    // if all parts are equal, draftVersion is not less than correctVersion
    return false;
  }

  private Callable<MeasureTestCaseValidationReport> buildCallableForMeasureId(
      final String measureId, final String accessToken) {
    return () -> testCaseService.updateTestCaseValidResourcesWithReport(measureId, accessToken);
  }
}
