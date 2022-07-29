package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.repositories.PopulationBasisRepository;
import gov.cms.madie.models.common.PopulationBasis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UtilsController {

  @Autowired private PopulationBasisRepository populationBasisRepository;

  @Cacheable("populationBasisValues")
  @GetMapping("/populationBasisValues")
  public ResponseEntity<List<String>> getAllPopulationBasisValues() {
    // add caching also log some debug
    List<PopulationBasis> populationBasisList = populationBasisRepository.findAll();
    if (populationBasisList.isEmpty()) {
      log.debug("No Population Basis values are available");
      throw new RuntimeException("No Population Basis values are available");
    }
    List<String> populationBasisValues =
        populationBasisList.stream().map(PopulationBasis::getPopulationBasisValue).toList();
    return ResponseEntity.status(HttpStatus.OK).body(populationBasisValues);
  }
}
