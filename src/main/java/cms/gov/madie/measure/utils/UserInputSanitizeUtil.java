package cms.gov.madie.measure.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.nodes.Document;

import io.micrometer.core.instrument.util.StringUtils;

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
}
