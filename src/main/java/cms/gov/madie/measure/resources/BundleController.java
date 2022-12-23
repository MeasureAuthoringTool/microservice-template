package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.InvalidResourceBundleStateException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.BundleService;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BundleController {

  private final MeasureRepository measureRepository;

  private final BundleService bundleService;

  @GetMapping(path = "/measures/{measureId}/bundles", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getMeasureBundle(
      @PathVariable String measureId,
      Principal principal,
      @RequestHeader("Authorization") String accessToken) {
    Optional<Measure> measureOptional = measureRepository.findById(measureId);
    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", measureId);
    }
    Measure measure = measureOptional.get();
    if (!principal.getName().equalsIgnoreCase(measure.getCreatedBy())
        && (CollectionUtils.isEmpty(measure.getAcls())
            || !measure.getAcls().stream()
                .anyMatch(
                    acl ->
                        acl.getUserId().equalsIgnoreCase(principal.getName())
                            && acl.getRoles().stream()
                                .anyMatch(role -> role.equals(RoleEnum.SHARED_WITH))))) {
      throw new UnauthorizedException("Measure", measureId, principal.getName());
    }
    if (measure.isCqlErrors()) {
      throw new InvalidResourceBundleStateException(
          "Measure", measureId, "since CQL errors exist.");
    }
    if (CollectionUtils.isEmpty(measure.getGroups())) {
      throw new InvalidResourceBundleStateException(
          "Measure", measureId, "since there are no associated measure groups.");
    }
    if (measure.getElmJson() == null || StringUtils.isBlank(measure.getElmJson())) {
      throw new InvalidResourceBundleStateException(
          "Measure", measureId, "since there are issues with the CQL.");
    }
    return ResponseEntity.ok(bundleService.bundleMeasure(measure, accessToken));
  }
}
