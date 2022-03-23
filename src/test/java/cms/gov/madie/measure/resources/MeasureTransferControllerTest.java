package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.MeasureMetaData;
import cms.gov.madie.measure.models.MeasurePopulation;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.MeasureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeasureTransferControllerTest {

  private static final String LAMBDA_TEST_API_KEY = "TOUCH-DOWN";

  private Measure measure;

  @Mock
  private MeasureService measureService;
  @Mock private MeasureRepository repository;

  @InjectMocks
  private MeasureTransferController controller;

  MockHttpServletRequest request;

  @BeforeEach
  public void setUp() {
    request = new MockHttpServletRequest();
    MeasureMetaData measureMetaData = new MeasureMetaData();
    List<Group> groups = List.of(
      new Group("id-abc", "Cohort",
        Map.of(MeasurePopulation.INITIAL_POPULATION, "Initial Population")));

    measureMetaData.setSteward("SB");
    measureMetaData.setCopyright("Copyright@SB");

    measure = Measure.builder()
      .measureSetId("abc-pqr-xyz")
      .version("0.000")
      .measureName("MedicationDispenseTest")
      .cqlLibraryName("MedicationDispenseTest")
      .measureScoring("Cohort")
      .model("QI-Core")
      .measureMetaData(measureMetaData)
      .groups(groups)
      .cql("library MedicationDispenseTest version '0.0.001' using FHIR version '4.0.1'")
      .build();
  }

  @Test
  public void createMeasureSuccessTest() {
    ArgumentCaptor<Measure> persistedMeasureArgCaptor = ArgumentCaptor.forClass(Measure.class);
    doNothing().when(measureService).checkDuplicateCqlLibraryName(any(String.class));
    doReturn(measure).when(repository).save(any(Measure.class));

    ResponseEntity<Measure>  response = controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY);

    verify(repository, times(1)).save(persistedMeasureArgCaptor.capture());
    Measure persistedMeasure = response.getBody();
    assertNotNull(persistedMeasure);

    assertEquals(measure.getMeasureSetId(), persistedMeasure.getMeasureSetId());
    assertEquals(measure.getMeasureName(), persistedMeasure.getMeasureName());
    assertEquals(measure.getCqlLibraryName(), persistedMeasure.getCqlLibraryName());
    assertEquals(measure.getMeasureScoring(), persistedMeasure.getMeasureScoring());
    assertEquals(measure.getCql(), persistedMeasure.getCql());
    assertEquals(measure.getGroups().size(), persistedMeasure.getGroups().size());
  }

  @Test
  public void createMeasureDuplicateCqlLibraryTest() {
    doThrow(new DuplicateKeyException("cqlLibraryName", "CQL library already exists."))
      .when(measureService).checkDuplicateCqlLibraryName(any(String.class));

    assertThrows(
      DuplicateKeyException.class, () -> controller.createMeasure(request, measure, LAMBDA_TEST_API_KEY));
  }
}
