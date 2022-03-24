package cms.gov.madie.measure.resources;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "logging")
public class LoggingController {

  @PostMapping("/log/login")
  public ResponseEntity<String> loginLog(@RequestBody String message) {
    log.info("loginLog(): " + message);
    if (StringUtils.isBlank(message)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HttpStatus.BAD_REQUEST.toString());
    }
    return ResponseEntity.status(HttpStatus.OK).body("logged login message: " + message);
  }

  @PostMapping("/log/logout")
  public ResponseEntity<String> logoutLog(@RequestBody String message) {
    log.info("logoutLog(): " + message);
    if (StringUtils.isBlank(message)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HttpStatus.BAD_REQUEST.toString());
    }
    return ResponseEntity.status(HttpStatus.OK).body("logged logout message: " + message);
  }
}
