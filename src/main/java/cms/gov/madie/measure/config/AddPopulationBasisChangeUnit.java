package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.PopulationBasisRepository;
import gov.cms.madie.models.common.PopulationBasis;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

@ChangeUnit(id = "add_population_basis_values-initializer", order = "1", author = "madie_dev")
public class AddPopulationBasisChangeUnit {

  private final MongoTemplate mongoTemplate;
  private final PopulationBasisRepository populationBasisRepository;

  public AddPopulationBasisChangeUnit(
      MongoTemplate mongoTemplate, PopulationBasisRepository populationBasisRepository) {
    this.mongoTemplate = mongoTemplate;
    this.populationBasisRepository = populationBasisRepository;
  }

  @Execution
  public void addPopulationBasisValues() {
    List<PopulationBasis> populationBasisList = new ArrayList<>();
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Boolean").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Condition (Problem)").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Encounter").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Procedure").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Account").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Activity Definition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Administrable Product Definition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Adverse Event").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Allergy Intolerance").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Appointment").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Appointment Response").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Biologically Derived Product").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Body Structure").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Care Plan").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Care Team").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Charge Item").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Charge Item Definition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Citation").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Claim").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Claim Response").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Clinical Impression").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Clinical Use Definition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Communication").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Communication Request").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Contract").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Coverage").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Coverage Eligibility Request").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Coverage Eligibility Response").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Detected Issue").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Device").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Device Definition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Device Metric").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Device Request").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Device Use Statement").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Diagnostic Report").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Endpoint").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Enrollment Request").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Enrollment Response").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Episode Of Care").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Event Definition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Evidence").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Evidence Report").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Evidence Variable").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Explanation Of Benefit").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Family Member History").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Flag").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Goal").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Group").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Guidance Response").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Healthcare Service").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Imaging Study").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Immunization").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Immunization Evaluation").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Immunization Recommendation").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Ingredient").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Insurance Plan").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Invoice").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Library").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("List").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Location").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Manufactured Item Definition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Measure").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Measure Report").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Media").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Medication").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Medication Administration").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Medication Dispense").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Medication Knowledge").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Medication Request").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Medication Statement").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Medicinal Product Definition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Molecular Sequence").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Nutrition Order").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Nutrition Product").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Observation").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Observation Definition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Organization").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Organization Affiliation").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Packaged Product Definition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Patient").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Payment Notice").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Payment Reconciliation").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Person").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Plan Definition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Practitioner").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Practitioner Role").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Questionnaire").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Questionnaire Response").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Regulated Authorization").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Related Person").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Request Group").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Research Study").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Research Subject").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Risk Assessment").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Schedule").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Service Request").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Slot").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Specimen").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Specimen Definition").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Substance").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Substance Definition").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Supply Delivery").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Supply Request").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Task").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Test Report").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Test Script").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Verification Result").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Vision Prescription").build());
    populationBasisRepository.insert(populationBasisList);
  }

  @RollbackExecution
  public void rollback() {
    mongoTemplate.findAllAndRemove(new Query(), PopulationBasis.class);
  }
}
