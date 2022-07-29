package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.repositories.PopulationBasisRepository;
import gov.cms.madie.models.common.PopulationBasis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilsControllerTest {

  @Mock
  private PopulationBasisRepository populationBasisRepository;

  @InjectMocks
  private UtilsController utilsController;

  @Test
  void getAllPopulationBasisValues() {
    List<PopulationBasis> populationBasisList = new ArrayList<>();
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Boolean").build());
    populationBasisList.add(
        PopulationBasis.builder().populationBasisValue("Condition (Problem)").build());
    populationBasisList.add(PopulationBasis.builder().populationBasisValue("Encounter").build());

    when(populationBasisRepository.findAll()).thenReturn(populationBasisList);
    var result = utilsController.getAllPopulationBasisValues();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(3, result.getBody().size());
  }

  @Test
  void noPopulationBasisValuesAvailableInDb() {
    when(populationBasisRepository.findAll()).thenReturn(new ArrayList<>());
    assertThrows(RuntimeException.class, () -> utilsController.getAllPopulationBasisValues());
  }
}