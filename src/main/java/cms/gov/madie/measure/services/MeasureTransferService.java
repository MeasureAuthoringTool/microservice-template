package cms.gov.madie.measure.services;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import cms.gov.madie.measure.utils.GroupPopulationUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;

@Slf4j
@Service
@AllArgsConstructor
public class MeasureTransferService {

  public Measure overwriteExistingMeasure(
      List<Measure> measuresWithSameSetId, Measure transferredMeasure) {
    List<Measure> draftdMeasures =
        measuresWithSameSetId.stream()
            .filter(
                measure ->
                    measure.getMeasureMetaData() != null && measure.getMeasureMetaData().isDraft())
            .collect(Collectors.toList());
    if (!CollectionUtils.isEmpty(draftdMeasures)) {
      draftdMeasures.sort(Comparator.comparing(Measure::getLastModifiedAt));
      Measure mostRecentMeasure = draftdMeasures.get(draftdMeasures.size() - 1);
      transferredMeasure.setId(mostRecentMeasure.getId());
      if (GroupPopulationUtil.areGroupsAndPopulationsMatching(
          mostRecentMeasure.getGroups(), transferredMeasure.getGroups())) {
        List<TestCase> originalTestCases = mostRecentMeasure.getTestCases();
        if (CollectionUtils.isNotEmpty(originalTestCases)) {
          // test cases that need to be carried over to the new transferred measure,
          // however, groupPopulations need to be wiped out because there is no way
          // to map the group ids to multiple groups, if the group ids are different
          // then execution will blow up. Users have to manually input expected values
          originalTestCases =
              originalTestCases.stream()
                  .map(
                      testCase ->
                          TestCase.builder()
                              .id(testCase.getId())
                              .name(testCase.getName())
                              .title(testCase.getTitle())
                              .series(testCase.getSeries())
                              .description(testCase.getDescription())
                              .createdAt(testCase.getCreatedAt())
                              .createdBy(testCase.getCreatedBy())
                              .lastModifiedAt(testCase.getLastModifiedAt())
                              .lastModifiedBy(testCase.getLastModifiedBy())
                              .resourceUri(testCase.getResourceUri())
                              .validResource(testCase.isValidResource())
                              .json(testCase.getJson())
                              .patientId(testCase.getPatientId())
                              .hapiOperationOutcome(testCase.getHapiOperationOutcome())
                              .build())
                  .collect(Collectors.toList());
        }
        transferredMeasure.setTestCases(originalTestCases);
        log.info(
            "Overwrite meausre id {} with the testcases from original measure",
            mostRecentMeasure.getId());
      }
    }
    return transferredMeasure;
  }
}
