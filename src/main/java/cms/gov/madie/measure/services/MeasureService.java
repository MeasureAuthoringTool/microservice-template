package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.MeasurePopulationOption;
import cms.gov.madie.measure.models.MeasureScoring;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.models.TestCaseGroupPopulation;
import cms.gov.madie.measure.models.TestCasePopulationValue;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.resources.DuplicateKeyException;
import io.micrometer.core.instrument.util.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static cms.gov.madie.measure.utils.ScoringPopulationDefinition.SCORING_POPULATION_MAP;

@Service
public class MeasureService {
  private final MeasureRepository measureRepository;

  public MeasureService(MeasureRepository measureRepository) {
    this.measureRepository = measureRepository;
  }

  public Group createOrUpdateGroup(Group group, String measureId, String username) {
    Measure measure = measureRepository.findById(measureId).orElse(null);
    if (measure == null) {
      throw new ResourceNotFoundException("Measure", measureId);
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

        if (!(existingGroup.getScoring() != null
            && existingGroup.getScoring().equals(group.getScoring()))
            || existingGroup.getScoring() == null && group.getScoring() != null) {
          measure.setTestCases(resetPopulationValuesForGroup(group, measure.getTestCases()));
        }
        existingGroup.setScoring(group.getScoring());
        existingGroup.setPopulation(group.getPopulation());
      } else { // if not present, add into groups collection
        group.setId(ObjectId.get().toString());
        measure.getGroups().add(group);
      }
    }
    measure.setLastModifiedBy(username);
    measure.setLastModifiedAt(Instant.now());
    measureRepository.save(measure);
    return group;
  }

  /**
   * Loops over the test cases searching for any with groups that match the updating group.
   * If any match, the group population values for that group on the test case will be re-initialized
   * with all the populations for that new scoring type, with all expected and actual values
   * set to false.
   *
   * @param group
   * @param testCases
   * @return
   */
  public List<TestCase> resetPopulationValuesForGroup(Group group, List<TestCase> testCases) {
    if (testCases == null || testCases.isEmpty() || group == null || group.getId() == null
        || group.getId().isEmpty() || group.getScoring() == null || group.getScoring().isEmpty()) {
      return testCases;
    }

    return testCases
        .stream()
        .map(tc -> {
          List<TestCaseGroupPopulation> groupPopulations =
              tc.getGroupPopulations() != null ?
                  tc.getGroupPopulations().stream()
                      .map(gp -> group.getId().equals(gp.getGroupId()) ?
                          gp.toBuilder().scoring(group.getScoring()).populationValues(getDefaultPopulationValuesForScoring(group.getScoring())).build()
                          : gp.toBuilder().build())
                      .collect(Collectors.toList())
                  : tc.getGroupPopulations();
          return tc.toBuilder().groupPopulations(groupPopulations).build();
        })
        .collect(Collectors.toList());
  }

  public List<TestCasePopulationValue> getDefaultPopulationValuesForScoring(String scoring) {
    List<MeasurePopulationOption> options = SCORING_POPULATION_MAP.get(MeasureScoring.valueOfText(scoring));
    return options == null ? List.of() :
        options.stream().map(option ->
                TestCasePopulationValue
                    .builder()
                    .expected(false)
                    .actual(false)
                    .name(option.getMeasurePopulation())
                    .build())
            .collect(Collectors.toList());
  }

  public void checkDuplicateCqlLibraryName(String cqlLibraryName) {
    if (StringUtils.isNotEmpty(cqlLibraryName)
        && measureRepository.findByCqlLibraryName(cqlLibraryName).isPresent()) {
      throw new DuplicateKeyException(
          "cqlLibraryName", "CQL library with given name already exists.");
    }
  }
}
