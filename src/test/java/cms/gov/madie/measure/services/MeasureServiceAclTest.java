package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Optional;

import gov.cms.madie.models.measure.MeasureSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.Measure;

@ExtendWith(MockitoExtension.class)
public class MeasureServiceAclTest {

  @Mock private MeasureRepository measureRepository;
  @Mock private MeasureSetService measureSetService;

  @InjectMocks private MeasureService measureService;

  @Test
  public void testGrantAccess() {
    Measure measure = Measure.builder().id("123").measureSetId("1-2-3").build();
    Optional<Measure> persistedMeasure = Optional.of(measure);
    when(measureRepository.findById(anyString())).thenReturn(persistedMeasure);
    when(measureSetService.updateMeasureSetAcls(anyString(), any(AclSpecification.class)))
        .thenReturn(new MeasureSet());

    boolean result = measureService.grantAccess(measure.getId(), "akinsgre");
    assertTrue(result);
  }

  @Test
  public void testGrantAccessNoMeasure() {
    Optional<Measure> persistedMeasure = Optional.empty();
    when(measureRepository.findById(eq("123"))).thenReturn(persistedMeasure);
    boolean result = measureService.grantAccess("123", "akinsgre");

    assertFalse(result);
  }
}
