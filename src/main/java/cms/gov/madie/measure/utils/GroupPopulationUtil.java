package cms.gov.madie.measure.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.bson.types.ObjectId;

import org.apache.commons.lang3.StringUtils;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;

public class GroupPopulationUtil {
  private GroupPopulationUtil() {}

  public static boolean areGroupsAndPopulationsMatching(
      List<Group> originalGroups, List<Group> newGroups) {
    boolean match = false;
    if (!CollectionUtils.isEmpty(originalGroups) && !CollectionUtils.isEmpty(newGroups)) {
      List<Group> matchedGroups =
          originalGroups.stream()
              .filter(
                  originalGroup ->
                      newGroups.stream()
                          .anyMatch(
                              newGroup -> isGroupPopulationsMatching(originalGroup, newGroup)))
              .toList();
      match = CollectionUtils.isNotEmpty(matchedGroups);
    }
    return match;
  }

  public static boolean isGroupPopulationsMatching(Group originalGroup, Group newGroup) {
    List<Population> originalValidPops = getValidPopulations(originalGroup);
    List<Population> newValidPops = getValidPopulations(newGroup);
    boolean populationsMatch = isPopulationMatch(originalValidPops, newValidPops);
    if (!populationsMatch) {
      return false;
    }

    List<MeasureObservation> originalValidObs = getValidObservations(originalGroup);
    List<MeasureObservation> newValidObs = getValidObservations(newGroup);
    boolean observationsMatch = isMeasureObservationMatch(originalValidObs, newValidObs);
    if (!observationsMatch) {
      return false;
    }

    List<Stratification> originalValidStras = getValidStratifications(originalGroup);
    List<Stratification> newValidStras = getValidStratifications(newGroup);
    boolean stratificationMatch = isStratificationMatch(originalValidStras, newValidStras);

    return stratificationMatch;
  }

  static List<Population> getValidPopulations(Group group) {
    if (group != null && !CollectionUtils.isEmpty(group.getPopulations())) {
      return group.getPopulations().stream()
          .filter(population -> !StringUtils.isBlank(population.getDefinition()))
          .collect(Collectors.toList());
    }
    return null;
  }

  static boolean isPopulationMatch(
      List<Population> originalValidPops, List<Population> newValidPops) {
    if (CollectionUtils.isEmpty(originalValidPops) && CollectionUtils.isEmpty(newValidPops)) {
      return true;
    } else {
      if (originalValidPops.size() != newValidPops.size()) {
        return false;
      }
      List<Population> nonMatch =
          originalValidPops.stream()
              .filter(
                  originalPop ->
                      newValidPops.stream()
                          .noneMatch(
                              newPop ->
                                  newPop
                                      .getDefinition()
                                      .equalsIgnoreCase(originalPop.getDefinition())))
              .toList();
      if (!CollectionUtils.isEmpty(nonMatch)) {
        return false;
      }
    }
    return true;
  }

  static List<MeasureObservation> getValidObservations(Group group) {
    if (group != null && !CollectionUtils.isEmpty(group.getMeasureObservations())) {
      return group.getMeasureObservations().stream()
          .filter(observation -> !StringUtils.isBlank(observation.getDefinition()))
          .collect(Collectors.toList());
    }
    return null;
  }

  static boolean isMeasureObservationMatch(
      List<MeasureObservation> originalValidObs, List<MeasureObservation> newValidObs) {
    if (CollectionUtils.isEmpty(originalValidObs) && CollectionUtils.isEmpty(newValidObs)) {
      return true;
    } else {
      if (originalValidObs.size() != newValidObs.size()) {
        return false;
      }
      List<MeasureObservation> nonMatch =
          originalValidObs.stream()
              .filter(
                  originalObs ->
                      newValidObs.stream()
                          .noneMatch(
                              newObs ->
                                  newObs
                                      .getDefinition()
                                      .equalsIgnoreCase(originalObs.getDefinition())))
              .toList();
      if (!CollectionUtils.isEmpty(nonMatch)) {
        return false;
      }
    }
    return true;
  }

  static List<Stratification> getValidStratifications(Group group) {
    if (group != null && !CollectionUtils.isEmpty(group.getStratifications())) {
      return group.getStratifications().stream()
          .filter(stratification -> !StringUtils.isBlank(stratification.getCqlDefinition()))
          .collect(Collectors.toList());
    }
    return null;
  }

  static boolean isStratificationMatch(
      List<Stratification> originalValidStrats, List<Stratification> newValidStrats) {
    if (CollectionUtils.isEmpty(originalValidStrats) && CollectionUtils.isEmpty(newValidStrats)) {
      return true;
    } else {
      if (originalValidStrats.size() != newValidStrats.size()) {
        return false;
      }
      List<Stratification> nonMatch =
          originalValidStrats.stream()
              .filter(
                  originalStrats ->
                      newValidStrats.stream()
                          .noneMatch(
                              newStrats ->
                                  newStrats
                                      .getCqlDefinition()
                                      .equalsIgnoreCase(originalStrats.getCqlDefinition())))
              .toList();
      if (!CollectionUtils.isEmpty(nonMatch)) {
        return false;
      }
    }
    return true;
  }

  /**
   * This method is to order the group populations in the order defined as in:
   * cms.gov.madie.measure.utils.TestCaseServiceUtil. When the test cases are imported, the group
   * populations are matched one by one in the order. Also, test case execution also needs the group
   * populations in the right order.
   *
   * @param the list of the groups
   * @return none
   */
  public static void reorderGroupPopulations(List<Group> groups) {
    List<Population> newPopulations;
    if (!CollectionUtils.isEmpty(groups)) {
      for (Group group : groups) {
        newPopulations = new ArrayList<>();
        List<Population> populations = group.getPopulations();
        if (!CollectionUtils.isEmpty(populations)) {
          newPopulations.add(findPopulation(populations, PopulationType.INITIAL_POPULATION));
          if (StringUtils.equals(
              group.getScoring(), MeasureScoring.CONTINUOUS_VARIABLE.toString())) {
            newPopulations.add(findPopulation(populations, PopulationType.MEASURE_POPULATION));
            newPopulations.add(
                findPopulation(populations, PopulationType.MEASURE_POPULATION_EXCLUSION));
          } else {
            if (!StringUtils.equals(group.getScoring(), MeasureScoring.COHORT.toString())) {
              newPopulations.add(findPopulation(populations, PopulationType.DENOMINATOR));
              newPopulations.add(findPopulation(populations, PopulationType.DENOMINATOR_EXCLUSION));
              newPopulations.add(findPopulation(populations, PopulationType.NUMERATOR));
              newPopulations.add(findPopulation(populations, PopulationType.NUMERATOR_EXCLUSION));
              if (!StringUtils.equals(group.getScoring(), MeasureScoring.RATIO.toString())) {
                newPopulations.add(
                    findPopulation(populations, PopulationType.DENOMINATOR_EXCEPTION));
              }
            }
          }
          group.setPopulations(newPopulations);
        }
      }
    }
  }

  private static Population findPopulation(
      List<Population> populations, PopulationType populationType) {
    return populations.stream()
        .filter(population -> population.getName() == populationType)
        .findFirst()
        .orElse(
            Population.builder()
                .id(ObjectId.get().toString())
                .name(populationType)
                .definition("")
                .build());
  }
}
