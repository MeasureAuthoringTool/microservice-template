package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.resources.DuplicateKeyException;
import io.micrometer.core.instrument.util.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

  public void checkDuplicateCqlLibraryName(String cqlLibraryName) {
    if (StringUtils.isNotEmpty(cqlLibraryName)
        && measureRepository.findByCqlLibraryName(cqlLibraryName).isPresent()) {
      throw new DuplicateKeyException(
          "cqlLibraryName", "CQL library with given name already exists.");
    }
  }
}
