package cms.gov.madie.measure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
public final class QiCoreJsonUtil {

  private QiCoreJsonUtil() {}

  /**
   * Helper function to test if JSON is well-formed for purposes of being used as a resource. Null
   * input is treated as invalid, because it is not a valid resource.
   *
   * @param json
   * @return true if json is present and well-formed, false otherwise
   */
  public static boolean isValidJson(String json) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      mapper.readTree(json);
      return true;
    } catch (JsonProcessingException | IllegalArgumentException e) {
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

  public static String getPatientName(String json, String type) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode jsonNode = mapper.readTree(json);
    JsonNode entries = jsonNode.get("entry");
    if (entries != null) {
      for (JsonNode entry : entries) {
        var resourceNode = entry.get("resource");
        if (resourceNode != null) {
          var resourceType = resourceNode.get("resourceType");
          if (resourceType != null && "PATIENT".equalsIgnoreCase(resourceType.asText())) {
            JsonNode names = resourceNode.get("name");
            if (names != null) {
              for (JsonNode name : names) {
                if ("family".equalsIgnoreCase(type)) {
                  return name.get("family").asText();
                } else if ("given".equalsIgnoreCase(type)) {
                  JsonNode givenNames = name.get("given");
                  if (givenNames != null) {
                    for (JsonNode givenName : givenNames) {
                      return givenName.asText();
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return null;
  }

  public static List<TestCaseGroupPopulation> getTestCaseGroupPopulationsFromMeasureReport(
      String json) throws JsonProcessingException {
    List<TestCaseGroupPopulation> groupPopulations = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(json);
    JsonNode entries = jsonNode.get("entry");
    if (entries != null) {
      for (JsonNode entry : entries) {
        JsonNode resourceNode = entry.get("resource");
        if (resourceNode != null) {
          JsonNode resourceType = resourceNode.get("resourceType");
          if (resourceType != null && "MeasureReport".equalsIgnoreCase(resourceType.asText())) {
            JsonNode groups = resourceNode.get("group");

            TestCaseGroupPopulation groupPopulation = null;
            if (groups != null) {
              for (JsonNode group : groups) {
                JsonNode populations = group.get("population");
                List<TestCasePopulationValue> populationValues = new ArrayList<>();
                if (populations != null) {
                  for (JsonNode pouplation : populations) {
                    JsonNode codeNode = pouplation.get("code");
                    String count =
                        pouplation.get("count") != null ? pouplation.get("count").asText() : "";
                    if (codeNode != null) {
                      JsonNode codings = codeNode.get("coding");
                      if (codings != null) {
                        for (JsonNode coding : codings) {
                          String code = coding.get("code").asText();
                          TestCasePopulationValue populationValue =
                              TestCasePopulationValue.builder()
                                  .name(PopulationType.fromCode(code))
                                  .expected(count)
                                  .build();
                          populationValues.add(populationValue);
                        }
                      }
                      groupPopulation =
                          TestCaseGroupPopulation.builder()
                              .populationValues(populationValues)
                              .build();
                    }
                  }
                  if (groupPopulation != null
                      && !CollectionUtils.isEmpty(groupPopulation.getPopulationValues())) {
                    groupPopulations.add(groupPopulation);
                  }
                }
              }
            }
          }
        }
      }
    }
    return groupPopulations;
  }

  public static String removeMeasureReportFromJson(String testCaseJson)
      throws JsonProcessingException {
    if (!StringUtils.isEmpty(testCaseJson)) {
      ObjectMapper objectMapper = new ObjectMapper();

      JsonNode rootNode = objectMapper.readTree(testCaseJson);
      ArrayNode entryArray = (ArrayNode) rootNode.get("entry");

      List<JsonNode> filteredList = new ArrayList<>();
      for (JsonNode entryNode : entryArray) {
        if (!"MeasureReport"
            .equalsIgnoreCase(entryNode.get("resource").get("resourceType").asText())) {
          filteredList.add(entryNode);
        }
      }
      entryArray.removeAll();
      filteredList.forEach(entryArray::add);
      return objectMapper.writeValueAsString(rootNode);
    } else {
      throw new RuntimeException("Unable to find Test case Json");
    }
  }

  public static String buildFullUrl(
      final String id, String resourceType, String madieJsonResourcesBaseUri) {
    return madieJsonResourcesBaseUri + resourceType + "/" + id;
  }

  public static String enforcePatientId(TestCase testCase, String madieJsonResourcesBaseUri) {
    String testCaseJson = testCase.getJson();
    if (!StringUtils.isEmpty(testCaseJson)) {
      ObjectMapper objectMapper = new ObjectMapper();
      String modifiedJsonString = testCaseJson;
      try {
        final String newPatientId = testCase.getPatientId().toString();
        JsonNode rootNode = objectMapper.readTree(testCaseJson);
        ArrayNode allEntries = (ArrayNode) rootNode.get("entry");
        if (allEntries != null) {
          for (JsonNode node : allEntries) {
            if (node.get("resource") != null
                && node.get("resource").get("resourceType") != null
                && node.get("resource").get("resourceType").asText().equalsIgnoreCase("Patient")) {
              JsonNode resourceNode = node.get("resource");
              ObjectNode o = (ObjectNode) resourceNode;
              ObjectNode parent = (ObjectNode) node;
              parent.put(
                  "fullUrl", buildFullUrl(newPatientId, "Patient", madieJsonResourcesBaseUri));
              o.put("id", newPatientId);
              modifiedJsonString = jsonNodeToString(objectMapper, rootNode);
            }
          }
        }
        return modifiedJsonString;
      } catch (JsonProcessingException e) {
        log.error("Error reading testCaseJson testCaseId = " + testCase.getId(), e);
      }
    }
    return testCaseJson;
  }

  // update full urls for non-patient resources
  public static String updateResourceFullUrls(TestCase testCase, String madieJsonResourcesBaseUri) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      JsonNode rootNode = mapper.readTree(testCase.getJson());
      JsonNode entry = rootNode.get("entry");
      if (entry != null) {
        for (JsonNode theNode : entry) {
          var resourceNode = theNode.get("resource");
          if (resourceNode != null) {
            var resourceType = resourceNode.get("resourceType").asText();
            if (resourceType != null
                && !"Patient".equalsIgnoreCase(resourceType)
                && theNode.has("fullUrl")) {
              String id = resourceNode.get("id").asText();
              String newUrl = buildFullUrl(id, resourceType, madieJsonResourcesBaseUri);
              log.info("Updating the full url of a resource [{}], new fullUrl is [{}]", id, newUrl);
              ObjectNode node = (ObjectNode) theNode;
              node.put("fullUrl", newUrl);
            }
          }
        }
      }
      return jsonNodeToString(mapper, rootNode);
    } catch (JsonProcessingException ex) {
      log.error("Error reading testCaseJson testCaseId = " + testCase.getId(), ex);
    }
    return testCase.getJson();
  }

  protected static String jsonNodeToString(ObjectMapper objectMapper, JsonNode rootNode) {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(bout, rootNode);
    } catch (Exception ex) {
      log.error("Exception : " + ex.getMessage());
    }
    return bout.toString();
  }
}
