package cms.gov.madie.measure.services;

import cms.gov.madie.measure.HapiFhirConfig;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import cms.gov.madie.measure.repositories.MeasureRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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
      if(!validateUserPermissions(existing, testCase, measure.getCreatedBy(), username)) {
        throw new UnauthorizedException("TestCase", testCase.getId(), username);
      }
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
    testCase.setHapiOperationOutcome(validateTestCaseJson(testCase, accessToken));
    measure.getTestCases().add(testCase);

    measureRepository.save(measure);

    log.info(
        "User [{}] successfully updated the test case with ID [{}] for the measure with ID[{}] ",
        username,
        testCase.getId(),
        measureId);
    return testCase;
  }

  /**
   * Returns false if any of the following are detected:
   *  - non-owner tries to modify test case expected values
   * @param existingTestCase Current state of test case to update, pulled from the database
   * @param updatingTestCase Test case with updated values
   * @param measureCreator Owner/Creator of measure
   * @param username User attempting to update the test case
   * @return true if user permissions are valid, false otherwise
   */
  public boolean validateUserPermissions(TestCase existingTestCase, TestCase updatingTestCase,
                                         final String measureCreator, final String username) {
    // TODO: check how strict the permission validation should be. Currently in line with ACs from MAT-4666
    if (existingTestCase == null || updatingTestCase == null ||
        existingTestCase.getGroupPopulations() == null || existingTestCase.getGroupPopulations().isEmpty() ||
        updatingTestCase.getGroupPopulations() == null || updatingTestCase.getGroupPopulations().isEmpty()) {
      return true;
    }
    // Owner can change anything - separated out for clarity
    if (StringUtils.equals(measureCreator, username)) {
      return true;
    }
    // verify that expected values are not changing
    List<TestCaseGroupPopulation> existingGroupPopulations = existingTestCase.getGroupPopulations();
    for (var existingGroupPop : existingGroupPopulations) {
      if (existingGroupPop == null) {
        continue;
      }
      TestCaseGroupPopulation updatingGroupPop = updatingTestCase.getGroupPopulations()
          .stream()
          .filter(tcgp -> StringUtils.equals(existingGroupPop.getGroupId(), tcgp.getGroupId()))
          .findFirst()
          .orElse(null);
      if (updatingGroupPop != null) {
        if ((updatingGroupPop.getPopulationValues() == null && existingGroupPop.getPopulationValues() != null) ||
            (updatingGroupPop.getPopulationValues() != null && existingGroupPop.getPopulationValues() == null) ||
            (updatingGroupPop.getPopulationValues().size() != existingGroupPop.getPopulationValues().size()) ||
            !updatingGroupPop.getPopulationValues().containsAll(existingGroupPop.getPopulationValues())
        ) {
          return false;
        }
      }
    }
    return true;
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

  public Measure findMeasureById(String measureId) {
    Measure measure = measureRepository.findById(measureId).orElse(null);
    if (measure == null) {
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
}
