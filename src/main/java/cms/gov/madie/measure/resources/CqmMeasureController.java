package cms.gov.madie.measure.resources;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.services.CqmMeasureService;
import gov.cms.madie.models.cqm.CqmMeasure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CqmMeasureController {
  private final CqmMeasureService cqmMeasureService;

  @GetMapping("/measures/{id}/cqmmeasure")
  public ResponseEntity<CqmMeasure> getCqmMeasure(
      @PathVariable("id") String id, @RequestHeader("Authorization") String accessToken) {
    return ResponseEntity.ok(cqmMeasureService.getCqmMeasure(id, accessToken));
  }
}
