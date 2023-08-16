package cms.gov.madie.measure.utils;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Population;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class TestCaseServiceUtil {

  public List<Group> getGroupsWithValidPopulations(List<Group> originalGroups) {
    List<Group> changedGroups = null;
    if (!isEmpty(originalGroups)) {
      changedGroups = new ArrayList<>();
      for (Group group : originalGroups) {
        if (!isEmpty(group.getPopulations())) {
          List<Population> changedPopulations = new ArrayList<>();
          for (Population population : group.getPopulations()) {
            if (!StringUtils.isBlank(population.getDefinition())) {
              changedPopulations.add(population);
            }
          }
          group.setPopulations(changedPopulations);
        }
        changedGroups.add(group);
      }
    }
    return changedGroups;
  }
}
