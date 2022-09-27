package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.repositories.OrganizationRepository;
import gov.cms.madie.models.common.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

  private static final String ORGANIZATIONS_TEST_API_KEY = "test-api-key";
  @Mock private OrganizationRepository organizationRepository;

  @InjectMocks private OrganizationController organizationController;

  MockHttpServletRequest request;

  @BeforeEach
  public void setUp() {
    request = new MockHttpServletRequest();
  }

  @Test
  void getAllOrganizations() {
    List<Organization> organizationList = new ArrayList<>();
    organizationList.add(Organization.builder().name("org1").oid("1.2.3.4").build());
    organizationList.add(Organization.builder().name("org2").oid("1.2.3.5").build());
    organizationList.add(Organization.builder().name("org3").oid("1.2.3.6").build());

    when(organizationRepository.findAll()).thenReturn(organizationList);
    var result = organizationController.getAllOrganizations();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(3, result.getBody().size());
  }

  @Test
  void noOrganizationsAvailableInDb() {
    when(organizationRepository.findAll()).thenReturn(new ArrayList<>());
    assertThrows(RuntimeException.class, () -> organizationController.getAllOrganizations());
  }

  @Test
  void addOrganizations() {
    List<Organization> organizationList = new ArrayList<>();
    organizationList.add(Organization.builder().name("org1").oid("1.2.3.4").build());
    organizationList.add(Organization.builder().name("org2").oid("1.2.3.5").build());
    organizationList.add(Organization.builder().name("org3").oid("1.2.3.6").build());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Organization>> persistedOrganizationsArgCaptor =
        ArgumentCaptor.forClass(List.class);
    doReturn(organizationList).when(organizationRepository).saveAll(any());
    var result =
        organizationController.addOrganizations(
            request, organizationList, ORGANIZATIONS_TEST_API_KEY);
    verify(organizationRepository, times(1)).saveAll(persistedOrganizationsArgCaptor.capture());
    List<Organization> persistedOrganizations = result.getBody();
    assertNotNull(persistedOrganizations);

    assertEquals(organizationList.get(0).getName(), persistedOrganizations.get(0).getName());
    assertEquals(organizationList.get(0).getId(), persistedOrganizations.get(0).getId());

    assertEquals(HttpStatus.CREATED, result.getStatusCode());
    assertEquals(3, result.getBody().size());
  }

  @Test
  void addOrganizationsThrowsDuplicateKeyException() {
    doThrow(new DuplicateKeyException("DuplicateKeyException Message", "Duplicate oid found"))
        .when(organizationRepository)
        .saveAll(any());

    assertThrows(
        DuplicateKeyException.class,
        () ->
            organizationController.addOrganizations(
                request, List.of(), ORGANIZATIONS_TEST_API_KEY));
  }
}
