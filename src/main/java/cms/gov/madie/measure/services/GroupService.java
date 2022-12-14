package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.validations.CqlDefinitionReturnTypeValidator;
import cms.gov.madie.measure.validations.CqlFunctionReturnTypeValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import gov.cms.madie.models.measure.TestCaseStratificationValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
      new CqlFunctionReturnTypeValidator()
          .validateCqlFunctionReturnTypes(group, measure.getElmJson());
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
    updateGroupForTestCases(group, measure.getTestCases());
    measure.setLastModifiedBy(username);
    measure.setLastModifiedAt(Instant.now());
    measureRepository.save(measure);
    return group;
  }

  /**
   * Update test case group if there are changes to the measure group as: 1. if
   * population/stratification added to the measure group, add it to the test case group 2. If
   * population/stratification removed from the measure group, add it to the test case group 3. if
   * Observation is removed from the measure group(for Ratio), remove it's all associated
   * observations from test case group 4. If group scoring or population basis changed, remove the
   * group from test case groups
   *
   * @param group Group being changed
   * @param testCases TestCases to iterate over and update
   */
  public void updateGroupForTestCases(Group group, List<TestCase> testCases) {
    if (group != null && !CollectionUtils.isEmpty(testCases)) {
      testCases.forEach(
          testCase -> {
            List<TestCaseGroupPopulation> testCaseGroups = testCase.getGroupPopulations();
            // if test case has groups
            if (!CollectionUtils.isEmpty(testCaseGroups)) {
              TestCaseGroupPopulation testCaseGroupPopulation =
                  testCaseGroups.stream()
                      .filter(
                          testCaseGroup ->
                              StringUtils.equals(group.getId(), testCaseGroup.getGroupId()))
                      .findAny()
                      .orElse(null);
              // if this is not new group
              if (testCaseGroupPopulation != null) {
                // if scoring & population basis not changed, merge the updates
                if (StringUtils.equals(testCaseGroupPopulation.getScoring(), group.getScoring())
                    && StringUtils.equals(
                        testCaseGroupPopulation.getPopulationBasis(), group.getPopulationBasis())) {
                  updateTestCaseGroupWithMeasureGroup(testCaseGroupPopulation, group);
                } else {
                  removeGroupFromTestCase(group.getId(), testCase);
                }
              }
            }
          });
    }
  }

  public Measure deleteMeasureGroup(String measureId, String groupId, String username) {

    if (measureId == null || measureId.trim().isEmpty()) {
      throw new InvalidIdException("Measure Id cannot be null");
    }
    Measure measure = measureRepository.findById(measureId).orElse(null);
    if (measure == null) {
      throw new ResourceNotFoundException("Measure", measureId);
    }

    if (!username.equalsIgnoreCase(measure.getCreatedBy())
        && (CollectionUtils.isEmpty(measure.getAcls())
            || !measure.getAcls().stream()
                .anyMatch(
                    acl ->
                        acl.getUserId().equalsIgnoreCase(username)
                            && acl.getRoles().stream()
                                .anyMatch(role -> role.equals(RoleEnum.SHARED_WITH))))) {
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
    List<TestCase> testCases = measure.getTestCases();
    removeGroupFromTestCases(groupId, testCases);
    return measureRepository.save(measure);
  }

  public void updateTestCaseGroupWithMeasureGroup(
      TestCaseGroupPopulation testCaseGroup, Group measureGroup) {
    // update test case populations based on measure group population
    List<TestCasePopulationValue> populations =
        measureGroup.getPopulations().stream()
            .map(measurePopulation -> updateTestCasePopulation(measurePopulation, testCaseGroup))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    // retain observations for CV group if there are any
    if (StringUtils.equals(
        measureGroup.getScoring(), MeasureScoring.CONTINUOUS_VARIABLE.toString())) {
      populations.addAll(findTestCaseObservations(testCaseGroup));
    } else if (StringUtils.equals(measureGroup.getScoring(), MeasureScoring.RATIO.toString())
        && !CollectionUtils.isEmpty(measureGroup.getMeasureObservations())) {
      //  retain all observations if both denominator and numerator observations present
      if (measureGroup.getMeasureObservations().size() == 2) {
        populations.addAll(findTestCaseObservations(testCaseGroup));
      } else {
        MeasureObservation observation = measureGroup.getMeasureObservations().get(0);
        Population population =
            findMeasurePopulation(observation.getCriteriaReference(), measureGroup);
        List<TestCasePopulationValue> observations;
        if (population.getName() == PopulationType.DENOMINATOR) {
          observations =
              findTestCasePopulationsContainingId(testCaseGroup, "denominatorObservation");
        } else {
          observations = findTestCasePopulationsContainingId(testCaseGroup, "numeratorObservation");
        }
        populations.addAll(observations);
      }
    }
    testCaseGroup.setPopulationValues(populations);

    // update test case stratification based on measure group stratification
    if (CollectionUtils.isEmpty(measureGroup.getStratifications())) {
      testCaseGroup.setStratificationValues(List.of());
    } else {
      AtomicInteger count = new AtomicInteger(1);
      List<TestCaseStratificationValue> stratification =
          measureGroup.getStratifications().stream()
              .map(
                  measureStrata -> {
                    String strataName =
                        String.format(
                            "Strata-%d %s",
                            count.getAndIncrement(), measureStrata.getAssociation().getDisplay());
                    return updateTestCaseStratification(measureStrata, testCaseGroup, strataName);
                  })
              .filter(Objects::nonNull)
              .toList();
      testCaseGroup.setStratificationValues(stratification);
    }
  }

  public void removeGroupFromTestCases(String groupId, List<TestCase> testCases) {
    if (StringUtils.isNotBlank(groupId) && !CollectionUtils.isEmpty(testCases)) {
      testCases.forEach(testCase -> removeGroupFromTestCase(groupId, testCase));
    }
  }

  private TestCasePopulationValue updateTestCasePopulation(
      Population population, TestCaseGroupPopulation testCaseGroup) {
    // if no cql definition(optional pop or unselected), no need to consider population
    if (StringUtils.isEmpty(population.getDefinition())) {
      return null;
    }
    // find matching population
    TestCasePopulationValue testCasePopulation =
        findTestCasePopulation(population.getId(), testCaseGroup);
    // if population did not find, create new one
    if (testCasePopulation == null) {
      testCasePopulation =
          TestCasePopulationValue.builder()
              .id(population.getId())
              .expected(null)
              .actual(null)
              .name(population.getName())
              .build();
    }
    return testCasePopulation;
  }

  private TestCaseStratificationValue updateTestCaseStratification(
      Stratification stratification, TestCaseGroupPopulation testCaseGroup, String strataName) {
    // if no cql definition(optional), no need to consider stratification
    if (StringUtils.isEmpty(stratification.getCqlDefinition())) {
      return null;
    }

    TestCaseStratificationValue testCaseStrata = null;
    if (!CollectionUtils.isEmpty(testCaseGroup.getStratificationValues())) {
      // find matching strata
      testCaseStrata =
          testCaseGroup.getStratificationValues().stream()
              .filter(testStrata -> StringUtils.equals(testStrata.getId(), stratification.getId()))
              .findAny()
              .orElse(null);
    }
    // if population did not find, create new one
    if (testCaseStrata == null) {
      testCaseStrata =
          TestCaseStratificationValue.builder()
              .id(stratification.getId())
              .expected(null)
              .actual(null)
              .build();
    }
    testCaseStrata.setName(strataName);
    return testCaseStrata;
  }

  private TestCasePopulationValue findTestCasePopulation(
      String populationId, TestCaseGroupPopulation testCaseGroup) {
    return testCaseGroup.getPopulationValues().stream()
        .filter(testPopulation -> StringUtils.equals(testPopulation.getId(), populationId))
        .findAny()
        .orElse(null);
  }

  private Population findMeasurePopulation(String populationId, Group group) {
    return group.getPopulations().stream()
        .filter(testPopulation -> StringUtils.equals(testPopulation.getId(), populationId))
        .findAny()
        .orElse(null);
  }

  private List<TestCasePopulationValue> findTestCaseObservations(
      TestCaseGroupPopulation testCaseGroup) {
    return testCaseGroup.getPopulationValues().stream()
        .filter(
            p ->
                p.getName() == PopulationType.MEASURE_OBSERVATION
                    || p.getName() == PopulationType.MEASURE_POPULATION_OBSERVATION
                    || p.getName() == PopulationType.DENOMINATOR_OBSERVATION
                    || p.getName() == PopulationType.NUMERATOR_OBSERVATION)
        .collect(Collectors.toList());
  }

  private List<TestCasePopulationValue> findTestCasePopulationsContainingId(
      TestCaseGroupPopulation testCaseGroup, String id) {
    return testCaseGroup.getPopulationValues().stream()
        .filter(p -> StringUtils.contains(p.getId(), id))
        .collect(Collectors.toList());
  }

  private void removeGroupFromTestCase(String groupId, TestCase testCase) {
    if (testCase.getGroupPopulations() != null) {
      List<TestCaseGroupPopulation> remainingGroups =
          testCase.getGroupPopulations().stream()
              .filter(group -> !groupId.equals(group.getGroupId()))
              .toList();
      testCase.setGroupPopulations(remainingGroups);
    }
  }
}
