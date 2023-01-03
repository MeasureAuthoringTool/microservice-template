package cms.gov.madie.measure.resources;

import java.security.Principal;
import java.util.Optional;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.utils.ControllerUtil;
import cms.gov.madie.measure.utils.ExportFileNamesUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.ExportService;
import gov.cms.madie.models.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExportController {

  private final MeasureRepository measureRepository;

  private final ExportService exportService;

  private static final String CONTENT_DISPOSITION = "Content-Disposition";

  @GetMapping(path = "/measures/{id}/exports", produces = "application/zip")
  public ResponseEntity<StreamingResponseBody> generateExports(
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
    ControllerUtil.verifyAuthorization(username, measure);

    return ResponseEntity.ok()
        .header(CONTENT_DISPOSITION, "attachment;filename=\""
            + ExportFileNamesUtil.getExportFileName(measure) + ".zip\"")
        .contentType(MediaType.valueOf("application/zip"))
        .body(out -> exportService.zipFile(measureOptional.get(), accessToken, out));
  }
}
