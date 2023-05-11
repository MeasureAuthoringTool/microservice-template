package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@ChangeUnit(id = "move_measure_acls", order = "1", author = "madie_dev")
public class MoveMeasureAclsToMeasureSetChangeUnit {

  @Execution
  public void addMeasureSetValues(
      MeasureSetRepository measureSetRepository, MeasureRepository measureRepository) {
    List<Measure> measures = measureRepository.findAll();
    measures.forEach(
        measure -> {
          List<AclSpecification> acls = measure.getAcls();
          if (CollectionUtils.isNotEmpty(acls)) {
            MeasureSet measureSet =
                measureSetRepository.findByMeasureSetId(measure.getMeasureSetId()).orElse(null);
            if (measureSet != null) {
              // if no ACLs present in measure set, add all measure ACLs to set
              if (CollectionUtils.isEmpty(measureSet.getAcls())) {
                measureSet.setAcls(acls);
              } else {
                // if measure set has acls, filter acls that do not exist
                List<AclSpecification> aclsToMove =
                    acls.stream().filter(acl -> !isAclExists(measureSet.getAcls(), acl)).toList();
                measureSet.getAcls().addAll(aclsToMove);
              }
              measureSetRepository.save(measureSet);
            }
          }
        });
  }

  @RollbackExecution
  public void rollbackExecution() {
    log.debug("Entering rollbackExecution()");
  }

  private boolean isAclExists(List<AclSpecification> acls, AclSpecification acl) {
    if (CollectionUtils.isEmpty(acls)) {
      return false;
    }
    Optional<AclSpecification> aclSpec =
        acls.stream().filter(a -> StringUtils.equals(a.getUserId(), acl.getUserId())).findFirst();
    return aclSpec.isPresent();
  }
}
