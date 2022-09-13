package cms.gov.madie.measure.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.validations.CqlDefinitionReturnTypeValidator;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class GroupService {

  private final MeasureRepository measureRepository;

  public Group createOrUpdateGroup(Group group, String measureId, String username) {
    Measure measure = measureRepository.findById(measureId).orElse(null);
    if (measure == null) {
      throw new ResourceNotFoundException("Measure", measureId);
    }
    try {
      new CqlDefinitionReturnTypeValidator()
          .validateCqlDefinitionReturnTypes(group, measure.getElmJson());
    } catch (JsonProcessingException ex) {
      log.error(
          "An error occurred while validating population "
              + "definition return types for measure {}",
          measure.getId(),
          ex);
      throw new InvalidIdException("Invalid elm json");
    }
    // no group present, this is the first group
    if (CollectionUtils.isEmpty(measure.getGroups())) {
      group.setId(ObjectId.get().toString());
      measure.setGroups(List.of(group));
    } else {
      Optional<Group> existingGroupOpt =
          measure.getGroups().stream().filter(g -> g.getId().equals(group.getId())).findFirst();
      // if group already exists, just update it
      if (existingGroupOpt.isPresent()) {
        Group existingGroup = existingGroupOpt.get();
        existingGroup.setScoring(group.getScoring());
        existingGroup.setPopulations(group.getPopulations());
        existingGroup.setMeasureObservations(group.getMeasureObservations());
        existingGroup.setGroupDescription(group.getGroupDescription());
        existingGroup.setImprovementNotation(group.getImprovementNotation());
        existingGroup.setRateAggregation(group.getRateAggregation());
        existingGroup.setMeasureGroupTypes(group.getMeasureGroupTypes());
        existingGroup.setScoringUnit(group.getScoringUnit());
        existingGroup.setStratifications(group.getStratifications());
        existingGroup.setPopulationBasis(group.getPopulationBasis());
      } else { // if not present, add into groups collection
        group.setId(ObjectId.get().toString());
        measure.getGroups().add(group);
      }
    }
    measure.setTestCases(setPopulationValuesForGroup(group, measure.getTestCases()));
    measure.setLastModifiedBy(username);
    measure.setLastModifiedAt(Instant.now());
    measureRepository.save(measure);
    return group;
  }

  /**
   * Loops over the test cases searching for any with groups that match the updating group. If any
   * match, then the test case group populations will be updated with the measure group populations,
   * along with expected and actual values set to false.
   *
   * @param group Group being changed
   * @param testCases TestCases to iterate over and update
   * @return TestCases updated with new scoring type (if any groups matched)
   */
  public List<TestCase> setPopulationValuesForGroup(Group group, List<TestCase> testCases) {
    if (testCases == null
        || testCases.isEmpty()
        || group == null
        || group.getId() == null
        || group.getId().isEmpty()
        || group.getScoring() == null
        || group.getScoring().isEmpty()) {
      return testCases;
    }

    return testCases.stream()
        .map(
            testCase -> {
              List<TestCaseGroupPopulation> testCaseGroupPopulations =
                  // When a group is not created but test case is created
                  // then when we add a group, since the testCaseGroupPopulations is null, they will
                  // never is saved in mongo
                  testCase.getGroupPopulations() != null
                      ? testCase.getGroupPopulations().stream()
                          .map(
                              testCaseGroupPopulation ->
                                  group.getId().equals(testCaseGroupPopulation.getGroupId())
                                      ? testCaseGroupPopulation
                                          .toBuilder()
                                          .scoring(group.getScoring())
                                          .populationValues(
                                              getTestCasePopulationsForMeasureGroupPopulations(
                                                  group))
                                          .build()
                                      : testCaseGroupPopulation.toBuilder().build())
                          .collect(Collectors.toList())
                      : testCase.getGroupPopulations();
              return testCase.toBuilder().groupPopulations(testCaseGroupPopulations).build();
            })
        .collect(Collectors.toList());
  }

  public Measure deleteMeasureGroup(String measureId, String groupId, String username) {

    if (measureId == null || measureId.trim().isEmpty()) {
      throw new InvalidIdException("Measure Id cannot be null");
    }
    Measure measure = measureRepository.findById(measureId).orElse(null);
    if (measure == null) {
      throw new ResourceNotFoundException("Measure", measureId);
    }

    if (!username.equals(measure.getCreatedBy())) {
      throw new UnauthorizedException("Measure", measureId, username);
    }

    if (groupId == null || groupId.trim().isEmpty()) {
      throw new InvalidIdException("Measure group Id cannot be null");
    }

    List<Group> remainingGroups =
        measure.getGroups().stream().filter(g -> !g.getId().equals(groupId)).toList();

    // to check if given group id is present
    if (remainingGroups.size() == measure.getGroups().size()) {
      throw new ResourceNotFoundException("Group", groupId);
    }

    measure.setGroups(remainingGroups);
    log.info(
        "User [{}] has successfully deleted a group with Id [{}] from measure [{}]",
        username,
        groupId,
        measure.getId());
    return measureRepository.save(measure);
  }

  /** @return a list of TestCasePopulationValues for those defines are assigned. */
  private List<TestCasePopulationValue> getTestCasePopulationsForMeasureGroupPopulations(
      Group group) {
    List<Population> groupPopulations = group.getPopulations();
    if (CollectionUtils.isEmpty(groupPopulations)) {
      return List.of();
    } else {
      return groupPopulations.stream()
          .map(
              groupPopulation ->
                  TestCasePopulationValue.builder()
                      .expected(false)
                      .actual(false)
                      .name(groupPopulation.getName())
                      .build())
          .collect(Collectors.toList());
    }
  }
}
