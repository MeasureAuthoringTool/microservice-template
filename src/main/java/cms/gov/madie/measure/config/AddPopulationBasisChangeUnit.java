package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.PopulationBasisRepository;
import gov.cms.madie.models.common.PopulationBasis;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

import java.util.ArrayList;
import java.util.List;

@ChangeUnit(id = "add_population_basis_values_initializer", order = "1", author = "madie_dev")
public class AddPopulationBasisChangeUnit {

  @Execution
  public void addPopulationBasisValues(PopulationBasisRepository populationBasisRepository) {
    List<PopulationBasis> populationBasisList = new ArrayList<>();
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("boolean").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Condition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Encounter").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Procedure").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Account").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ActivityDefinition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("AdministrableProductDefinition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("AdverseEvent").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("AllergyIntolerance").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Appointment").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("AppointmentResponse").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("BiologicallyDerivedProduct").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("BodyStructure").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("CarePlan").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("CareTeam").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("ChargeItem").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ChargeItemDefinition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Citation").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Claim").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ClaimResponse").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ClinicalImpression").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ClinicalUseDefinition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Communication").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("CommunicationRequest").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Contract").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Coverage").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("CoverageEligibilityRequest").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("CoverageEligibilityResponse").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("DetectedIssue").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Device").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("DeviceDefinition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("DeviceMetric").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("DeviceRequest").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("DeviceUseStatement").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("DiagnosticReport").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Endpoint").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("EnrollmentRequest").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("EnrollmentResponse").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("EpisodeOfCare").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("EventDefinition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Evidence").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("EvidenceReport").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("EvidenceVariable").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ExplanationOfBenefit").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("FamilyMemberHistory").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Flag").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Goal").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Group").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("GuidanceResponse").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("HealthcareService").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("ImagingStudy").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Immunization").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ImmunizationEvaluation").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ImmunizationRecommendation").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Ingredient").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("InsurancePlan").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Invoice").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Library").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("List").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Location").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ManufacturedItemDefinition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Measure").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("MeasureReport").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Media").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Medication").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("MedicationAdministration").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("MedicationDispense").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("MedicationKnowledge").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("MedicationRequest").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("MedicationStatement").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("MedicinalProductDefinition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("MolecularSequence").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("NutritionOrder").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("NutritionProduct").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Observation").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ObservationDefinition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Organization").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("OrganizationAffiliation").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("PackagedProductDefinition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Patient").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("PaymentNotice").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("PaymentReconciliation").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Person").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("PlanDefinition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Practitioner").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("PractitionerRole").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Questionnaire").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("QuestionnaireResponse").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("RegulatedAuthorization").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("RelatedPerson").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("RequestGroup").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ResearchStudy").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ResearchSubject").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("RiskAssessment").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Schedule").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("ServiceRequest").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Slot").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Specimen").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("SpecimenDefinition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Substance").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("SubstanceDefinition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("SupplyDelivery").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("SupplyRequest").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Task").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("TestReport").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("TestScript").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("VerificationResult").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("VisionPrescription").build());
    populationBasisRepository.insert(populationBasisList);
  }

  @RollbackExecution
  public void rollbackExecution(PopulationBasisRepository populationBasisRepository) {
    populationBasisRepository.deleteAll();
  }
}
