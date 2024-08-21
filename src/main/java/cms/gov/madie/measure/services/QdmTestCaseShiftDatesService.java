package cms.gov.madie.measure.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cms.gov.madie.measure.exceptions.CqmConversionException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import gov.cms.madie.models.cqm.datacriteria.AdverseEvent;
import gov.cms.madie.models.cqm.datacriteria.AllergyIntolerance;
import gov.cms.madie.models.cqm.datacriteria.AssessmentOrder;
import gov.cms.madie.models.cqm.datacriteria.AssessmentPerformed;
import gov.cms.madie.models.cqm.datacriteria.AssessmentRecommended;
import gov.cms.madie.models.cqm.datacriteria.CareGoal;
import gov.cms.madie.models.cqm.datacriteria.CommunicationPerformed;
import gov.cms.madie.models.cqm.datacriteria.DeviceOrder;
import gov.cms.madie.models.cqm.datacriteria.DeviceRecommended;
import gov.cms.madie.models.cqm.datacriteria.Diagnosis;
import gov.cms.madie.models.cqm.datacriteria.DiagnosticStudyOrder;
import gov.cms.madie.models.cqm.datacriteria.DiagnosticStudyPerformed;
import gov.cms.madie.models.cqm.datacriteria.DiagnosticStudyRecommended;
import gov.cms.madie.models.cqm.datacriteria.EncounterOrder;
import gov.cms.madie.models.cqm.datacriteria.EncounterPerformed;
import gov.cms.madie.models.cqm.datacriteria.EncounterRecommended;
import gov.cms.madie.models.cqm.datacriteria.FamilyHistory;
import gov.cms.madie.models.cqm.datacriteria.ImmunizationAdministered;
import gov.cms.madie.models.cqm.datacriteria.ImmunizationOrder;
import gov.cms.madie.models.cqm.datacriteria.InterventionOrder;
import gov.cms.madie.models.cqm.datacriteria.InterventionPerformed;
import gov.cms.madie.models.cqm.datacriteria.InterventionRecommended;
import gov.cms.madie.models.cqm.datacriteria.LaboratoryTestOrder;
import gov.cms.madie.models.cqm.datacriteria.LaboratoryTestPerformed;
import gov.cms.madie.models.cqm.datacriteria.LaboratoryTestRecommended;
import gov.cms.madie.models.cqm.datacriteria.MedicationActive;
import gov.cms.madie.models.cqm.datacriteria.MedicationAdministered;
import gov.cms.madie.models.cqm.datacriteria.MedicationDischarge;
import gov.cms.madie.models.cqm.datacriteria.MedicationDispensed;
import gov.cms.madie.models.cqm.datacriteria.MedicationOrder;
import gov.cms.madie.models.cqm.datacriteria.Participation;
import gov.cms.madie.models.cqm.datacriteria.PatientCareExperience;
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristic;
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristicBirthdate;
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristicClinicalTrialParticipant;
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristicEthnicity;
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristicExpired;
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristicPayer;
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristicRace;
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristicSex;
import gov.cms.madie.models.cqm.datacriteria.PhysicalExamOrder;
import gov.cms.madie.models.cqm.datacriteria.PhysicalExamPerformed;
import gov.cms.madie.models.cqm.datacriteria.PhysicalExamRecommended;
import gov.cms.madie.models.cqm.datacriteria.ProcedureOrder;
import gov.cms.madie.models.cqm.datacriteria.ProcedurePerformed;
import gov.cms.madie.models.cqm.datacriteria.ProcedureRecommended;
import gov.cms.madie.models.cqm.datacriteria.ProviderCareExperience;
import gov.cms.madie.models.cqm.datacriteria.RelatedPerson;
import gov.cms.madie.models.cqm.datacriteria.SubstanceAdministered;
import gov.cms.madie.models.cqm.datacriteria.SubstanceOrder;
import gov.cms.madie.models.cqm.datacriteria.SubstanceRecommended;
import gov.cms.madie.models.cqm.datacriteria.Symptom;
import gov.cms.madie.models.cqm.datacriteria.basetypes.DataElement;
import gov.cms.madie.models.cqm.datacriteria.basetypes.TestCaseJson;
import gov.cms.madie.models.measure.TestCase;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QdmTestCaseShiftDatesService {

  private final TestCaseService testCaseService;

  private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private static final String SEPARATOR = " |\n";

  @Autowired
  public QdmTestCaseShiftDatesService(TestCaseService testCaseService) {
    this.testCaseService = testCaseService;
  }

  public TestCase shiftTestCaseDates(
      String measureId, String testCaseId, int shifted, String username, String accessToken) {

    TestCase testCase = null;
    List<TestCase> testCases = testCaseService.findTestCasesByMeasureId(measureId);
    if (CollectionUtils.isEmpty(testCases)) {
      throw new ResourceNotFoundException("TestCase", measureId);
    } else {
      Optional<TestCase> existingOpt =
          testCases.stream().filter(tc -> tc.getId().equals(testCaseId)).findFirst();
      if (existingOpt.isEmpty()) {
        throw new ResourceNotFoundException("TestCase", measureId);
      }
      testCase = existingOpt.get();
    }

    shiftDatesForTestCase(testCase, shifted);
    testCaseService.updateTestCase(testCase, measureId, username, accessToken);

    return testCase;
  }

  protected TestCase shiftDatesForTestCase(TestCase testCase, int shifted) {
    try {
      TestCaseJson testCaseJson = mapper.readValue(testCase.getJson(), TestCaseJson.class);
      testCaseJson.setBirthDatetime(testCaseJson.shiftDateByYear(shifted));
      List<DataElement> elements = testCaseJson.getDataElements();
      if (CollectionUtils.isNotEmpty(elements)) {
        for (DataElement element : elements) {
          shiftDates(element, shifted);
        }
      }
      String newJson = mapper.writeValueAsString(testCaseJson);
      testCase.setJson(newJson);
    } catch (JsonProcessingException e) {
      log.error(
          "An issue occurred while shifting the test case dates for the test case id: "
              + testCase.getId()
              + " JsonProcessingException -> "
              + e.getMessage());
      throw new CqmConversionException(testCase.getTitle() + "/" + testCase.getId() + SEPARATOR);
    } catch (Exception e) {
      log.error(
          "An issue occurred while shifting the test case dates for the test case id: "
              + testCase.getId()
              + " Exception -> "
              + e.getMessage());
      throw new CqmConversionException(testCase.getTitle() + "/" + testCase.getId() + SEPARATOR);
    }
    return testCase;
  }

  void shiftDates(DataElement dataElement, int shifted) {
    if (dataElement instanceof AdverseEvent) {
      AdverseEvent adverseEvent = (AdverseEvent) dataElement;
      adverseEvent.shiftDates(shifted);
    } else if (dataElement instanceof AllergyIntolerance) {
      AllergyIntolerance allergyIntolerance = (AllergyIntolerance) dataElement;
      allergyIntolerance.shiftDates(shifted);
    } else if (dataElement instanceof AssessmentOrder) {
      AssessmentOrder assessmentOrder = (AssessmentOrder) dataElement;
      assessmentOrder.shiftDates(shifted);
    } else if (dataElement instanceof AssessmentPerformed) {
      AssessmentPerformed assessmentPerformed = (AssessmentPerformed) dataElement;
      assessmentPerformed.shiftDates(shifted);
    } else if (dataElement instanceof AssessmentRecommended) {
      AssessmentRecommended assessmentRecommended = (AssessmentRecommended) dataElement;
      assessmentRecommended.shiftDates(shifted);
    } else if (dataElement instanceof CareGoal) {
      CareGoal careGoal = (CareGoal) dataElement;
      careGoal.shiftDates(shifted);
    } else if (dataElement instanceof CommunicationPerformed) {
      CommunicationPerformed communicationPerformed = (CommunicationPerformed) dataElement;
      communicationPerformed.shiftDates(shifted);
    } else if (dataElement instanceof DeviceOrder) {
      DeviceOrder deviceOrder = (DeviceOrder) dataElement;
      deviceOrder.shiftDates(shifted);
    } else if (dataElement instanceof DeviceRecommended) {
      DeviceRecommended deviceRecommended = (DeviceRecommended) dataElement;
      deviceRecommended.shiftDates(shifted);
    } else if (dataElement instanceof Diagnosis) {
      Diagnosis diagnosis = (Diagnosis) dataElement;
      diagnosis.shiftDates(shifted);
    } else if (dataElement instanceof DiagnosticStudyOrder) {
      DiagnosticStudyOrder diagnosticStudyOrder = (DiagnosticStudyOrder) dataElement;
      diagnosticStudyOrder.shiftDates(shifted);
    } else if (dataElement instanceof DiagnosticStudyPerformed) {
      DiagnosticStudyPerformed diagnosticStudyPerformed = (DiagnosticStudyPerformed) dataElement;
      diagnosticStudyPerformed.shiftDates(shifted);
    } else if (dataElement instanceof DiagnosticStudyRecommended) {
      DiagnosticStudyRecommended diagnosticStudyRecommended =
          (DiagnosticStudyRecommended) dataElement;
      diagnosticStudyRecommended.shiftDates(shifted);
    } else if (dataElement instanceof EncounterOrder) {
      EncounterOrder encounterOrder = (EncounterOrder) dataElement;
      encounterOrder.shiftDates(shifted);
    } else if (dataElement instanceof EncounterPerformed) {
      EncounterPerformed encounterPerformed = (EncounterPerformed) dataElement;
      encounterPerformed.shiftDates(shifted);
    } else if (dataElement instanceof EncounterRecommended) {
      EncounterRecommended encounterRecommended = (EncounterRecommended) dataElement;
      encounterRecommended.shiftDates(shifted);
    } else if (dataElement instanceof FamilyHistory) {
      FamilyHistory familyHistory = (FamilyHistory) dataElement;
      familyHistory.shiftDates(shifted);
    } else if (dataElement instanceof ImmunizationAdministered) {
      ImmunizationAdministered immunizationAdministered = (ImmunizationAdministered) dataElement;
      immunizationAdministered.shiftDates(shifted);
    } else if (dataElement instanceof ImmunizationOrder) {
      ImmunizationOrder immunizationOrder = (ImmunizationOrder) dataElement;
      immunizationOrder.shiftDates(shifted);
    } else if (dataElement instanceof InterventionOrder) {
      InterventionOrder interventionOrder = (InterventionOrder) dataElement;
      interventionOrder.shiftDates(shifted);
    } else if (dataElement instanceof InterventionPerformed) {
      InterventionPerformed interventionPerformed = (InterventionPerformed) dataElement;
      interventionPerformed.shiftDates(shifted);
    } else if (dataElement instanceof InterventionRecommended) {
      InterventionRecommended interventionRecommended = (InterventionRecommended) dataElement;
      interventionRecommended.shiftDates(shifted);
    } else if (dataElement instanceof LaboratoryTestOrder) {
      LaboratoryTestOrder laboratoryTestOrder = (LaboratoryTestOrder) dataElement;
      laboratoryTestOrder.shiftDates(shifted);
    } else if (dataElement instanceof LaboratoryTestPerformed) {
      LaboratoryTestPerformed laboratoryTestPerformed = (LaboratoryTestPerformed) dataElement;
      laboratoryTestPerformed.shiftDates(shifted);
    } else if (dataElement instanceof LaboratoryTestRecommended) {
      LaboratoryTestRecommended laboratoryTestRecommended = (LaboratoryTestRecommended) dataElement;
      laboratoryTestRecommended.shiftDates(shifted);
    } else if (dataElement instanceof MedicationActive) {
      MedicationActive medicationActive = (MedicationActive) dataElement;
      medicationActive.shiftDates(shifted);
    } else if (dataElement instanceof MedicationAdministered) {
      MedicationAdministered medicationAdministered = (MedicationAdministered) dataElement;
      medicationAdministered.shiftDates(shifted);
    } else if (dataElement instanceof MedicationDischarge) {
      MedicationDischarge medicationDischarge = (MedicationDischarge) dataElement;
      medicationDischarge.shiftDates(shifted);
    } else if (dataElement instanceof MedicationDispensed) {
      MedicationDispensed medicationDispensed = (MedicationDispensed) dataElement;
      medicationDispensed.shiftDates(shifted);
    } else if (dataElement instanceof MedicationOrder) {
      MedicationOrder medicationOrder = (MedicationOrder) dataElement;
      medicationOrder.shiftDates(shifted);
    } else if (dataElement instanceof Participation) {
      Participation participation = (Participation) dataElement;
      participation.shiftDates(shifted);
    } else if (dataElement instanceof PatientCareExperience) {
      PatientCareExperience patientCareExperience = (PatientCareExperience) dataElement;
      patientCareExperience.shiftDates(shifted);
    } else if (dataElement instanceof PatientCharacteristic) {
      PatientCharacteristic patientCharacteristic = (PatientCharacteristic) dataElement;
      patientCharacteristic.shiftDates(shifted);
    } else if (dataElement instanceof PatientCharacteristicBirthdate) {
      PatientCharacteristicBirthdate patientCharacteristicBirthdate =
          (PatientCharacteristicBirthdate) dataElement;
      patientCharacteristicBirthdate.shiftDates(shifted);
    } else if (dataElement instanceof PatientCharacteristicClinicalTrialParticipant) {
      PatientCharacteristicClinicalTrialParticipant patientCharacteristicClinicalTrialParticipant =
          (PatientCharacteristicClinicalTrialParticipant) dataElement;
      patientCharacteristicClinicalTrialParticipant.shiftDates(shifted);
    } else if (dataElement instanceof PatientCharacteristicExpired) {
      PatientCharacteristicExpired patientCharacteristicExpired =
          (PatientCharacteristicExpired) dataElement;
      patientCharacteristicExpired.shiftDates(shifted);
    } else if (dataElement instanceof PatientCharacteristicPayer) {
      PatientCharacteristicPayer patientCharacteristicPayer =
          (PatientCharacteristicPayer) dataElement;
      patientCharacteristicPayer.shiftDates(shifted);
    } else if (dataElement instanceof PhysicalExamOrder) {
      PhysicalExamOrder physicalExamOrder = (PhysicalExamOrder) dataElement;
      physicalExamOrder.shiftDates(shifted);
    } else if (dataElement instanceof PhysicalExamPerformed) {
      PhysicalExamPerformed physicalExamPerformed = (PhysicalExamPerformed) dataElement;
      physicalExamPerformed.shiftDates(shifted);
    } else if (dataElement instanceof PhysicalExamRecommended) {
      PhysicalExamRecommended physicalExamRecommended = (PhysicalExamRecommended) dataElement;
      physicalExamRecommended.shiftDates(shifted);
    } else if (dataElement instanceof ProcedurePerformed) {
      ProcedurePerformed procedurePerformed = (ProcedurePerformed) dataElement;
      procedurePerformed.shiftDates(shifted);
    } else if (dataElement instanceof ProcedureRecommended) {
      ProcedureRecommended procedureRecommended = (ProcedureRecommended) dataElement;
      procedureRecommended.shiftDates(shifted);
    } else if (dataElement instanceof ProviderCareExperience) {
      ProviderCareExperience providerCareExperience = (ProviderCareExperience) dataElement;
      providerCareExperience.shiftDates(shifted);
    } else if (dataElement instanceof ProcedureOrder) {
      ProcedureOrder procedureOrder = (ProcedureOrder) dataElement;
      procedureOrder.shiftDates(shifted);
    } else if (dataElement instanceof SubstanceAdministered) {
      SubstanceAdministered substanceAdministered = (SubstanceAdministered) dataElement;
      substanceAdministered.shiftDates(shifted);
    } else if (dataElement instanceof SubstanceOrder) {
      SubstanceOrder substanceOrder = (SubstanceOrder) dataElement;
      substanceOrder.shiftDates(shifted);
    } else if (dataElement instanceof SubstanceRecommended) {
      SubstanceRecommended substanceRecommended = (SubstanceRecommended) dataElement;
      substanceRecommended.shiftDates(shifted);
    } else if (dataElement instanceof Symptom) {
      Symptom symptom = (Symptom) dataElement;
      symptom.shiftDates(shifted);
    } else if (dataElement instanceof PatientCharacteristicEthnicity
        || dataElement instanceof PatientCharacteristicSex
        || dataElement instanceof PatientCharacteristicRace
        || dataElement instanceof RelatedPerson) {
      // no dates to shift
    } else {
      log.error("Unsupported data type: " + dataElement.toString());
      throw new CqmConversionException(
          "Unsupported data type: " + dataElement.toString() + SEPARATOR);
    }
  }

  public List<TestCase> shiftAllTestCaseDates(
      String measureId, int shifted, String username, String accessToken) {
    List<TestCase> testCases = testCaseService.findTestCasesByMeasureId(measureId);
    if (CollectionUtils.isEmpty(testCases)) {
      throw new ResourceNotFoundException("TestCases", measureId);
    }
    StringBuilder testCaseFailures = new StringBuilder();

    List<TestCase> allTestCases = new ArrayList<>();
    for (TestCase testCase : testCases) {
      try {
        TestCase shiftedTC = shiftDatesForTestCase(testCase, shifted);
        allTestCases.add(shiftedTC);
        testCaseService.updateTestCase(shiftedTC, measureId, username, accessToken);
      } catch (CqmConversionException ex) {
        testCaseFailures.append(ex.getMessage());
        allTestCases.add(testCase);
      }
    }
    if (StringUtils.isNotBlank(testCaseFailures.toString())) {
      String errMsg =
          "The following test cases have issues shifting dates:\n" + testCaseFailures.toString();
      throw new CqmConversionException(StringUtils.removeEndIgnoreCase(errMsg, SEPARATOR));
    }
    return allTestCases;
  }
}
