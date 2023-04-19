package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.EndorsementRepository;
import gov.cms.madie.models.common.EndorserOrganization;
import gov.cms.madie.models.measure.Endorsement;
import java.util.List;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EndorsementService {

  private EndorsementRepository endorsementRepository;

  public void validateEndorsements(List<Endorsement> endorsements) {
    List<String> validEndorsements =
        endorsementRepository.findAll().stream()
            .map(EndorserOrganization::getEndorserOrganization)
            .toList();
    for (Endorsement endorsement : endorsements) {
      if (!validEndorsements.contains(endorsement.getEndorser())) {
        throw new ValidationException("Endorser is not valid");
      }
    }
  }
}
