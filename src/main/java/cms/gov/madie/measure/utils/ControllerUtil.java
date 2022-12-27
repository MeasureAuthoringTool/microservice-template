package cms.gov.madie.measure.utils;

import cms.gov.madie.measure.exceptions.UnauthorizedException;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.Measure;
import org.springframework.util.CollectionUtils;

public class ControllerUtil {
  public static final String TEST_CASES = "/measures/{measureId}/test-cases";

  public static void verifyAuthorization(String username, Measure measure) {
    if (!measure.getCreatedBy().equalsIgnoreCase(username)
        && (CollectionUtils.isEmpty(measure.getAcls())
            || !measure.getAcls().stream()
                .anyMatch(
                    acl ->
                        acl.getUserId().equalsIgnoreCase(username)
                            && acl.getRoles().stream()
                                .anyMatch(role -> role.equals(RoleEnum.SHARED_WITH))))) {
      throw new UnauthorizedException("Measure", measure.getId(), username);
    }
  }
}
