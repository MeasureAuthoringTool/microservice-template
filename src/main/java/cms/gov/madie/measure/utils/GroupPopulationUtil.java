package cms.gov.madie.measure.utils;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroupPopulationUtil {
  private GroupPopulationUtil() {}

  public static void setGroupAndPopulationsDisplayIds(Measure measure, Group group) {
    String groupNumber = String.valueOf(getGroupNumber(group, measure.getGroups()));

    // set group display id
    group.setDisplayId("Group_" + groupNumber);

    // set population display id
    if (!CollectionUtils.isEmpty(group.getPopulations())) {
      boolean hasMultipleIps =
          group.getPopulations().stream()
                  .filter(pop -> pop.getName().equals(PopulationType.INITIAL_POPULATION))
                  .count()
              > 1;

      for (int index = 0; index < group.getPopulations().size(); index++) {
        Population population = group.getPopulations().get(index);
        String popDisplayId =
            getPopulationDisplayId(population, groupNumber, hasMultipleIps, index);
        population.setDisplayId(popDisplayId);
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
    String newPopDisplayId = population.getName().getDisplay().replace(" ", "") + "_" + groupNumber;
    if (multipleIps && (index == 0 || index == 1)) {
      newPopDisplayId = newPopDisplayId + "_" + (index + 1);
    }
    return newPopDisplayId;
  }
}
