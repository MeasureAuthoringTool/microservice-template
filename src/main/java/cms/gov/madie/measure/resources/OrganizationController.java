package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.repositories.OrganizationRepository;
import gov.cms.madie.models.common.Organization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OrganizationController {

  private static final String HARP_ID_HEADER = "harp-id";
  private final OrganizationRepository organizationRepository;

  @Cacheable("organizations")
  @GetMapping("/organizations")
  public ResponseEntity<List<Organization>> getAllOrganizations() {
    List<Organization> organizationList = organizationRepository.findAll();
    if (CollectionUtils.isEmpty(organizationList)) {
      log.debug("No organizations are available");
      throw new RuntimeException("No organizations are available");
    }
    return ResponseEntity.status(HttpStatus.OK).body(organizationList);
  }

  @PostMapping("/organizations")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<List<Organization>> addOrganizations(
      HttpServletRequest request,
      @RequestBody List<Organization> organizations,
      @Value("${organizations-api-key}") String apiKey) {
    final String userName = request.getHeader(HARP_ID_HEADER);
    log.info("User {} is attempting to add new organizations {}", userName, organizations);
    try {
      List<Organization> savedOrganizations = organizationRepository.saveAll(organizations);
      log.info("User {} successfully added new organizations", userName);
      return ResponseEntity.status(HttpStatus.CREATED).body(savedOrganizations);
    } catch (org.springframework.dao.DuplicateKeyException duplicateKeyException) {
      log.error("One of the organizations is already available with same OID");
      throw new DuplicateKeyException(
          duplicateKeyException.getLocalizedMessage(), "Duplicate oid found");
    }
  }
}
