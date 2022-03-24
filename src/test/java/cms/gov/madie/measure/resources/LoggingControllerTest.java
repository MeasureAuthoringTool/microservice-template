package cms.gov.madie.measure.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class LoggingControllerTest {

  @InjectMocks private LoggingController controller;

  public static final String USER_INFO =
      "{\"sub\":\"testSub\",\"email\":\"test.user@test.com\",\"email_verified\":true}";

  @Test
  void testLoginLogSuccess() {
    ResponseEntity<String> response = controller.loginLog(USER_INFO);

    assertNotNull(response.getBody());
    assertEquals(response.getStatusCode(), HttpStatus.OK);
    assertTrue(response.getBody().contains(USER_INFO));
  }

  @Test
  void testLoginLogBadRequest() {
    ResponseEntity<String> response = controller.loginLog(null);

    assertNotNull(response.getBody());
    assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
    assertTrue(response.getBody().contains(HttpStatus.BAD_REQUEST.toString()));
  }

  @Test
  void testLogoutLogSuccess() {
    ResponseEntity<String> response = controller.logoutLog(USER_INFO);

    assertNotNull(response.getBody());
    assertEquals(response.getStatusCode(), HttpStatus.OK);
    assertTrue(response.getBody().contains(USER_INFO));
  }

  @Test
  void testLogoutLogBadRequest() {
    ResponseEntity<String> response = controller.logoutLog(null);

    assertNotNull(response.getBody());
    assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
    assertTrue(response.getBody().contains(HttpStatus.BAD_REQUEST.toString()));
  }
}
