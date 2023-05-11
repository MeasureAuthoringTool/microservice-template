package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.MeasureSetRepository;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeasureSetServiceTest {

  @InjectMocks private MeasureSetService measureSetService;
  @Mock MeasureSetRepository measureSetRepository;
  @Mock private ActionLogService actionLogService;
  MeasureSet measureSet;

  @BeforeEach
  public void setUp() {
    measureSet = MeasureSet.builder().measureSetId("msid-2").owner("user-1").build();
  }

  @Test
  public void testCreateMeasureSet() {
    when(measureSetRepository.existsByMeasureSetId("msid-2")).thenReturn(false);
    when(measureSetRepository.save(measureSet)).thenReturn(measureSet);
    measureSetService.createMeasureSet("user-1", "msid-xyz-p12r-12ert", "msid-2");

    verify(measureSetRepository, times(1)).existsByMeasureSetId("msid-2");
    verify(measureSetRepository, times(1)).save(measureSet);
    verify(actionLogService, times(1))
        .logAction(measureSet.getId(), Measure.class, ActionType.CREATED, "user-1");
  }

  @Test
  public void testNotCreateMeasureSetWhenMeasureSetIdExists() {
    when(measureSetRepository.existsByMeasureSetId("msid-2")).thenReturn(true);
    measureSetService.createMeasureSet("user-1", "msid-xyz-p12r-12ert", "msid-2");
    verify(measureSetRepository, times(1)).existsByMeasureSetId("msid-2");
    verify(measureSetRepository, times(0)).save(measureSet);
  }
}
