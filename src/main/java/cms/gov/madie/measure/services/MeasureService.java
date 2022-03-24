package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.TestCase;
import cms.gov.madie.measure.models.TestCaseGroupPopulation;
import cms.gov.madie.measure.repositories.MeasureRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        if (!(existingGroup.getScoring() != null && existingGroup.getScoring().equals(group.getScoring())) ||
            (existingGroup.getScoring() == null && group.getScoring() != null)) {
          measure.setTestCases(clearPopulationValuesForGroup(existingGroup.getId(), measure.getTestCases()));
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

  public List<TestCase> clearPopulationValuesForGroup(String groupId, List<TestCase> testCases) {
    if (testCases == null || testCases.isEmpty() || groupId == null || groupId.isEmpty()) {
      return testCases;
    }

    return testCases.stream().map(tc -> {
      List<TestCaseGroupPopulation> groupPopulations = tc.getGroupPopulations() != null
          ? tc.getGroupPopulations().stream()
          .filter(gp -> !groupId.equals(gp.getGroupId()))
          .map(gp -> gp.toBuilder().build()).collect(Collectors.toList()) : tc.getGroupPopulations();
      return tc.toBuilder().groupPopulations(groupPopulations).build();
    }).collect(Collectors.toList());
  }
}
