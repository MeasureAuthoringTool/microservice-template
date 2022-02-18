package cms.gov.madie.measure.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserInputSanitizeUtilTest {

  @Test
  public void testSanitizeUserInputWithEmptyString() {
    String input = "";
    String result = UserInputSanitizeUtil.sanitizeUserInput(input);

    assertEquals("", result);
  }

  @Test
  public void testSanitizeUserInputWithScript() {
    String input = "<script>alert(\"Let's play hide and seek!\")</script>";
    String result = UserInputSanitizeUtil.sanitizeUserInput(input);

    assertEquals("", result);
  }

  @Test
  public void testSanitizeUserInputWithScriptAttributes() {
    String input = "<b onmouseover=alert('Wufff!')>click me!</b>";
    String result = UserInputSanitizeUtil.sanitizeUserInput(input);

    assertEquals("<b>click me!</b>", result);
  }

  @Test
  public void testSanitizeUserInputWithCodeEncoding() {
    String input =
        "<META HTTP-EQUIV=\"refresh\" CONTENT=\"0;url=data:text/html;base64,PHNjcmlwdD5hbGVydCgndGVzdDMnKTwvc2NyaXB0Pg\">";
    String result = UserInputSanitizeUtil.sanitizeUserInput(input);

    assertEquals("", result);
  }

  @Test
  public void testSanitizeUserInputWithScriptViaEncodedUri() {
    String input = "<IMG SRC=j&#X41vascript:alert('test2')>";
    String result = UserInputSanitizeUtil.sanitizeUserInput(input);

    assertEquals("", result);
  }

  @Test
  public void testSanitizeUserInputWithNonMaliciousSymbols() {
    String input = "4 < A1C < 5 and med use > 3 &&";
    String result = UserInputSanitizeUtil.sanitizeUserInput(input);

    assertEquals("4 < A1C < 5 and med use > 3 &&", result);
  }
}
