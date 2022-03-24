package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.MeasureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/measure-transfer")
public class MeasureTransferController {
  private static String HARP_ID_HEADER = "harp-id";
  @Autowired private final MeasureRepository repository;
  @Autowired private final MeasureService measureService;

  @PostMapping("/mat-measures")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<Measure> createMeasure(
      HttpServletRequest request,
      @RequestBody @Validated({Measure.ValidationSequence.class}) Measure measure,
      @Value("${lambda-api-key}") String apiKey) {
    String harpId= request.getHeader(HARP_ID_HEADER);
    log.info("Measure [{}] is being transferred over to MADiE by [{}]", measure.getMeasureName(), harpId);
    measureService.checkDuplicateCqlLibraryName(measure.getCqlLibraryName());

    // TODO: decide on audit records
    Instant now = Instant.now();
    measure.setCreatedAt(now);
    measure.setLastModifiedAt(now);
    Measure savedMeasure = repository.save(measure);
    log.info("Measure [{}] transfer complete", measure.getMeasureName());
    return ResponseEntity.status(HttpStatus.CREATED).body(savedMeasure);
  }
}
