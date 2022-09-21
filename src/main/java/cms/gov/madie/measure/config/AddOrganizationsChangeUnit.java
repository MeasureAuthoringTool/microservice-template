package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.OrganizationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.common.Organization;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@ChangeUnit(id = "add_organizations_initializer", order = "1", author = "madie_dev")
public class AddOrganizationsChangeUnit {

  private final ObjectMapper objectMapper;

  public AddOrganizationsChangeUnit(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Execution
  public void addOrganizations(OrganizationRepository organizationRepository) throws IOException {
    File organizationsData = ResourceUtils.getFile("classpath:data/organizations.json");

    List<Organization> organizationList =
        Arrays.asList(objectMapper.readValue(organizationsData, Organization[].class));
    organizationRepository.insert(organizationList);
  }

  @RollbackExecution
  public void rollbackExecution(OrganizationRepository organizationRepository) {
    organizationRepository.deleteAll();
  }
}
