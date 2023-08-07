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
        List<Measure> measures = measureRepository.findAll();
        if (CollectionUtils.isNotEmpty(measures)) {
            setTempMeasures(measures);
            measures.stream()
                    .filter(m -> ModelType.QI_CORE.getValue().equals(m.getModel()))
                    .forEach(
                    measure -> {
                        log.info("processing measure {} with model {}", measure.getMeasureName(), measure.getModel());
                        List<TestCase> testCases = measure.getTestCases();
                        if (CollectionUtils.isNotEmpty(testCases)) {
                            testCases.stream().filter(tc -> !StringUtils.isBlank(tc.getJson())).forEach(
                                    testCase -> {
                                        try {
                                            UUID patientIdUuid = testCase.getPatientId();
                                            if (patientIdUuid == null) {
                                                patientIdUuid = UUID.randomUUID();
                                                testCase.setPatientId(patientIdUuid);
                                            }
                                            final String newPatientId = patientIdUuid.toString();

                                            String oldPatientId = QiCoreJsonUtil.getPatientId(testCase.getJson());
                                            String oldFullUrl = QiCoreJsonUtil.getPatientFullUrl(testCase.getJson());

                                            // Refs update makes the assumption that the ref will start with Patient/
                                            String updatedJson = testCaseService.enforcePatientId(testCase);
                                            final String previousJson = testCaseService.enforcePatientId(testCase);
                                            if (QiCoreJsonUtil.isUuid(oldPatientId)) {
                                                log.info("TestCase {} already had patient ID updated, so falling back to wider regex for patient refs", testCase.getId());
                                                updatedJson = QiCoreJsonUtil.replacePatientRefs(updatedJson, newPatientId);
                                            } else {
                                                log.info("TestCase {} used old ID to update patient refs ", testCase.getId());
                                                updatedJson = QiCoreJsonUtil.replacePatientRefs(updatedJson, oldPatientId, newPatientId);
                                            }

                                            if (!StringUtils.isBlank(oldFullUrl)) {
                                                updatedJson = QiCoreJsonUtil.replaceFullUrlRefs(updatedJson, oldFullUrl, newPatientId);
                                            }

                                            if (previousJson.equals(updatedJson)) {
                                                log.warn("TestCase {} - no patient refs were updated!", testCase.getId());
                                            }

                                            testCase.setJson(updatedJson);

                                        } catch (Exception ex) {
                                            log.info("Error updating measure {}, test case {}", measure.getId(), testCase.getId(), ex);
                                        }
                                    });
                            measure.setTestCases(testCases);

                            measureRepository.save(measure);
                        }
                    });
        }
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
