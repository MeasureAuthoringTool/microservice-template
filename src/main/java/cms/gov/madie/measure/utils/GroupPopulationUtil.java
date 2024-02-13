package cms.gov.madie.measure.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.Stratification;

public class GroupPopulationUtil {
  private GroupPopulationUtil() {}

  public static boolean isAllGroupsAndPopulationsMatching(
      List<Group> originalGroups, List<Group> newGroups) {
    if (CollectionUtils.isEmpty(originalGroups) && CollectionUtils.isEmpty(newGroups)) {
      return true;
    } else if ((CollectionUtils.isEmpty(originalGroups) && !CollectionUtils.isEmpty(newGroups))
        || (CollectionUtils.isEmpty(newGroups) && !CollectionUtils.isEmpty(originalGroups))) {
      return false;
    } else {
      for (int i = 0; i < originalGroups.size(); i++) {
        for (int j = 0; j < newGroups.size(); j++) {
          if (isGroupPopulationsMatching(originalGroups.get(i), newGroups.get(j))) {
            return true;
          }
        }
      }
    }
    return false;
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
}
