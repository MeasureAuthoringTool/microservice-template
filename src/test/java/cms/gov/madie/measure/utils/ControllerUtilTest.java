package cms.gov.madie.measure.utils;

import cms.gov.madie.measure.exceptions.UnauthorizedException;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControllerUtilTest {

  //  @Test
  //  public void testVerifyAuthorizationThrowsExceptionForDifferentUsers() {
  //    assertThrows(
  //        UnauthorizedException.class, () -> measureService.verifyAuthorization("user1",
  // measure));
  //  }
  //
  //  @Test
  //  public void testVerifyAuthorizationPassesForSharedUser() throws Exception {
  //    AclSpecification acl = new AclSpecification();
  //    acl.setUserId("userTest");
  //    acl.setRoles(List.of(RoleEnum.SHARED_WITH));
  //    measure.setAcls(List.of(acl));
  //    assertDoesNotThrow(() -> measureService.verifyAuthorization("userTest", measure));
  //  }

}
