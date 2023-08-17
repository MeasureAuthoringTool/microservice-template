package cms.gov.madie.measure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;

public final class QiCoreJsonUtil {
  private QiCoreJsonUtil() {}

  public static boolean isValidJson(String json) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      mapper.readTree(json);
      return true;
    } catch (JsonProcessingException e) {
      // do nothing
    }
    return false;
  }

  public static String getPatientId(String json) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode jsonNode = mapper.readTree(json);
    JsonNode entry = jsonNode.get("entry");
    JsonNode theNode = null;
    Iterator<JsonNode> entyIter = entry.iterator();
    String existingPatientId = null;
    while (entyIter.hasNext()) {
      theNode = entyIter.next();
      var resourceNode = theNode.get("resource");
      if (resourceNode != null) {
        var resourceType = resourceNode.get("resourceType");
        if (resourceType != null && "PATIENT".equalsIgnoreCase(resourceType.asText())) {
          existingPatientId = resourceNode.get("id").textValue();
        }
      }
    }
    return existingPatientId;
  }

  public static String getPatientFullUrl(String json) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode jsonNode = mapper.readTree(json);
    JsonNode entry = jsonNode.get("entry");
    JsonNode theNode = null;
    Iterator<JsonNode> entyIter = entry.iterator();
    String fullUrl = null;
    while (entyIter.hasNext()) {
      theNode = entyIter.next();
      var resourceNode = theNode.get("resource");
      if (resourceNode != null) {
        var resourceType = resourceNode.get("resourceType");
        if (resourceType != null
            && "PATIENT".equalsIgnoreCase(resourceType.asText())
            && theNode.has("fullUrl")) {
          fullUrl = theNode.get("fullUrl").asText();
        }
      }
    }
    return fullUrl;
  }

  public static String updateFullUrl(
      final String fullUrl, final String oldPatientId, final String newPatientId) {
    if (!StringUtils.isBlank(fullUrl) && fullUrl.endsWith(oldPatientId)) {
      return fullUrl.substring(0, fullUrl.length() - oldPatientId.length()) + newPatientId;
    }
    return fullUrl;
  }

  public static String replacePatientRefs(String json, String oldPatientId, String newPatientId) {
    final String pattern = "(?i)(\"reference\"\\s*:\\s*\"Patient/" + oldPatientId + "\")";
    return json.replaceAll(pattern, "\"reference\": \"Patient/" + newPatientId + "\"");
  }

  public static String replacePatientRefs(String json, String newPatientId) {
    final String pattern = "(?i)(\"reference\"\\s*:\\s*\"Patient\\/[A-Za-z0-9\\-\\.]{1,64}\")";
    return json.replaceAll(pattern, "\"reference\": \"Patient/" + newPatientId + "\"");
  }

  public static String replaceFullUrlRefs(String json, String oldFullUrl, String newPatientId) {
    final String pattern = "(\"reference\"\\s*:\\s*\"" + Pattern.quote(oldFullUrl) + "\")";
    return json.replaceAll(pattern, "\"reference\": \"Patient/" + newPatientId + "\"");
  }

  public static boolean isUuid(String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (Exception exception) {
      // handle the case where string is not valid UUID
    }
    return false;
  }
}
