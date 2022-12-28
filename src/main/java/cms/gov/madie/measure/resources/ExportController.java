package cms.gov.madie.measure.resources;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.utils.ControllerUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.ExportService;
import gov.cms.madie.models.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/measure-bundle")
public class ExportController {

  private final MeasureRepository measureRepository;
  private final ExportService measureBundleService;

  @PostMapping("/measures/{id}")
  public ResponseEntity<String> generateExports(
      Principal principal,
      @PathVariable("id") String id,
      @RequestHeader("Authorization") String accessToken) {

    final String username = principal.getName();
    log.info("User [{}] is attempting to export measure [{}]", username, id);

    Optional<Measure> measureOptional = measureRepository.findById(id);

    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", id);
    }

    Measure measure = measureOptional.get();
    ControllerUtil.verifyAuthorization(username, measure);

    try {
      measureBundleService.zipFile(measureOptional.get(), accessToken);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return new ResponseEntity<>(HttpStatus.GATEWAY_TIMEOUT);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
