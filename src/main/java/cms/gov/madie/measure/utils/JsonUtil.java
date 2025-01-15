package cms.gov.madie.measure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.QdmMeasure;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import gov.cms.madie.models.measure.TestCaseStratificationValue;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public final class JsonUtil {
  private static final String CQFM_TEST_DESCRIPTION_URL =
      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-testCaseDescription";

  private JsonUtil() {}

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

  public static JsonNode getResourceNode(String json, String resourceType)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(json);
    JsonNode entries = jsonNode.get("entry");
    if (entries != null) {
      for (JsonNode entry : entries) {
        JsonNode resourceNode = entry.get("resource");
        if (resourceNode != null) {
          JsonNode resourceTypeNode = resourceNode.get("resourceType");
          if (resourceTypeNode != null
              && resourceType.equalsIgnoreCase(resourceTypeNode.asText())) {
            return resourceNode;
          }
        }
      }
    }
    return null;
  }

  public static String getPatientName(String json, String type) throws JsonProcessingException {
    JsonNode resourceNode = getResourceNode(json, "patient");
    if (resourceNode == null) {
      return null;
    }
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
    return null;
  }

  public static String getTestDescription(String testCaseBundle) throws JsonProcessingException {
    JsonNode resourceNode = getResourceNode(testCaseBundle, "MeasureReport");
    if (resourceNode == null || resourceNode.get("extension") == null) {
      return null;
    }
    JsonNode reportExtension = resourceNode.get("extension");
    for (JsonNode entry : reportExtension) {
      String extensionUrl = entry.get("url").asText();
      if (CQFM_TEST_DESCRIPTION_URL.equalsIgnoreCase(extensionUrl)) {
        return entry.get("valueMarkdown").asText();
      }
    }
    return null;
  }

  public static List<TestCaseGroupPopulation> getTestCaseGroupPopulationsFromMeasureReport(
      String json, Measure measure) throws JsonProcessingException {
    List<TestCaseGroupPopulation> groupPopulations = new ArrayList<>();
    JsonNode resourceNode = getResourceNode(json, "MeasureReport");
    if (resourceNode != null) {
      JsonNode groups = resourceNode.get("group");
      TestCaseGroupPopulation groupPopulation = null;
      if (groups != null) {
        int stratCount = 1;
        for (JsonNode group : groups) {
          JsonNode populations = group.get("population");
          List<TestCasePopulationValue> populationValues = new ArrayList<>();
          if (populations != null) {
            for (JsonNode population : populations) {
              JsonNode codeNode = population.get("code");
              String count =
                  population.get("count") != null ? population.get("count").asText() : "";
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
                    TestCaseGroupPopulation.builder().populationValues(populationValues).build();
              }
            }
            if (groupPopulation != null
                && !CollectionUtils.isEmpty(groupPopulation.getPopulationValues())) {
              groupPopulations.add(groupPopulation);
            }
          }
          // get Stratifications
          JsonNode stratifications = group.get("stratifier");
          if (stratifications != null) {
            List<TestCaseStratificationValue> stratificationValues = new ArrayList<>();
            for (JsonNode stratification : stratifications) {
              log.debug("This is a stratification {}", stratification);
              String groupId = group.get("id").toString().replace("\"", "");
              String stratId = stratification.get("id").toString().replace("\"", "");

              Optional<Group> popGroup =
                  measure.getGroups().stream()
                      .filter(grp -> grp.getId().equals(groupId))
                      .findFirst();

              // So I think we need to match up the stratifications to the groups and group
              // population...
              String stratName =
                  ((ArrayNode) stratification.get("code")).get(0).get("text").toString();
              ArrayNode stratums = (ArrayNode) stratification.get("stratum");
              ArrayNode expectedValuesArray =
                  StreamSupport.stream(stratums.spliterator(), false)
                      .filter(stratum -> stratum.findPath("value").findValue("text").asBoolean())
                      .collect(Collectors.toList())
                      .get(0)
                      .withArrayProperty("population");
              // from expectedValuesStrat, I can get all the expected Pop values
              List<TestCasePopulationValue> expectedStratValues = new ArrayList<>();
              expectedValuesArray.forEach(
                  (expVal) -> {
                    String code = expVal.get("code").get("coding").get(0).get("code").asText();

                    TestCasePopulationValue value =
                        TestCasePopulationValue.builder()
                            .name(PopulationType.fromCode(code))
                            .expected(
                                popGroup.get().getPopulationBasis().equals("Boolean")
                                    ? Integer.parseInt(expVal.get("count").asText()) == 1
                                    : Integer.parseInt(expVal.get("count").asText()))
                            .build();
                    expectedStratValues.add(value);
                  });
              TestCaseStratificationValue stratValue =
                  TestCaseStratificationValue.builder()
                      .id(stratId) // UUID.randomUUID().toString())
                      .name(stratName)
                      .build();
              stratValue.setPopulationValues(expectedStratValues);
              stratificationValues.add(stratValue);
            }
            groupPopulation.setStratificationValues(stratificationValues);
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

  public static String getPatientNameQdm(String json, String type) throws JsonProcessingException {
    JsonNode resourceNode = getResourceNodeQdm(json, type);
    if (resourceNode == null) {
      return null;
    }
    if (type.equalsIgnoreCase("givenNames")) {
      for (JsonNode givenName : resourceNode) {
        return givenName.asText();
      }
    }
    return resourceNode.asText();
  }

  public static JsonNode getResourceNodeQdm(String json, String resourceType)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(json);
    return jsonNode.get(resourceType);
  }

  public static List<TestCaseGroupPopulation> getTestCaseGroupPopulationsQdm(
      String json, Measure measure) throws JsonProcessingException {
    List<TestCaseGroupPopulation> groupPopulations = new ArrayList<>();
    JsonNode populations = getResourceNodeQdm(json, "expectedValues");
    if (populations != null) {
      int stratCount = 0; // appended to strat name
      for (JsonNode population : populations) {
        List<TestCasePopulationValue> populationValues = new ArrayList<>();
        TestCaseGroupPopulation groupPopulation = null;

        handlePopulationValues(population, populationValues, measure);

        JsonNode stratification = population.get("STRAT");
        if (stratification != null) {
          groupPopulation =
              TestCaseGroupPopulation.builder()
                  .stratificationValues(
                      handleStratificationValues(
                          stratification, populationValues, measure, ++stratCount))
                  .build();
        } else {
          groupPopulation =
              TestCaseGroupPopulation.builder().populationValues(populationValues).build();
        }
        groupPopulations.add(groupPopulation);
      }
    }
    return groupPopulations;
  }

  private static void handlePopulationValues(
      JsonNode population, List<TestCasePopulationValue> populationValues, Measure measure) {
    if (population.get("IPP") != null) {
      TestCasePopulationValue populationValue =
          TestCasePopulationValue.builder()
              .name(PopulationType.INITIAL_POPULATION)
              .expected(population.get("IPP").asInt())
              .build();
      populationValues.add(populationValue);
    }

    setObservationValuesForCV(population, populationValues, measure);

    setDenominatorValues(population, populationValues, measure);

    setNumeratorValues(population, populationValues, measure);

    if (population.get("DENEXCEP") != null) {
      TestCasePopulationValue populationValue =
          TestCasePopulationValue.builder()
              .name(PopulationType.DENOMINATOR_EXCEPTION)
              .expected(population.get("DENEXCEP").asInt())
              .build();
      populationValues.add(populationValue);
    }
  }

  static void setObservationValuesForCV(
      JsonNode population, List<TestCasePopulationValue> populationValues, Measure measure) {
    QdmMeasure qdmMeasure = (QdmMeasure) measure;
    if (StringUtils.equals(
        qdmMeasure.getScoring(), MeasureScoring.CONTINUOUS_VARIABLE.toString())) {
      if (population.get("MSRPOPL") != null) {
        TestCasePopulationValue populationValue =
            TestCasePopulationValue.builder()
                .name(PopulationType.MEASURE_POPULATION)
                .expected(population.get("MSRPOPL").asInt())
                .build();
        populationValues.add(populationValue);
      }
      if (population.get("OBSERV") != null
          && CollectionUtils.isNotEmpty(measure.getGroups())
          && measure.getGroups().size() == 1) {
        for (JsonNode observation : population.get("OBSERV")) {
          if (observation != null) {
            TestCasePopulationValue populationValue =
                TestCasePopulationValue.builder()
                    .id(UUID.randomUUID().toString()) // need to have id
                    .name(
                        PopulationType
                            .MEASURE_POPULATION_OBSERVATION) // MEASURE_POPULATION_OBSERVATION is
                    // for QDM, MEASURE_OBSERVATION is for
                    // QI-Core
                    .expected(observation.asInt())
                    .build();
            populationValues.add(populationValue);
          }
        }
      }

      if (population.get("MSRPOPLEX") != null) {
        TestCasePopulationValue populationValue =
            TestCasePopulationValue.builder()
                .name(PopulationType.MEASURE_POPULATION_EXCLUSION)
                .expected(population.get("MSRPOPLEX").asInt())
                .build();
        populationValues.add(populationValue);
      }
    }
  }

  protected static void setDenominatorValues(
      JsonNode population, List<TestCasePopulationValue> populationValues, Measure measure) {
    if (population.get("DENOM") != null) {
      TestCasePopulationValue populationValue =
          TestCasePopulationValue.builder()
              .name(PopulationType.DENOMINATOR)
              .expected(population.get("DENOM").asInt())
              .build();
      populationValues.add(populationValue);
    }
    QdmMeasure qdmMeasure = (QdmMeasure) measure;
    if (StringUtils.equals(qdmMeasure.getScoring(), MeasureScoring.RATIO.toString())) {
      if (population.get("DENOM_OBSERV") != null) {
        for (JsonNode observation : population.get("DENOM_OBSERV")) {
          if (observation != null) {
            TestCasePopulationValue populationValue =
                TestCasePopulationValue.builder()
                    .name(PopulationType.DENOMINATOR_OBSERVATION)
                    .expected(observation.asInt())
                    .build();
            populationValues.add(populationValue);
          }
        }
      }
    }
    if (population.get("DENEX") != null) {
      TestCasePopulationValue populationValue =
          TestCasePopulationValue.builder()
              .name(PopulationType.DENOMINATOR_EXCLUSION)
              .expected(population.get("DENEX").asInt())
              .build();
      populationValues.add(populationValue);
    }
  }

  protected static void setNumeratorValues(
      JsonNode population, List<TestCasePopulationValue> populationValues, Measure measure) {
    if (population.get("NUMER") != null) {
      TestCasePopulationValue populationValue =
          TestCasePopulationValue.builder()
              .name(PopulationType.NUMERATOR)
              .expected(population.get("NUMER").asInt())
              .build();
      populationValues.add(populationValue);
    }
    QdmMeasure qdmMeasure = (QdmMeasure) measure;
    if (StringUtils.equals(qdmMeasure.getScoring(), MeasureScoring.RATIO.toString())) {
      if (population.get("NUMER_OBSERV") != null) {
        for (JsonNode observation : population.get("NUMER_OBSERV")) {
          if (observation != null) {
            TestCasePopulationValue populationValue =
                TestCasePopulationValue.builder()
                    .name(PopulationType.NUMERATOR_OBSERVATION)
                    .expected(observation.asInt())
                    .build();
            populationValues.add(populationValue);
          }
        }
      }
    }
    if (population.get("NUMEX") != null) {
      TestCasePopulationValue populationValue =
          TestCasePopulationValue.builder()
              .name(PopulationType.NUMERATOR_EXCLUSION)
              .expected(population.get("NUMEX").asInt())
              .build();
      populationValues.add(populationValue);
    }
  }

  private static List<TestCaseStratificationValue> handleStratificationValues(
      JsonNode stratification,
      List<TestCasePopulationValue> populationValues,
      Measure measure,
      int stratCount) {
    QdmMeasure qdmMeasure = (QdmMeasure) measure;

    if (!qdmMeasure.getScoring().equalsIgnoreCase(MeasureScoring.RATIO.toString())) {
      List<TestCaseStratificationValue> stratificationValues = new ArrayList<>();
      String stratName = "Strata-" + stratCount;
      TestCaseStratificationValue stratValue =
          TestCaseStratificationValue.builder()
              .id(UUID.randomUUID().toString())
              .name(stratName)
              .expected(
                  qdmMeasure.isPatientBasis()
                      ? stratification.intValue() == 1
                      : stratification.intValue())
              .build();
      stratificationValues.add(stratValue);
      stratValue.setPopulationValues(populationValues);
      return stratificationValues;
    }
    return null;
  }

  public static String getTestDescriptionQdm(String json) throws JsonProcessingException {
    JsonNode notesNode = getResourceNodeQdm(json, "notes");
    if (notesNode != null) {
      return notesNode.asText();
    }
    return null;
  }

  public static String getTestCaseJson(String json) throws JsonProcessingException {
    JsonNode patientNode = getResourceNodeQdm(json, "qdmPatient");
    if (patientNode != null) {
      return patientNode.toPrettyString();
    }
    return null;
  }
}
