package cms.gov.madie.measure.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import cms.gov.madie.measure.services.HumanReadableService;

@ExtendWith(MockitoExtension.class)
public class HumanReadableControllerTest {

  @Mock private HumanReadableService humanReadableService;
  @InjectMocks private HumanReadableController humanReadableController;

  @Test
  public void testGetHumanReadableWithCSS() throws Exception {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("testUser");

    when(humanReadableService.getHumanReadableWithCSS(anyString(), anyString(), anyString()))
        .thenReturn("test human readable");

    ResponseEntity<String> result =
        humanReadableController.getHumanReadableWithCSS(
            "test measure id", principal, "test access token");

    assertEquals("test human readable", result.getBody());
  }
}
