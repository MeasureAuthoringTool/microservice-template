package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.services.SequenceGenerationService;
import gov.cms.madie.models.measure.MeasureSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GeneratorController {
  private final SequenceGenerationService sequenceGenerationService;

  @PostMapping("/sequence/increment/{sequenceName}/{measureSetId}")
  public ResponseEntity<MeasureSet> incrementSequence(
      @RequestBody @PathVariable String sequenceName) {
    MeasureSet updatedMeasureSet = sequenceGenerationService.incrementSequence(sequenceName);
    return ResponseEntity.status(HttpStatus.CREATED).body(updatedMeasureSet);
  }
}
