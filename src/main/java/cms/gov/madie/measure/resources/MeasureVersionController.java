package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.services.VersionService;
import gov.cms.madie.models.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import static cms.gov.madie.measure.services.VersionService.VersionValidationResult.TEST_CASE_ERROR;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/measures")
public class MeasureVersionController {

  private final VersionService versionService;

  @PutMapping("/{id}/version")
  public ResponseEntity<Measure> createVersion(
      @PathVariable("id") String id,
      @RequestParam String versionType,
      Principal principal,
      @RequestHeader("Authorization") String accessToken)
      throws Exception {
    return ResponseEntity.ok(
        versionService.createVersion(id, versionType, principal.getName(), accessToken));
  }

  @GetMapping("/{id}/version")
  public ResponseEntity<Void> checkValidVersion(
      @PathVariable("id") String id,
      @RequestParam String versionType,
      Principal principal,
      @RequestHeader("Authorization") String accessToken) {
    var validationResult =
        versionService.checkValidVersioning(id, versionType, principal.getName(), accessToken);
    if (validationResult == TEST_CASE_ERROR) {
      return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/{id}/draft")
  public ResponseEntity<Measure> createDraft(
      @PathVariable("id") String id, @RequestBody final Measure measure, Principal principal) {
    if (StringUtils.isBlank(measure.getMeasureName())) {
      throw new InvalidIdException("Measure name is required.");
    }
    var output = versionService.createDraft(id, measure.getMeasureName(), principal.getName());
    return ResponseEntity.status(HttpStatus.CREATED).body(output);
  }
}
