package cms.gov.madie.measure.services;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristicExpired;
import gov.cms.madie.models.cqm.datacriteria.PatientCharacteristicPayer;
import gov.cms.madie.models.cqm.datacriteria.PhysicalExamOrder;
import gov.cms.madie.models.cqm.datacriteria.PhysicalExamPerformed;
import gov.cms.madie.models.cqm.datacriteria.PhysicalExamRecommended;
import gov.cms.madie.models.cqm.datacriteria.ProcedureOrder;
import gov.cms.madie.models.cqm.datacriteria.ProcedurePerformed;
import gov.cms.madie.models.cqm.datacriteria.ProcedureRecommended;
import gov.cms.madie.models.cqm.datacriteria.ProviderCareExperience;
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
public class TestCaseShiftDatesService {

  private final TestCaseService testCaseService;

  @Autowired
  public TestCaseShiftDatesService(TestCaseService testCaseService) {
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

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    try {
      TestCaseJson testCaseJson = mapper.readValue(testCase.getJson(), TestCaseJson.class);
      if (testCaseJson != null) {
        testCaseJson.setBirthDatetime(testCaseJson.shiftDateByYear(shifted));
        List<DataElement> elements = testCaseJson.getDataElements();
        if (CollectionUtils.isNotEmpty(elements)) {
          for (DataElement element : elements) {
            shiftDates(element, shifted);
          }
        }
        String newJson = mapper.writeValueAsString(testCaseJson);
        testCase.setJson(newJson);
        testCaseService.updateTestCase(testCase, measureId, username, accessToken);
      }
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new CqmConversionException("JsonProcessingException for test case id : " + testCaseId);
    }

    return testCase;
  }

  void shiftDates(DataElement dataElement, int shifted) {
    String type = dataElement.get_type() != null ? dataElement.get_type() : "";

    switch (type) {
      case "QDM::AdverseEvent":
        AdverseEvent adverseEvent = (AdverseEvent) dataElement;
        adverseEvent.shiftDates(shifted);
        break;
      case "QDM::AllergyIntolerance":
        AllergyIntolerance allergyIntolerance = (AllergyIntolerance) dataElement;
        allergyIntolerance.shiftDates(shifted);
        break;
      case "QDM::AssessmentOrder":
        AssessmentOrder assessmentOrder = (AssessmentOrder) dataElement;
        assessmentOrder.shiftDates(shifted);
        break;
      case "QDM::AssessmentPerformed":
        AssessmentPerformed assessmentPerformed = (AssessmentPerformed) dataElement;
        assessmentPerformed.shiftDates(shifted);
        break;
      case "QDM::AssessmentRecommended":
        AssessmentRecommended assessmentRecommended = (AssessmentRecommended) dataElement;
        assessmentRecommended.shiftDates(shifted);
        break;
      case "QDM::CareGoal":
        CareGoal careGoal = (CareGoal) dataElement;
        careGoal.shiftDates(shifted);
        break;
      case "QDM::CommunicationPerformed":
        CommunicationPerformed communicationPerformed = (CommunicationPerformed) dataElement;
        communicationPerformed.shiftDates(shifted);
        break;
      case "QDM::DeviceOrder":
        DeviceOrder deviceOrder = (DeviceOrder) dataElement;
        deviceOrder.shiftDates(shifted);
        break;
      case "QDM::DeviceRecommended":
        DeviceRecommended deviceRecommended = (DeviceRecommended) dataElement;
        deviceRecommended.shiftDates(shifted);
        break;
      case "QDM::Diagnosis":
        Diagnosis diagnosis = (Diagnosis) dataElement;
        diagnosis.shiftDates(shifted);
        break;
      case "QDM::DiagnosticStudyOrder":
        DiagnosticStudyOrder diagnosticStudyOrder = (DiagnosticStudyOrder) dataElement;
        diagnosticStudyOrder.shiftDates(shifted);
        break;
      case "QDM::DiagnosticStudyPerformed":
        DiagnosticStudyPerformed diagnosticStudyPerformed = (DiagnosticStudyPerformed) dataElement;
        diagnosticStudyPerformed.shiftDates(shifted);
        break;
      case "QDM::DiagnosticStudyRecommended":
        DiagnosticStudyRecommended diagnosticStudyRecommended =
            (DiagnosticStudyRecommended) dataElement;
        diagnosticStudyRecommended.shiftDates(shifted);
        break;
      case "QDM::EncounterOrder":
        EncounterOrder encounterOrder = (EncounterOrder) dataElement;
        encounterOrder.shiftDates(shifted);
        break;
      case "QDM::EncounterPerformed":
        EncounterPerformed encounterPerformed = (EncounterPerformed) dataElement;
        encounterPerformed.shiftDates(shifted);
        break;
      case "QDM::EncounterRecommended":
        EncounterRecommended encounterRecommended = (EncounterRecommended) dataElement;
        encounterRecommended.shiftDates(shifted);
        break;
      case "QDM::FamilyHistory":
        FamilyHistory familyHistory = (FamilyHistory) dataElement;
        familyHistory.shiftDates(shifted);
        break;
      case "QDM::ImmunizationAdministered":
        ImmunizationAdministered immunizationAdministered = (ImmunizationAdministered) dataElement;
        immunizationAdministered.shiftDates(shifted);
        break;
      case "QDM::ImmunizationOrder":
        ImmunizationOrder immunizationOrder = (ImmunizationOrder) dataElement;
        immunizationOrder.shiftDates(shifted);
        break;
      case "QDM::InterventionOrder":
        InterventionOrder interventionOrder = (InterventionOrder) dataElement;
        interventionOrder.shiftDates(shifted);
        break;
      case "QDM::InterventionPerformed":
        InterventionPerformed interventionPerformed = (InterventionPerformed) dataElement;
        interventionPerformed.shiftDates(shifted);
        break;
      case "QDM::InterventionRecommended":
        InterventionRecommended interventionRecommended = (InterventionRecommended) dataElement;
        interventionRecommended.shiftDates(shifted);
        break;
      case "QDM::LaboratoryTestOrder":
        LaboratoryTestOrder laboratoryTestOrder = (LaboratoryTestOrder) dataElement;
        laboratoryTestOrder.shiftDates(shifted);
        break;
      case "QDM::LaboratoryTestPerformed":
        LaboratoryTestPerformed laboratoryTestPerformed = (LaboratoryTestPerformed) dataElement;
        laboratoryTestPerformed.shiftDates(shifted);
        break;
      case "QDM::LaboratoryTestRecommended":
        LaboratoryTestRecommended laboratoryTestRecommended =
            (LaboratoryTestRecommended) dataElement;
        laboratoryTestRecommended.shiftDates(shifted);
        break;
      case "QDM::MedicationActive":
        MedicationActive medicationActive = (MedicationActive) dataElement;
        medicationActive.shiftDates(shifted);
        break;
      case "QDM::MedicationAdministered":
        MedicationAdministered medicationAdministered = (MedicationAdministered) dataElement;
        medicationAdministered.shiftDates(shifted);
        break;
      case "QDM::MedicationDischarge":
        MedicationDischarge medicationDischarge = (MedicationDischarge) dataElement;
        medicationDischarge.shiftDates(shifted);
        break;
      case "QDM::MedicationDispensed":
        MedicationDispensed medicationDispensed = (MedicationDispensed) dataElement;
        medicationDispensed.shiftDates(shifted);
        break;
      case "QDM::MedicationOrder":
        MedicationOrder medicationOrder = (MedicationOrder) dataElement;
        medicationOrder.shiftDates(shifted);
        break;
      case "QDM::Participation":
        Participation participation = (Participation) dataElement;
        participation.shiftDates(shifted);
        break;
      case "QDM::PatientCareExperience":
        PatientCareExperience patientCareExperience = (PatientCareExperience) dataElement;
        patientCareExperience.shiftDates(shifted);
        break;
      case "QDM::PatientCharacteristic":
        PatientCharacteristic patientCharacteristic = (PatientCharacteristic) dataElement;
        patientCharacteristic.shiftDates(shifted);
        break;
      case "QDM::PatientCharacteristicBirthdate":
        PatientCharacteristicBirthdate patientCharacteristicBirthdate =
            (PatientCharacteristicBirthdate) dataElement;
        patientCharacteristicBirthdate.shiftDates(shifted);
        break;
      case "QDM::PatientCharacteristicClinicalTrialParticipant":
        PatientCharacteristicClinicalTrialParticipant
            patientCharacteristicClinicalTrialParticipant =
                (PatientCharacteristicClinicalTrialParticipant) dataElement;
        patientCharacteristicClinicalTrialParticipant.shiftDates(shifted);
        break;
      case "QDM::PatientCharacteristicExpired":
        PatientCharacteristicExpired patientCharacteristicExpired =
            (PatientCharacteristicExpired) dataElement;
        patientCharacteristicExpired.shiftDates(shifted);
        break;
      case "QDM::PatientCharacteristicPayer":
        PatientCharacteristicPayer patientCharacteristicPayer =
            (PatientCharacteristicPayer) dataElement;
        patientCharacteristicPayer.shiftDates(shifted);
        break;
      case "QDM::PhysicalExamOrder":
        PhysicalExamOrder physicalExamOrder = (PhysicalExamOrder) dataElement;
        physicalExamOrder.shiftDates(shifted);
        break;
      case "QDM::PhysicalExamPerformed":
        PhysicalExamPerformed physicalExamPerformed = (PhysicalExamPerformed) dataElement;
        physicalExamPerformed.shiftDates(shifted);
        break;
      case "QDM::PhysicalExamRecommended":
        PhysicalExamRecommended physicalExamRecommended = (PhysicalExamRecommended) dataElement;
        physicalExamRecommended.shiftDates(shifted);
        break;
      case "QDM::ProcedurePerformed":
        ProcedurePerformed procedurePerformed = (ProcedurePerformed) dataElement;
        procedurePerformed.shiftDates(shifted);
        break;
      case "QDM::ProcedureRecommended":
        ProcedureRecommended procedureRecommended = (ProcedureRecommended) dataElement;
        procedureRecommended.shiftDates(shifted);
        break;
      case "QDM::ProviderCareExperience":
        ProviderCareExperience providerCareExperience = (ProviderCareExperience) dataElement;
        providerCareExperience.shiftDates(shifted);
        break;
      case "QDM::ProcedureOrder":
        ProcedureOrder procedureOrder = (ProcedureOrder) dataElement;
        procedureOrder.shiftDates(shifted);
        break;
      case "QDM::SubstanceAdministered":
        SubstanceAdministered substanceAdministered = (SubstanceAdministered) dataElement;
        substanceAdministered.shiftDates(shifted);
        break;
      case "QDM::SubstanceOrder":
        SubstanceOrder substanceOrder = (SubstanceOrder) dataElement;
        substanceOrder.shiftDates(shifted);
        break;
      case "QDM::SubstanceRecommended":
        SubstanceRecommended substanceRecommended = (SubstanceRecommended) dataElement;
        substanceRecommended.shiftDates(shifted);
        break;
      case "QDM::Symptom":
        Symptom symptom = (Symptom) dataElement;
        symptom.shiftDates(shifted);
        break;
      default:
        throw new CqmConversionException("Unsupported data type: " + type);
    }
  }
}
