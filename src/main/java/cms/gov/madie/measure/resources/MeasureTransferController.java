package cms.gov.madie.measure.resources;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.MeasureService;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/measure-transfer")
public class MeasureTransferController {
  private static final String HARP_ID_HEADER = "harp-id";
  private final MeasureService measureService;
  private final ActionLogService actionLogService;

  @PostMapping("/mat-measures")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<Measure> createMeasure(
      HttpServletRequest request,
      @RequestBody @Validated({Measure.ValidationSequence.class}) Measure measure,
      @RequestParam String cmsId,
      @Value("${lambda-api-key}") String apiKey) {
    String harpId = request.getHeader(HARP_ID_HEADER);
    log.info(
        "Measure [{}] is being transferred over to MADiE by [{}]",
        measure.getMeasureName(),
        harpId);

    Measure savedMeasure = measureService.importMatMeasure(measure, cmsId, apiKey, harpId);

    log.info("Measure [{}] transfer complete", measure.getMeasureName());
    actionLogService.logAction(
        savedMeasure.getId(), Measure.class, ActionType.IMPORTED, savedMeasure.getCreatedBy());
    return ResponseEntity.status(HttpStatus.CREATED).body(savedMeasure);
  }
}
