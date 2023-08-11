package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.*;
import cms.gov.madie.measure.repositories.MeasureRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Slf4j
@Service
public class TestCaseService {

  private final MeasureRepository measureRepository;
  private ActionLogService actionLogService;
  private FhirServicesClient fhirServicesClient;
  private ObjectMapper mapper;
  private MeasureService measureService;

  @Value("${madie.json.resources.base-uri}")
  private String madieJsonResourcesBaseUri;

  @Autowired
  public TestCaseService(
      MeasureRepository measureRepository,
      ActionLogService actionLogService,
      FhirServicesClient fhirServicesClient,
      ObjectMapper mapper,
      MeasureService measureService) {
    this.measureRepository = measureRepository;
    this.actionLogService = actionLogService;
    this.fhirServicesClient = fhirServicesClient;
    this.mapper = mapper;
    this.measureService = measureService;
  }

  protected TestCase enrichNewTestCase(TestCase testCase, String username) {
    final TestCase enrichedTestCase = testCase.toBuilder().build();
    Instant now = Instant.now();
    enrichedTestCase.setId(ObjectId.get().toString());
    enrichedTestCase.setCreatedAt(now);
    enrichedTestCase.setCreatedBy(username);
    enrichedTestCase.setLastModifiedAt(now);
    enrichedTestCase.setLastModifiedBy(username);
    enrichedTestCase.setResourceUri(null);
    enrichedTestCase.setHapiOperationOutcome(null);
    enrichedTestCase.setValidResource(false);
    enrichedTestCase.setPatientId(UUID.randomUUID());
    return enrichedTestCase;
  }

  protected void verifyUniqueTestCaseName(TestCase testCase, Measure measure) {
    if (isEmpty(measure.getTestCases())) {
      return;
    }
    // ignore spaces
    final String newName = StringUtils.deleteWhitespace(testCase.getTitle() + testCase.getSeries());

    boolean matchesExistingTestCaseName =
        measure.getTestCases().stream()
            // exclude the current test case
            .filter(tc -> !tc.getId().equals(testCase.getId()))
            .map(tc -> StringUtils.deleteWhitespace(tc.getTitle() + tc.getSeries()))
            .anyMatch(existingName -> existingName.equalsIgnoreCase(newName));

    if (matchesExistingTestCaseName) {
      throw new DuplicateTestCaseNameException();
    }
  }

  public TestCase persistTestCase(
      TestCase testCase, String measureId, String username, String accessToken) {
    final Measure measure = findMeasureById(measureId);

    if (!measure.getMeasureMetaData().isDraft()) {
      throw new InvalidDraftStatusException(measure.getId());
    }

    verifyUniqueTestCaseName(testCase, measure);

    TestCase enrichedTestCase = enrichNewTestCase(testCase, username);
    enrichedTestCase = validateTestCaseAsResource(enrichedTestCase, accessToken);

    if (measure.getTestCases() == null) {
      measure.setTestCases(List.of(enrichedTestCase));
    } else {
      measure.getTestCases().add(enrichedTestCase);
    }

    measureRepository.save(measure);

    actionLogService.logAction(
        enrichedTestCase.getId(), TestCase.class, ActionType.CREATED, username);

    log.info(
        "User [{}] successfully created new test case with ID [{}] for the measure with ID[{}] ",
        username,
        testCase.getId(),
        measureId);
    return enrichedTestCase;
  }

  public List<TestCase> persistTestCases(
      List<TestCase> newTestCases, String measureId, String username, String accessToken) {
    if (newTestCases == null || newTestCases.isEmpty()) {
      return newTestCases;
    }
    final Measure measure = findMeasureById(measureId);

    if (!measure.getMeasureMetaData().isDraft()) {
      throw new InvalidDraftStatusException(measure.getId());
    }

    List<TestCase> enrichedTestCases = new ArrayList<>(newTestCases.size());
    for (TestCase testCase : newTestCases) {
      TestCase enriched = enrichNewTestCase(testCase, username);
      enriched = validateTestCaseAsResource(enriched, accessToken);
      enrichedTestCases.add(enriched);
      actionLogService.logAction(enriched.getId(), TestCase.class, ActionType.IMPORTED, username);
    }
    if (measure.getTestCases() == null) {
      measure.setTestCases(enrichedTestCases);
    } else {
      measure.getTestCases().addAll(enrichedTestCases);
    }

    measureRepository.save(measure);

    log.info(
        "User [{}] successfully imported [{}] test cases to the measure with ID[{}] ",
        username,
        enrichedTestCases.size(),
        measureId);
    return enrichedTestCases;
  }

  public TestCase validateTestCaseAsResource(final TestCase testCase, final String accessToken) {
    final HapiOperationOutcome hapiOperationOutcome = validateTestCaseJson(testCase, accessToken);
    return testCase == null
        ? null
        : testCase
            .toBuilder()
            .hapiOperationOutcome(hapiOperationOutcome)
            .validResource(hapiOperationOutcome != null && hapiOperationOutcome.isSuccessful())
            .build();
  }

  public TestCase updateTestCase(
      TestCase testCase, String measureId, String username, String accessToken) {
    Measure measure = measureService.findMeasureById(measureId);
    if (measure == null) {
      throw new ResourceNotFoundException("Measure", measureId);
    }

    if (!measure.getMeasureMetaData().isDraft()) {
      throw new InvalidDraftStatusException(measure.getId());
    }

    if (measure.getTestCases() == null) {
      measure.setTestCases(new ArrayList<>());
    }

    verifyUniqueTestCaseName(testCase, measure);
    measureService.verifyAuthorization(username, measure);
    Instant now = Instant.now();
    testCase.setLastModifiedAt(now);
    testCase.setLastModifiedBy(username);

    Optional<TestCase> existingOpt =
        measure.getTestCases().stream().filter(p -> p.getId().equals(testCase.getId())).findFirst();
    if (existingOpt.isPresent()) {
      TestCase existing = existingOpt.get();
      testCase.setCreatedAt(existing.getCreatedAt());
      testCase.setCreatedBy(existing.getCreatedBy());
      testCase.setResourceUri(existing.getResourceUri());
      // assure patientId is not overwritten
      testCase.setPatientId(existing.getPatientId());
      measure.getTestCases().remove(existing);
    } else {
      // still allowing upsert
      testCase.setId(ObjectId.get().toString());
      testCase.setCreatedAt(now);
      testCase.setCreatedBy(username);
      testCase.setPatientId(UUID.randomUUID());
    }

    TestCase validatedTestCase = validateTestCaseAsResource(testCase, accessToken);
    if (ModelType.QI_CORE.getValue().equalsIgnoreCase(measure.getModel())) {
      validatedTestCase.setJson(enforcePatientId(validatedTestCase));
    }
    measure.getTestCases().add(validatedTestCase);

    measureRepository.save(measure);

    log.info(
        "User [{}] successfully updated the test case with ID [{}] for the measure with ID[{}] ",
        username,
        testCase.getId(),
        measureId);
    return validatedTestCase;
  }

  public TestCase getTestCase(
      String measureId, String testCaseId, boolean validate, String accessToken) {
    TestCase testCase =
        Optional.ofNullable(findMeasureById(measureId).getTestCases())
            .orElseThrow(() -> new ResourceNotFoundException("Test Case", testCaseId)).stream()
            .filter(tc -> tc.getId().equals(testCaseId))
            .findFirst()
            .orElse(null);
    if (testCase == null) {
      throw new ResourceNotFoundException("Test Case", testCaseId);
    } else if (validate) {
      testCase.setHapiOperationOutcome(validateTestCaseJson(testCase, accessToken));
    }
    return testCase;
  }

  public List<TestCase> findTestCasesByMeasureId(String measureId) {
    return findMeasureById(measureId).getTestCases();
  }

  public String deleteTestCase(String measureId, String testCaseId, String username) {
    if (StringUtils.isBlank(testCaseId) || StringUtils.isBlank(measureId)) {
      log.info("Test case/Measure Id cannot be null");
      throw new InvalidIdException("Test case cannot be deleted, please contact the helpdesk");
    }

    Measure measure = findMeasureById(measureId);

    if (!measure.getMeasureMetaData().isDraft()) {
      throw new InvalidDraftStatusException(measure.getId());
    }

    measureService.verifyAuthorization(username, measure);
    if (isEmpty(measure.getTestCases())) {
      log.info("Measure with ID [{}] doesn't have any test cases", measureId);
      throw new InvalidIdException("Test case cannot be deleted, please contact the helpdesk");
    }
    List<TestCase> remainingTestCases =
        measure.getTestCases().stream().filter(g -> !g.getId().equals(testCaseId)).toList();
    // to check if given test case id is present
    if (remainingTestCases.size() == measure.getTestCases().size()) {
      log.info(
          "Measure with ID [{}] doesn't have any test case with ID [{}]", measureId, testCaseId);
      throw new InvalidIdException("Test case cannot be deleted, please contact the helpdesk");
    }
    measure.setTestCases(remainingTestCases);
    log.info(
        "User [{}] has successfully deleted a test case with Id [{}] from measure [{}]",
        username,
        testCaseId,
        measureId);

    measureRepository.save(measure);
    return "Test case deleted successfully: " + testCaseId;
  }

  public String deleteTestCases(String measureId, List<String> testCaseIds, String username) {
    if (isEmpty(testCaseIds) || StringUtils.isBlank(measureId)) {
      log.info("Test case Ids or Measure Id is Empty");
      throw new InvalidIdException("Test cases cannot be deleted, please contact the helpdesk");
    }

    Measure measure = findMeasureById(measureId);

    if (!measure.getMeasureMetaData().isDraft()) {
      throw new InvalidDraftStatusException(measure.getId());
    }

    measureService.verifyAuthorization(username, measure);
    if (isEmpty(measure.getTestCases())) {
      log.info("Measure with ID [{}] doesn't have any test cases", measureId);
      throw new InvalidIdException(
          "Measure {} doesn't have any existing test cases to delete", measureId);
    }

    List<TestCase> deletedTestCases =
        measure.getTestCases().stream().filter(tc -> testCaseIds.contains(tc.getId())).toList();

    List<TestCase> remainingTestCases =
        measure.getTestCases().stream().filter(tc -> !testCaseIds.contains(tc.getId())).toList();

    measure.setTestCases(remainingTestCases);
    measureRepository.save(measure);

    List<String> notDeletedTestCases =
        testCaseIds.stream()
            .filter(
                id -> deletedTestCases.stream().noneMatch(tc -> tc.getId().equalsIgnoreCase(id)))
            .toList();
    if (!isEmpty(notDeletedTestCases)) {
      log.info(
          "User [{}] was unable to delete following test cases with Ids [{}] from measure [{}]",
          username,
          String.join(", ", notDeletedTestCases),
          measureId);
      return "Succesfully deleted provided test cases except [ "
          + String.join(", ", notDeletedTestCases)
          + " ]";
    }
    log.info(
        "User [{}] has successfully deleted following test cases with Ids [{}] from measure [{}]",
        username,
        String.join(", ", testCaseIds),
        measureId);
    return "Succesfully deleted provided test cases";
  }

  public List<TestCaseImportOutcome> importTestCases(
      List<TestCaseImportRequest> testCaseImportRequests,
      String measureId,
      String userName,
      String accessToken) {
    Measure measure = findMeasureById(measureId);
    return testCaseImportRequests.stream()
        .map(
            testCaseImportRequest -> {
              Optional<TestCase> existingTestCase =
                  measure.getTestCases().stream()
                      .filter(
                          testCase ->
                              testCase.getPatientId().equals(testCaseImportRequest.getPatientId()))
                      .findFirst();
              if (existingTestCase.isPresent()) {
                return updateTestCaseJsonAndSaveTestCase(
                    existingTestCase.get(),
                    testCaseImportRequest,
                    measureId,
                    userName,
                    accessToken);
              } else {
                log.info(
                    "User {} is unable to import test case with patient id : "
                        + "{} because Patient ID is not found ",
                    userName,
                    testCaseImportRequest.getPatientId());
                return TestCaseImportOutcome.builder()
                    .patientId(testCaseImportRequest.getPatientId())
                    .successful(false)
                    .message("Patient Id is not found")
                    .build();
              }
            })
        .toList();
  }

  private TestCaseImportOutcome updateTestCaseJsonAndSaveTestCase(
      TestCase existingTestCase,
      TestCaseImportRequest testCaseImportRequest,
      String measureId,
      String userName,
      String accessToken) {
    try {
      existingTestCase.setJson(removeMeasureReportFromJson(testCaseImportRequest.getJson()));
      TestCase updatedTestCase = updateTestCase(existingTestCase, measureId, userName, accessToken);
      log.info(
          "User {} succesfully imported test case with patient id : {}",
          userName,
          updatedTestCase.getPatientId());
      return TestCaseImportOutcome.builder()
          .patientId(updatedTestCase.getPatientId())
          .successful(true)
          .build();
    } catch (JsonProcessingException e) {
      log.info(
          "User {} is unable to import test case with patient id : "
              + "{} due to Malformed test case json bundle",
          userName,
          testCaseImportRequest.getPatientId());
      return TestCaseImportOutcome.builder()
          .patientId(testCaseImportRequest.getPatientId())
          .successful(false)
          .message(
              "Error while processing Test Case Json. "
                  + "Please make sure Test Case JSON is valid and Measure Report is not modified")
          .build();
    } catch (ResourceNotFoundException
        | InvalidDraftStatusException
        | InvalidMeasureStateException
        | UnauthorizedException e) {
      log.info(
          "User {} is unable to import test case with patient id : {}; Error Message : {}",
          userName,
          testCaseImportRequest.getPatientId(),
          e.getMessage());
      return TestCaseImportOutcome.builder()
          .patientId(testCaseImportRequest.getPatientId())
          .successful(false)
          .message(e.getMessage())
          .build();
    } catch (Exception e) {
      log.info(
          "User {} is unable to import test case with patient id : {}; Error Message : {}",
          userName,
          testCaseImportRequest.getPatientId(),
          e.getMessage());
      return TestCaseImportOutcome.builder()
          .patientId(testCaseImportRequest.getPatientId())
          .successful(false)
          .message(
              "Unable to import test case, please try again."
                  + " if the error persists, Please contact helpdesk.")
          .build();
    }
  }

  private String removeMeasureReportFromJson(String testCaseJson) throws JsonProcessingException {
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

  public Measure findMeasureById(String measureId) {
    Measure measure = measureRepository.findById(measureId).orElse(null);
    if (measure == null) {
      log.info("Could not find Measure with id: {}", measureId);
      throw new ResourceNotFoundException("Measure", measureId);
    }
    return measure;
  }

  public List<String> findTestCaseSeriesByMeasureId(String measureId) {
    Measure measure =
        measureRepository
            .findAllTestCaseSeriesByMeasureId(measureId)
            .orElseThrow(() -> new ResourceNotFoundException("Measure", measureId));
    return Optional.ofNullable(measure.getTestCases()).orElse(List.of()).stream()
        .map(TestCase::getSeries)
        .filter(series -> series != null && !series.trim().isEmpty())
        .distinct()
        .collect(Collectors.toList());
  }

  public HapiOperationOutcome validateTestCaseJson(TestCase testCase, String accessToken) {
    if (testCase == null || StringUtils.isBlank(testCase.getJson())) {
      return null;
    }

    try {
      ResponseEntity<String> output =
          fhirServicesClient.validateBundle(testCase.getJson(), accessToken);
      return mapper.readValue(output.getBody(), HapiOperationOutcome.class);
    } catch (HttpClientErrorException ex) {
      log.warn("HAPI FHIR returned response code [{}]", ex.getRawStatusCode(), ex);
      try {
        return HapiOperationOutcome.builder()
            .code(ex.getRawStatusCode())
            .message("Unable to validate test case JSON due to errors")
            .outcomeResponse(mapper.readValue(ex.getResponseBodyAsString(), Object.class))
            .build();
      } catch (JsonProcessingException e) {
        return handleJsonProcessingException();
      }
    } catch (JsonProcessingException e) {
      log.error("An error occurred while processing test case JSON validation outcome", e);
      return handleJsonProcessingException();
    } catch (Exception ex) {
      log.error("Exception occurred validating bundle with FHIR Service:", ex);
      return HapiOperationOutcome.builder()
          .code(500)
          .message("An unknown exception occurred while validating the test case JSON.")
          .build();
    }
  }

  private HapiOperationOutcome handleJsonProcessingException() {
    return HapiOperationOutcome.builder()
        .code(500)
        .message(
            "Unable to validate test case JSON due to errors, "
                + "but outcome not able to be interpreted!")
        .build();
  }

  public String buildFullUrlForPatient(final String newPatientId) {
    return madieJsonResourcesBaseUri + newPatientId;
  }

  public String enforcePatientId(TestCase testCase) {
    String testCaseJson = testCase.getJson();
    if (!StringUtils.isEmpty(testCaseJson)) {
      ObjectMapper objectMapper = new ObjectMapper();
      String modifiedjsonString = testCaseJson;
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
              parent.put("fullUrl", buildFullUrlForPatient(newPatientId));

              o.put("id", newPatientId);

              ByteArrayOutputStream bout = getByteArrayOutputStream(objectMapper, rootNode);
              modifiedjsonString = bout.toString();
            }
          }
        }

        return modifiedjsonString;
      } catch (JsonProcessingException e) {
        log.error("Error reading testCaseJson testCaseId = " + testCase.getId(), e);
      }
    }
    return testCaseJson;
  }

  protected ByteArrayOutputStream getByteArrayOutputStream(
      ObjectMapper objectMapper, JsonNode rootNode) {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(bout, rootNode);
    } catch (Exception ex) {
      log.error("Exception : " + ex.getMessage());
    }
    return bout;
  }
}
