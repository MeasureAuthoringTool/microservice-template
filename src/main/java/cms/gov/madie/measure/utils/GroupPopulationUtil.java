package cms.gov.madie.measure.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import cms.gov.madie.measure.exceptions.GroupPopulationDisplayIdException;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;

public class GroupPopulationUtil {
  private GroupPopulationUtil() {}

  public static void validatePopulations(Measure measure, Group group) {
    String existingGroupDisplayId = group.getDisplayId();
    String newGroupDisplayId = String.valueOf(getGroupNumber(group, measure.getGroups()));

    if (existingGroupDisplayId != null
        && !existingGroupDisplayId.equalsIgnoreCase("Group_" + String.valueOf(newGroupDisplayId))) {
      throw new GroupPopulationDisplayIdException("Invalid group display id.");
    }

    if (!CollectionUtils.isEmpty(group.getPopulations())) {
      List<Population> ips =
          group.getPopulations().stream()
              .filter(pop -> pop.getName().equals(PopulationType.INITIAL_POPULATION))
              .collect(Collectors.toList());

      for (int index = 0; index < group.getPopulations().size(); index++) {
        Population population = group.getPopulations().get(index);
        if (population.getDisplayId() != null
            && !population
                .getDisplayId()
                .equalsIgnoreCase(
                    getPopulationDisplayId(
                        population,
                        newGroupDisplayId,
                        (!CollectionUtils.isEmpty(ips) && ips.size() > 1),
                        index))) {
          throw new GroupPopulationDisplayIdException("Invalid group population display id.");
        }
      }
    }
  }

  static int getGroupNumber(Group group, List<Group> groups) {
    int groupNumber = 0;
    for (int i = 0; i < groups.size(); i++) {
      Group currentGroup = groups.get(i);
      if (currentGroup.getId().equals(group.getId())) {
        groupNumber = i + 1;
      }
    }
    return groupNumber;
  }

  static String getPopulationDisplayId(
      Population population, String groupNumber, boolean multipleIps, int index) {
    String newPopDisplayId = null;
    newPopDisplayId = population.getName().getDisplay().replace(" ", "") + "_" + groupNumber;
    if (multipleIps && (index == 0 || index == 1)) {
      newPopDisplayId = newPopDisplayId + "_" + (index + 1);
    }
    return newPopDisplayId;
  }
}
