package cms.gov.madie.measure.resources;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.services.VersionService;
import gov.cms.madie.models.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/measures")
public class MeasureVersionController {

  private final VersionService versionService;

  @PutMapping("/version/{id}")
  public ResponseEntity<Measure> createVersion(
      @PathVariable("id") String id,
      @RequestParam String versionType,
      Principal principal,
      @RequestHeader("Authorization") String accessToken) {
    return ResponseEntity.ok(
        versionService.createVersion(id, versionType, principal.getName(), accessToken));
  }
}
