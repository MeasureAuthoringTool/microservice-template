package cms.gov.madie.measure.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import cms.gov.madie.measure.repositories.EndorsementRepository;
import gov.cms.madie.models.common.EndorserOrganization;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EndorsementControllerTest {

  @Mock private EndorsementRepository endorsementRepository;

  @InjectMocks private EndorsementController endorsementController;

  @Test
  void getAllEndorsementsTest() {
    List<EndorserOrganization> endorserOrganizationList = new ArrayList<>();
    endorserOrganizationList.add(EndorserOrganization.builder().endorserOrganization("-").build());
    endorserOrganizationList.add(
        EndorserOrganization.builder().endorserOrganization("NQF").build());

    when(endorsementRepository.findAll()).thenReturn(endorserOrganizationList);
    var result = endorsementController.getAllEndorsements();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(2, result.getBody().size());
  }

  @Test
  void noEndorserOrganizationsAvailableInDb() {
    when(endorsementRepository.findAll()).thenReturn(new ArrayList<>());
    assertThrows(RuntimeException.class, () -> endorsementController.getAllEndorsements());
  }
}
