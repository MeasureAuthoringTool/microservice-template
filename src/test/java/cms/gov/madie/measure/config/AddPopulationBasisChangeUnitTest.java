package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.PopulationBasisRepository;
import gov.cms.madie.models.common.PopulationBasis;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.Times;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AddPopulationBasisChangeUnitTest {

  @Test
  @SuppressWarnings("unchecked")
  void addPopulationBasisValues() {
    PopulationBasisRepository populationBasisRepository = mock(PopulationBasisRepository.class);
    new AddPopulationBasisChangeUnit().addPopulationBasisValues(populationBasisRepository);

    ArgumentCaptor<List<PopulationBasis>> populationBasisList = ArgumentCaptor.forClass(List.class);
    verify(populationBasisRepository, new Times(1)).insert(populationBasisList.capture());
    assertEquals(108, populationBasisList.getValue().size());
  }

  @Test
  void rollbackExecution() {
    PopulationBasisRepository populationBasisRepository = mock(PopulationBasisRepository.class);
    new AddPopulationBasisChangeUnit().rollbackExecution(populationBasisRepository);
    verify(populationBasisRepository, new Times(1)).deleteAll();
  }
}
