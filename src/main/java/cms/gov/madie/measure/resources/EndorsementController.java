package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.repositories.EndorsementRepository;
import gov.cms.madie.models.common.EndorserOrganization;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EndorsementController {
  private final EndorsementRepository endorsementsRepository;

  @Cacheable("endorsements")
  @GetMapping("/endorsements")
  public ResponseEntity<List<EndorserOrganization>> getAllEndorsements() {
    List<EndorserOrganization> endorsementsList = endorsementsRepository.findAll();
    if (CollectionUtils.isEmpty(endorsementsList)) {
      log.debug("No endorsement organizations are available");
      throw new IllegalStateException("No endorsement organizations are available");
    }
    return ResponseEntity.status(HttpStatus.OK).body(endorsementsList);
  }
}
