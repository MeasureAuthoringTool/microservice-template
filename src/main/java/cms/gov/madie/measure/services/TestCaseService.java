package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.JobStatus;
import cms.gov.madie.measure.dto.MeasureTestCaseValidationReport;
import cms.gov.madie.measure.dto.TestCaseValidationReport;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.Group;
import cms.gov.madie.measure.exceptions.*;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.utils.QiCoreJsonUtil;
import cms.gov.madie.measure.utils.TestCaseServiceUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.measure.TestCaseImportOutcome;
import gov.cms.madie.models.measure.TestCaseImportRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
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
  private TestCaseServiceUtil testCaseServiceUtil;

  @Value("${madie.json.resources.base-uri}")
  @Getter
  private String madieJsonResourcesBaseUri;

  @Autowired
  public TestCaseService(
      MeasureRepository measureRepository,
      ActionLogService actionLogService,
      FhirServicesClient fhirServicesClient,
      ObjectMapper mapper,
      MeasureService measureService,
      TestCaseServiceUtil testCaseServiceUtil) {
    this.measureRepository = measureRepository;
    this.actionLogService = actionLogService;
    this.fhirServicesClient = fhirServicesClient;
    this.mapper = mapper;
    this.measureService = measureService;
    this.testCaseServiceUtil = testCaseServiceUtil;
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
    enrichedTestCase =
        validateTestCaseAsResource(
            enrichedTestCase, ModelType.valueOfName(measure.getModel()), accessToken);

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
      enriched =
          validateTestCaseAsResource(
              enriched, ModelType.valueOfName(measure.getModel()), accessToken);
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

  public MeasureTestCaseValidationReport updateTestCaseValidResourcesWithReport(
      final String measureId, final String accessToken) {
    log.info(
        "Thread [{}] :: Updating ValidResource flag for all test cases on measure [{}]",
        Thread.currentThread().getName(),
        measureId);
    final Optional<Measure> measureOpt = measureRepository.findById(measureId);
    if (measureOpt.isPresent()) {
      final Measure measure = measureOpt.get();
      MeasureTestCaseValidationReport measureReport =
          MeasureTestCaseValidationReport.builder()
              .measureName(measure.getMeasureName())
              .measureId(measure.getId())
              .measureSetId(measure.getMeasureSetId())
              .measureVersionId(measure.getVersionId())
              .testCaseValidationReports(List.of())
              .build();

      if (!isEmpty(measure.getTestCases())) {
        List<TestCaseValidationReport> reports =
            measure.getTestCases().stream()
                .map(
                    testCase ->
                        TestCaseValidationReport.builder()
                            .testCaseId(testCase.getId())
                            .patientId(testCase.getPatientId().toString())
                            .previousValidResource(testCase.isValidResource())
                            .build())
                .toList();
        List<TestCase> validatedTestCases =
            updateTestCaseValidResourcesForMeasure(measure, accessToken);
        Map<String, TestCase> testCaseMap =
            validatedTestCases.stream()
                .collect(Collectors.toMap(TestCase::getId, Function.identity()));
        reports.forEach(
            report ->
                report.setCurrentValidResource(
                    testCaseMap.get(report.getTestCaseId()).isValidResource()));
        measureReport.setTestCaseValidationReports(reports);
      }

      measureReport.setJobStatus(JobStatus.COMPLETED);
      return measureReport;
    }

    return MeasureTestCaseValidationReport.builder()
        .measureId(measureId)
        .jobStatus(JobStatus.SKIPPED)
        .build();
  }

  public List<TestCase> updateTestCaseValidResourcesForMeasure(
      Measure measure, final String accessToken) {
    List<TestCase> validatedTestCases =
        validateTestCasesAsResources(
            measure.getTestCases(), ModelType.valueOfName(measure.getModel()), accessToken);
    measure.setTestCases(validatedTestCases);
    measureRepository.save(measure);
    return validatedTestCases;
  }

  public List<TestCase> validateTestCasesAsResources(
      final List<TestCase> testCases, final ModelType modelType, final String accessToken) {
    List<TestCase> validatedTestCases = new ArrayList<>();

    if (!isEmpty(testCases)) {
      validatedTestCases =
          testCases.stream()
              .map(testCase -> validateTestCaseAsResource(testCase, modelType, accessToken))
              .collect(Collectors.toList());
    }

    return validatedTestCases;
  }

  public TestCase validateTestCaseAsResource(
      final TestCase testCase, final ModelType modelType, final String accessToken) {
    if (ModelType.QDM_5_6.equals(modelType)) {
      return testCase == null
          ? null
          : testCase
              .toBuilder()
              .validResource(QiCoreJsonUtil.isValidJson(testCase.getJson()))
              .build();
    } else {
      final HapiOperationOutcome hapiOperationOutcome = validateTestCaseJson(testCase, accessToken);
      return testCase == null
          ? null
          : testCase
              .toBuilder()
              .hapiOperationOutcome(hapiOperationOutcome)
              .validResource(hapiOperationOutcome != null && hapiOperationOutcome.isSuccessful())
              .build();
    }
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
      if (testCase.getPatientId() == null) {
        testCase.setPatientId(UUID.randomUUID());
      }
    }
    TestCase validatedTestCase =
        validateTestCaseAsResource(
            testCase, ModelType.valueOfName(measure.getModel()), accessToken);
    if (ModelType.QI_CORE.getValue().equalsIgnoreCase(measure.getModel())) {
      validatedTestCase.setJson(
          QiCoreJsonUtil.enforcePatientId(validatedTestCase, madieJsonResourcesBaseUri));
      validatedTestCase.setJson(
          QiCoreJsonUtil.updateResourceFullUrls(validatedTestCase, madieJsonResourcesBaseUri));
      validatedTestCase.setJson(
          QiCoreJsonUtil.replacePatientRefs(
              validatedTestCase.getJson(), validatedTestCase.getPatientId().toString()));
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
    Set<UUID> checkedTestCases = new HashSet<>();
    return testCaseImportRequests.stream()
        .filter(
            testCaseImportRequest ->
                !checkedTestCases.contains(testCaseImportRequest.getPatientId()))
        .map(
            testCaseImportRequest -> {
              checkedTestCases.add(testCaseImportRequest.getPatientId());
              if (testCaseImportRequests.stream()
                      .map(TestCaseImportRequest::getPatientId)
                      .filter(uuid -> uuid.equals(testCaseImportRequest.getPatientId()))
                      .count()
                  > 1) {
                return TestCaseImportOutcome.builder()
                    .patientId(testCaseImportRequest.getPatientId())
                    .successful(false)
                    .message(
                        "Multiple test case files are not supported."
                            + " Please make sure only one JSON file is in the folder.")
                    .build();
              }
              if (testCaseImportRequest.getJson() == null
                  || testCaseImportRequest.getJson().isEmpty()) {
                return TestCaseImportOutcome.builder()
                    .patientId(testCaseImportRequest.getPatientId())
                    .successful(false)
                    .message("Test Case file is missing.")
                    .build();
              }
              if (isEmpty(measure.getTestCases())) {
                return validateTestCaseJsonAndCreateTestCase(
                    testCaseImportRequest, measure, userName, accessToken);
              }
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
                    accessToken,
                    null);
              } else {
                return validateTestCaseJsonAndCreateTestCase(
                    testCaseImportRequest, measure, userName, accessToken);
              }
            })
        .toList();
  }

  private TestCaseImportOutcome validateTestCaseJsonAndCreateTestCase(
      TestCaseImportRequest testCaseImportRequest,
      Measure measure,
      String userName,
      String accessToken) {
    try {
      String patientFamilyName =
          QiCoreJsonUtil.getPatientName(testCaseImportRequest.getJson(), "family");
      String patientGivenName =
          QiCoreJsonUtil.getPatientName(testCaseImportRequest.getJson(), "given");
      log.info(
          "Test Case title + Test Case Group:  {}", patientGivenName + " " + patientFamilyName);
      if (StringUtils.isBlank(patientGivenName)) {
        return TestCaseImportOutcome.builder()
            .patientId(testCaseImportRequest.getPatientId())
            .successful(false)
            .message("Test Case Title is required.")
            .build();
      }
      TestCase newTestCase =
          TestCase.builder()
              .title(patientGivenName)
              .series(patientFamilyName)
              .patientId(testCaseImportRequest.getPatientId())
              .build();
      List<TestCaseGroupPopulation> testCaseGroupPopulations =
          QiCoreJsonUtil.getTestCaseGroupPopulationsFromMeasureReport(
              testCaseImportRequest.getJson());
      List<Group> groups = testCaseServiceUtil.getGroupsWithValidPopulations(measure.getGroups());
      boolean matched =
          testCaseServiceUtil.matchCriteriaGroups(testCaseGroupPopulations, groups, newTestCase);
      String warningMessage = null;
      if (!matched) {
        warningMessage =
            "The measure populations do not match the populations in the import file. "
                + "The Test Case has been imported, but no expected values have been set.";
      }
      return updateTestCaseJsonAndSaveTestCase(
          newTestCase,
          testCaseImportRequest,
          measure.getId(),
          userName,
          accessToken,
          warningMessage);
    } catch (JsonProcessingException ex) {
      log.info(
          "User {} is unable to import test case with patient id : "
              + "{} because of JsonProcessingException: "
              + ex.getMessage(),
          userName,
          testCaseImportRequest.getPatientId());
      return TestCaseImportOutcome.builder()
          .patientId(testCaseImportRequest.getPatientId())
          .successful(false)
          .message(
              "Error while processing Test Case JSON."
                  + " Please make sure Test Case JSON is valid.")
          .build();
    }
  }

  private TestCaseImportOutcome updateTestCaseJsonAndSaveTestCase(
      TestCase existingTestCase,
      TestCaseImportRequest testCaseImportRequest,
      String measureId,
      String userName,
      String accessToken,
      String warningMessage) {
    TestCaseImportOutcome failureOutcome =
        TestCaseImportOutcome.builder()
            .patientId(testCaseImportRequest.getPatientId())
            .successful(false)
            .build();
    try {
      String description = QiCoreJsonUtil.getTestDescription(testCaseImportRequest.getJson());
      existingTestCase.setJson(
          QiCoreJsonUtil.removeMeasureReportFromJson(testCaseImportRequest.getJson()));
      existingTestCase.setDescription(description);
      TestCase updatedTestCase = updateTestCase(existingTestCase, measureId, userName, accessToken);
      log.info(
          "User {} successfully imported test case with patient id : {}",
          userName,
          updatedTestCase.getPatientId());
      TestCaseImportOutcome testCaseImportOutcome =
          TestCaseImportOutcome.builder()
              .patientId(updatedTestCase.getPatientId())
              .successful(true)
              .build();
      if (warningMessage != null) {
        testCaseImportOutcome.setMessage(warningMessage);
      }
      return testCaseImportOutcome;
    } catch (JsonProcessingException e) {
      log.info(
          "User {} is unable to import test case with patient id : "
              + "{} due to Malformed test case json bundle",
          userName,
          testCaseImportRequest.getPatientId());
      failureOutcome.setMessage(
          "Error while processing Test Case JSON.  Please make sure Test Case JSON is valid.");
      return failureOutcome;
    } catch (ResourceNotFoundException
        | InvalidDraftStatusException
        | InvalidMeasureStateException
        | UnauthorizedException
        | DuplicateTestCaseNameException e) {
      log.info(
          "User {} is unable to import test case with patient id : {}; Error Message : {}",
          userName,
          testCaseImportRequest.getPatientId(),
          e.getMessage());
      failureOutcome.setMessage(e.getMessage());
      return failureOutcome;
    } catch (Exception e) {
      log.info(
          "User {} is unable to import test case with patient id : {}; Error Message : {}",
          userName,
          testCaseImportRequest.getPatientId(),
          e.getMessage());
      failureOutcome.setMessage(
          "Unable to import test case, please try again. "
              + "If the error persists, Please contact helpdesk.");
      return failureOutcome;
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
      return fhirServicesClient.validateBundle(testCase.getJson(), accessToken).getBody();
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
}
