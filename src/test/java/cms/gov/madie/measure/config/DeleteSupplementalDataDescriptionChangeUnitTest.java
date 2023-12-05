package cms.gov.madie.measure.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Update;

import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.DefDescPair;
import gov.cms.madie.models.measure.Measure;

@ExtendWith(MockitoExtension.class)
public class DeleteSupplementalDataDescriptionChangeUnitTest {

  @Mock private MongoOperations mongoOperations;
  @Mock private MeasureRepository measureRepository;
  @Mock private BulkOperations bulkOperations;

  private Measure measure;
  private Query query = new Query();
  private Update update = new Update().unset("supplementalData.$[].description");
  private DeleteSupplementalDataDescriptionChangeUnit changeUnit =
      new DeleteSupplementalDataDescriptionChangeUnit();

  @BeforeEach
  public void setup() {
    DefDescPair supplementalData =
        DefDescPair.builder().definition("SDE definition").description("SDE description").build();
    measure =
        Measure.builder()
            .id("TestMeasure1")
            .measureName("Test Measure 1")
            .model(ModelType.QI_CORE.getValue())
            .supplementalData(List.of(supplementalData))
            .build();
  }

  @Test
  public void testDeleteSdeDescriptionFromDefDescPair() {
    when(measureRepository.findAll()).thenReturn(List.of(measure));

    when(mongoOperations.bulkOps(BulkOperations.BulkMode.UNORDERED, Measure.class))
        .thenReturn(bulkOperations);

    changeUnit.deleteSdeDescriptionFromDefDescPair(mongoOperations, measureRepository);

    verify(bulkOperations, new Times(1)).updateOne(query, update);
    verify(bulkOperations, new Times(1)).execute();
  }

  @Test
  public void testDeleteSdeDescriptionNoExecution() {
    when(measureRepository.findAll()).thenReturn(List.of());

    changeUnit.deleteSdeDescriptionFromDefDescPair(mongoOperations, measureRepository);

    verify(bulkOperations, new Times(0)).updateOne(query, update);
    verify(bulkOperations, new Times(0)).execute();
  }

  @Test
  void testRollback() {
    changeUnit.rollbackExecution();
    verifyNoInteractions(measureRepository);
  }
}
