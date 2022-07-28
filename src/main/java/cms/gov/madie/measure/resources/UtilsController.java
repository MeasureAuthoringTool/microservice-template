package cms.gov.madie.measure.resources;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UtilsController {

  @GetMapping("/populationBasisValues")
  public ResponseEntity<List<String>> getAllPopulationBasisValues() {

    return ResponseEntity.ok(List.of("string"));
  }
}
