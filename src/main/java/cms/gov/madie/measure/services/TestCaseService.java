package cms.gov.madie.measure.services;

import cms.gov.madie.measure.HapiFhirConfig;
import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import cms.gov.madie.measure.repositories.MeasureRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestCaseService {

  private final MeasureRepository measureRepository;
  private HapiFhirConfig hapiFhirConfig;
  private RestTemplate hapiFhirRestTemplate;
  private ActionLogService actionLogService;
  private FhirServicesClient fhirServicesClient;
  private ObjectMapper mapper;

  @Autowired
  public TestCaseService(
      MeasureRepository measureRepository,
      HapiFhirConfig hapiFhirConfig,
      @Qualifier("hapiFhirRestTemplate") RestTemplate hapiFhirRestTemplate,
      ActionLogService actionLogService,
      FhirServicesClient fhirServicesClient,
      ObjectMapper mapper) {
    this.measureRepository = measureRepository;
    this.hapiFhirConfig = hapiFhirConfig;
    this.hapiFhirRestTemplate = hapiFhirRestTemplate;
    this.actionLogService = actionLogService;
    this.fhirServicesClient = fhirServicesClient;
    this.mapper = mapper;
  }

  public TestCase persistTestCase(
      TestCase testCase, String measureId, String username, String accessToken) {
    Measure measure = findMeasureById(measureId);

    Instant now = Instant.now();
    // mongo doesn't create object id for embedded objects, setting manually
    testCase.setId(ObjectId.get().toString());
    testCase.setCreatedAt(now);
    testCase.setCreatedBy(username);
    testCase.setLastModifiedAt(now);
    testCase.setLastModifiedBy(username);
    testCase.setValidResource(false);
    testCase.setHapiOperationOutcome(null);
    testCase.setResourceUri(null);

    //    TestCase upserted = upsertFhirBundle(testCase);
    testCase.setHapiOperationOutcome(validateTestCaseJson(testCase, accessToken));

    if (measure.getTestCases() == null) {
      measure.setTestCases(List.of(testCase));
    } else {
      measure.getTestCases().add(testCase);
    }

    measureRepository.save(measure);

    actionLogService.logAction(testCase.getId(), TestCase.class, ActionType.CREATED, username);

    log.info(
        "User [{}] successfully created new test case with ID [{}] for the measure with ID[{}] ",
        username,
        testCase.getId(),
        measureId);
    return testCase;
  }

  public TestCase updateTestCase(
      TestCase testCase, String measureId, String username, String accessToken) {
    Measure measure = findMeasureById(measureId);
    if (measure.getTestCases() == null) {
      measure.setTestCases(new ArrayList<>());
    }

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
      measure.getTestCases().remove(existing);
    } else {
      // still allowing upsert
      testCase.setId(ObjectId.get().toString());
      testCase.setCreatedAt(now);
      testCase.setCreatedBy(username);
    }

    // try to persist the Patient Bundle to HAPI FHIR
    if (testCase == null || StringUtils.isBlank(testCase.getJson())) {
      testCase.setValidResource(false);
    } else {
      HapiOperationOutcome hapiOperationOutcome = validateTestCaseJson(testCase, accessToken);
      testCase.setHapiOperationOutcome(hapiOperationOutcome);
      if ((hapiOperationOutcome.getCode() >= 200 || hapiOperationOutcome.getCode() <= 299)
          && hapiOperationOutcome.isSuccessful()) {
        testCase.setValidResource(true);
      } else {
        testCase.setValidResource(false);
      }
    }

    measure.getTestCases().add(testCase);

    measureRepository.save(measure);

    log.info(
        "User [{}] successfully updated the test case with ID [{}] for the measure with ID[{}] ",
        username,
        testCase.getId(),
        measureId);
    return testCase;
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
    } else if (validate && !testCase.isValidResource()) {
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
    if (!hasPermissionToDelete(username, measure)) {
      log.info(
          "User [{}] is not authorized to delete the test case with ID [{}] from measure [{}]",
          username,
          testCaseId,
          measureId);
      throw new UnauthorizedException("Measure", measureId, username);
    }
    if (CollectionUtils.isEmpty(measure.getTestCases())) {
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

  public Measure findMeasureById(String measureId) {
    Measure measure = measureRepository.findById(measureId).orElse(null);
    if (measure == null) {
      throw new ResourceNotFoundException("Measure", measureId);
    }
    return measure;
  }

  private Boolean hasPermissionToDelete(String username, Measure measure) {
    return username.equals(measure.getCreatedBy())
        || (!CollectionUtils.isEmpty(measure.getAcls())
            && measure.getAcls().stream()
                .anyMatch(
                    acl ->
                        acl.getUserId().equals(username)
                            && acl.getRoles().stream()
                                .anyMatch(role -> role.equals(RoleEnum.SHARED_WITH))));
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
}
