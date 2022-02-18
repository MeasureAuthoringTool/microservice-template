package cms.gov.madie.measure.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import io.micrometer.core.instrument.util.StringUtils;

public class UserInputSanitizeUtil {

  public static String sanitizeUserInput(String input) {
    if (StringUtils.isNotBlank(input)) {
      input = Jsoup.clean(input, Safelist.basic());
      if (StringUtils.isNotBlank(input)) {
        input = input.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
      }
    }
    return input;
  }
}
