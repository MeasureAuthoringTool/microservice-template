package cms.gov.madie.measure.resources;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "logging")
public class LoggingController {

  @GetMapping("/getlogger")
  public ResponseEntity<String> test(@RequestBody String message) {
    log.info(message);
    return ResponseEntity.status(HttpStatus.CREATED).body("get test message");
  }

  @PostMapping("/log/login")
  public ResponseEntity<String> loginLog(@RequestBody String message) {
    log.info("loginLog(): " + message);
    return ResponseEntity.status(HttpStatus.OK).body("logged login message: " + message);
  }

  @PostMapping("/log/logout")
  public ResponseEntity<String> logoutLog(@RequestBody String message) {
    log.info("logoutLog(): " + message);
    return ResponseEntity.status(HttpStatus.OK).body("logged logout message: " + message);
  }
}
