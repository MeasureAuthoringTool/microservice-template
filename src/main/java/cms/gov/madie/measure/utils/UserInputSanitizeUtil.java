package cms.gov.madie.measure.utils;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.nodes.Document;

import java.util.List;

public class UserInputSanitizeUtil {

  public static String sanitizeUserInput(String input) {
    String sanitized = input;
    if (StringUtils.isNotBlank(input)) {
      sanitized =
          Jsoup.clean(
              input, "", Safelist.basic(), new Document.OutputSettings().prettyPrint(false));
      if (StringUtils.isNotBlank(sanitized)) {
        sanitized =
            sanitized.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
      }
    }
    return sanitized;
  }

  public static List<String> sanitizeUserInput(List<String> input) {
    input.replaceAll(UserInputSanitizeUtil::sanitizeUserInput);
    return input;
  }
}
