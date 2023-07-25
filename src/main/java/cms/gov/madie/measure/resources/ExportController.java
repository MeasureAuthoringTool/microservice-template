package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.BundleService;
import cms.gov.madie.measure.services.FhirServicesClient;
import cms.gov.madie.measure.utils.ControllerUtil;
import cms.gov.madie.measure.utils.ExportFileNamesUtil;
import gov.cms.madie.models.measure.Measure;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExportController {

  private final MeasureRepository measureRepository;

  private final BundleService bundleService;

  private final FhirServicesClient fhirServicesClient;

  @GetMapping(path = "/measures/{id}/exports", produces = "application/zip")
  public ResponseEntity<byte[]> getZip(
      Principal principal,
      @PathVariable("id") String id,
      @RequestHeader("Authorization") String accessToken) {

    final String username = principal.getName();
    log.info("User [{}] is attempting to export measure [{}]", username, id);

    Optional<Measure> measureOptional = measureRepository.findById(id);

    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", id);
    }

    Measure measure = measureOptional.get();

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment;filename=\"" + ExportFileNamesUtil.getExportFileName(measure) + ".zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(bundleService.exportBundleMeasure(measure, accessToken));
  }

  @GetMapping(
      path = ControllerUtil.TEST_CASES + "/{testCaseId}/exports",
      produces = "application/zip")
  public ResponseEntity<byte[]> getTestCaseExport(
      Principal principal,
      @RequestHeader("Authorization") String accessToken,
      @PathVariable String measureId,
      @PathVariable String testCaseId) {

    final String username = principal.getName();
    log.info("User [{}] is attempting to export test case [{}]", username, testCaseId);

    Optional<Measure> measureOptional = measureRepository.findById(measureId);

    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", measureId);
    }

    Measure measure = measureOptional.get();

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment;filename=\""
                + ExportFileNamesUtil.getTestCaseExportZipName(measure)
                + ".zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(fhirServicesClient.getTestCaseExport(measure, accessToken, testCaseId));
  }

  @PutMapping(path = ControllerUtil.TEST_CASES + "/exports", produces = "application/zip")
  public ResponseEntity<byte[]> getTestCaseExport(
      Principal principal,
      @RequestHeader("Authorization") String accessToken,
      @PathVariable String measureId,
      @RequestBody List<String> testCaseId) {

    final String username = principal.getName();
    log.info("User [{}] is attempting to export test cases for [{}]", username, measureId);

    Optional<Measure> measureOptional = measureRepository.findById(measureId);

    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", measureId);
    }

    Measure measure = measureOptional.get();

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment;filename=\""
                + ExportFileNamesUtil.getTestCaseExportZipName(measure)
                + ".zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(fhirServicesClient.getTestCaseExports(measure, accessToken, testCaseId));
  }
}
