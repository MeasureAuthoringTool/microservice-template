package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.TestCaseService;
import cms.gov.madie.measure.utils.QiCoreJsonUtil;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Slf4j
@ChangeUnit(id = "update_fhir_resource_ids", order = "1", author = "madie_dev")
public class UpdateTestCaseResourceFullUrlsChangeUnit {

  private final String MADiE_URL = "https://madie.cms.gov/";
  @Setter private List<Measure> tempMeasures;

  @Execution
  public void updateTestCaseResourceFullUrls(
      MeasureRepository measureRepository, TestCaseService testCaseService) {
    log.info("STARTING - update_testcase_json resource id and reference");
    List<Measure> measures = measureRepository.findAll();
    if (CollectionUtils.isNotEmpty(measures)) {
      setTempMeasures(measures);
      measures.stream()
          .filter(m -> ModelType.QI_CORE.getValue().equals(m.getModel()))
          .forEach(
              measure -> {
                List<TestCase> testCases = measure.getTestCases();
                if (CollectionUtils.isNotEmpty(testCases)) {
                  testCases.stream()
                      .filter(tc -> !StringUtils.isBlank(tc.getJson()))
                      .forEach(
                          testCase -> {
                            if (!QiCoreJsonUtil.isValidJson(testCase.getJson())) {
                              log.warn(
                                  "Skipping test case [{}] on measure [{}] as JSON is invalid",
                                  testCase.getId(),
                                  measure.getId());
                              return;
                            }
                            try {
                              String updatedJson = testCaseService.updateResourceFullUrls(testCase);
                              testCase.setJson(updatedJson);
                            } catch (Exception ex) {
                              log.info(
                                  "Error updating Measure [{}], TestCase [{}]",
                                  measure.getId(),
                                  testCase.getId(),
                                  ex);
                            }
                          });
                  measure.setTestCases(testCases);
                  measureRepository.save(measure);
                }
              });
    }
    log.info("COMPLETED - update_testcase_json_patient_uuid");
  }

  //  private String updateResourceIds(TestCaseService testCaseService, TestCase testCase)
  //      throws JsonProcessingException {
  //    ObjectMapper mapper = new ObjectMapper();
  //    JsonNode rootNode = mapper.readTree(testCase.getJson());
  //    JsonNode entry = rootNode.get("entry");
  //    Iterator<JsonNode> iterator = entry.iterator();
  //    while (iterator.hasNext()) {
  //      var theNode = iterator.next();
  //      var resourceNode = theNode.get("resource");
  //      if (resourceNode != null) {
  //        var resourceType = resourceNode.get("resourceType").asText();
  //        if (resourceType != null
  //            && !"Patient".equalsIgnoreCase(resourceType)
  //            && theNode.has("fullUrl")) {
  //          String newUrl = MADiE_URL + resourceType + "/" + resourceNode.get("id").asText();
  //          log.info(newUrl);
  //          ObjectNode node = (ObjectNode) theNode;
  //          node.put("fullUrl", newUrl);
  //        }
  //      }
  //    }
  //    ByteArrayOutputStream bout = testCaseService.getByteArrayOutputStream(mapper, rootNode);
  //    return bout.toString();
  //  }

  @RollbackExecution
  public void rollbackExecution(MeasureRepository measureRepository) {
    if (CollectionUtils.isNotEmpty(tempMeasures)) {
      tempMeasures.forEach(
          measure -> {
            measureRepository.save(measure);
          });
    }
  }
}
