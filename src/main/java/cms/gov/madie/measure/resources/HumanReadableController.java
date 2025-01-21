package cms.gov.madie.measure.resources;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.services.HumanReadableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HumanReadableController {

  private final HumanReadableService humanReadableService;

  @GetMapping("/humanreadable/{id}")
  public ResponseEntity<String> getHumanReadableWithCSS(
      @PathVariable("id") String id,
      Principal principal,
      @RequestHeader("Authorization") String accessToken)
      throws Exception {
    final String username = principal.getName();
    log.info(
        "User [{}] is attempting to get human readable with CSS for measure [{}]", username, id);
    return ResponseEntity.ok(
        humanReadableService.getHumanReadableWithCSS(id, username, accessToken));
  }
}
