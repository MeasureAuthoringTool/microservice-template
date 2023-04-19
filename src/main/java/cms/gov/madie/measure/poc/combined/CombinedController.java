package cms.gov.madie.measure.poc.combined;

import cms.gov.madie.measure.poc.combined.model.Measure;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CombinedController {

  @PostMapping("/combined")
  public ResponseEntity<Measure> addMeasure(
      @RequestBody @Validated Measure measure, @RequestHeader("Authorization") String accessToken) {
    return ResponseEntity.status(HttpStatus.CREATED).body(measure);
  }
}
