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
import java.util.UUID;

@Slf4j
@ChangeUnit(id = "update_testcase_json_patient_uuid", order = "1", author = "madie_dev")
public class UpdateTestCaseJsonPatientUuidChangeUnit {

    @Setter
    private List<Measure> tempMeasures;

    @Execution
    public void updatedTestCaseJsonWithPatientUuid(MeasureRepository measureRepository, TestCaseService testCaseService) {
        log.info("STARTING - update_testcase_json_patient_uuid");
        List<Measure> measures = measureRepository.findAll();
        if (CollectionUtils.isNotEmpty(measures)) {
            setTempMeasures(measures);
            measures.stream()
                    .filter(m -> ModelType.QI_CORE.getValue().equals(m.getModel()))
                    .forEach(
                    measure -> {
                        List<TestCase> testCases = measure.getTestCases();
                        if (CollectionUtils.isNotEmpty(testCases)) {
                            testCases.stream().filter(tc -> !StringUtils.isBlank(tc.getJson())).forEach(
                                    testCase -> {
                                        if (!QiCoreJsonUtil.isValidJson(testCase.getJson())) {
                                            log.warn("Skipping test case [{}] on measure [{}] as JSON is invalid", testCase.getId(), measure.getId());
                                            return;
                                        }
                                        try {
                                            UUID patientIdUuid = testCase.getPatientId();
                                            if (patientIdUuid == null) {
                                                patientIdUuid = UUID.randomUUID();
                                                testCase.setPatientId(patientIdUuid);
                                                log.warn("Measure [{}], TestCase [{}] - patientId was missing, generated new UUID!",
                                                        measure.getId(), testCase.getId());
                                            }
                                            final String newPatientId = patientIdUuid.toString();
                                            final String oldFullUrl = QiCoreJsonUtil.getPatientFullUrl(testCase.getJson());

                                            // Refs update makes the assumption that the ref will start with Patient/
                                            String updatedJson = testCaseService.enforcePatientId(testCase);
                                            final String previousJson = updatedJson;
                                            updatedJson = QiCoreJsonUtil.replacePatientRefs(updatedJson, newPatientId);

                                            if (!StringUtils.isBlank(oldFullUrl)) {
                                                updatedJson = QiCoreJsonUtil.replaceFullUrlRefs(updatedJson, oldFullUrl, newPatientId);
                                            }

                                            if (previousJson.equals(updatedJson)) {
                                                log.warn("Measure [{}], TestCase [{}] - no patient refs were updated!", measure.getId(), testCase.getId());
                                            }

                                            testCase.setJson(updatedJson);

                                        } catch (Exception ex) {
                                            log.info("Error updating Measure [{}], TestCase [{}]", measure.getId(), testCase.getId(), ex);
                                        }
                                    });
                            measure.setTestCases(testCases);
                            measureRepository.save(measure);
                        }
                    });
        }
        log.info("COMPLETED - update_testcase_json_patient_uuid");
    }



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
