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
import java.util.stream.Collectors;

@Slf4j
@ChangeUnit(id = "move_measure_acls", order = "2", author = "madie_dev")
public class MoveMeasureAclsToMeasureSetChangeUnit {

  @Execution
  public void moveMeasureAClsToMeasureSet(
      MeasureSetRepository measureSetRepository, MeasureRepository measureRepository) {
    List<Measure> measures = measureRepository.findAll();
    measures.forEach(
        measure -> {
          if (CollectionUtils.isNotEmpty(measure.getAcls())) {
            // there are measures that have duplicate ACLs, remove duplicates.
            List<AclSpecification> acls =
              measure.getAcls().stream().distinct().collect(Collectors.toList());
            MeasureSet measureSet =
                measureSetRepository.findByMeasureSetId(measure.getMeasureSetId()).orElse(null);
            if (measureSet != null) {
              // if no ACLs present in measure set, add all measure ACLs to set
              if (CollectionUtils.isEmpty(measureSet.getAcls())) {
                measureSet.setAcls(acls);
                measureSetRepository.save(measureSet);
              } else {
                // if measure set has acls, filter measure acls that do not exist
                List<AclSpecification> aclsToMove =
                    acls.stream().filter(acl -> !isAclExists(measureSet.getAcls(), acl)).toList();
                measureSet.getAcls().addAll(aclsToMove);
                if (CollectionUtils.isNotEmpty(aclsToMove)) {
                  measureSetRepository.save(measureSet);
                }
              }
            }
          }
        });
  }

  @RollbackExecution
  public void rollbackExecution(MeasureSetRepository measureSetRepository) {
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
