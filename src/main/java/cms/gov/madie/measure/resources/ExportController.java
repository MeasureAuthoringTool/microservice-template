package cms.gov.madie.measure.resources;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.MeasureBundleService;
import gov.cms.madie.models.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/measure-bundle")
public class ExportController {

  private final MeasureRepository measureRepository;
  private final MeasureBundleService measureBundleService;

  @PostMapping("/measures/{id}")
  public ResponseEntity<String> generateExports(
      Principal principal, @PathVariable("id") String id) {
    final String username = principal.getName();
    log.info("User [{}] is attempting to export measure [{}]", username, id);

    Optional<Measure> measureOptional = measureRepository.findById(id);
    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", id);
    }
    //{eCQM Abbreviated Title}v{MeasureVersion}-{Modelfamily}.json (FHIR Bundle)
    String zipFileName =
        measureOptional.get().getEcqmTitle()
            + "-v"
            + measureOptional.get().getVersion()
            + "-"
            + measureOptional.get().getModel();

    try {
      measureBundleService.zipFile(zipFileName, measureOptional.get());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return new ResponseEntity<>(HttpStatus.GATEWAY_TIMEOUT);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
