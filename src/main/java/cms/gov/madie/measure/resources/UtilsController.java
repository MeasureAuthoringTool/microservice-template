package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.repositories.PopulationBasisRepository;
import gov.cms.madie.models.common.PopulationBasis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UtilsController {

  private final PopulationBasisRepository populationBasisRepository;

  @Cacheable("populationBasisValues")
  @GetMapping("/populationBasisValues")
  public ResponseEntity<List<String>> getAllPopulationBasisValues() {
    List<PopulationBasis> populationBasisList = populationBasisRepository.findAll();
    if (CollectionUtils.isEmpty(populationBasisList)) {
      log.debug("No Population Basis values are available");
      throw new RuntimeException("No Population Basis values are available");
    }
    List<String> populationBasisValues =
        populationBasisList.stream().map(PopulationBasis::getPopulationBasisValue).toList();
    return ResponseEntity.status(HttpStatus.OK).body(populationBasisValues);
  }
}
