package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.Times;
import java.io.IOException;
import java.util.List;
import static org.mockito.Mockito.*;


public class AddMeasureSetChangeUnitTest {

  MeasureSet measureSet;
  Measure measure;

  @Test
  @SuppressWarnings("unchecked")
  void addMeasureSetValues() throws IOException {

    measure =
        Measure.builder().id(null).createdBy("testCreatedBy").measureSetId("abc-pqr-xyz").build();
    measureSet = MeasureSet.builder().measureSetId("abc-pqr-xyz").owner("testCreatedBy").build();

    MeasureSetRepository measureSetRepository = mock(MeasureSetRepository.class);
    MeasureRepository measureRepository = mock(MeasureRepository.class);
    when(measureSetRepository.existsByMeasureSetId("abc-pqr-xyz")).thenReturn(false);
    when(measureRepository.findDistinctByMeasureSetField()).thenReturn((List.of(measure)));
    new AddMeasureSetChangeUnit(new ObjectMapper())
        .addMeasureSetValues(measureSetRepository, measureRepository);
    verify(measureSetRepository, new Times(1)).save(measureSet);
  }

  @Test
  void rollbackExecution() {
    MeasureSetRepository measureSetRepository = mock(MeasureSetRepository.class);
    new AddMeasureSetChangeUnit(new ObjectMapper()).rollbackExecution(measureSetRepository);
    verify(measureSetRepository, new Times(1)).deleteAll();
  }
}
