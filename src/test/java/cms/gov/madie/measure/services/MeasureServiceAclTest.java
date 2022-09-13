package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Optional;
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

  @InjectMocks private MeasureService measureService;

  @Test
  public void testGrantAccess() {

    Measure measure = new Measure();
    Optional<Measure> persistedMeasure = Optional.of(measure);

    when(measureRepository.findById(eq("123"))).thenReturn(persistedMeasure);
    boolean result = measureService.grantAccess("123", "akinsgre", "gakins");

    assertTrue(result);
  }

  @Test
  public void testGrantAccessExistingAclSizeZero() {

    Measure measure = new Measure();
    measure.setAcls(new ArrayList<AclSpecification>() {});
    Optional<Measure> persistedMeasure = Optional.of(measure);

    when(measureRepository.findById(eq("123"))).thenReturn(persistedMeasure);
    boolean result = measureService.grantAccess("123", "akinsgre", "gakins");

    assertTrue(result);
  }

  @Test
  public void testGrantAccessExistingAclSizeGreaterThanZero() {

    Measure measure = new Measure();
    AclSpecification spec = new AclSpecification();
    spec.setUserId("akinsgre");
    spec.setRoles(
        new ArrayList<RoleEnum>() {
          {
            add(RoleEnum.SHARED_WITH);
          }
        });
    measure.setAcls(
        new ArrayList<AclSpecification>() {
          {
            add(spec);
          }
        });
    Optional<Measure> persistedMeasure = Optional.of(measure);

    when(measureRepository.findById(eq("123"))).thenReturn(persistedMeasure);
    boolean result = measureService.grantAccess("123", "akinsgre", "gakins");

    assertTrue(result);
  }

  @Test
  public void testGrantAccessNoMeasure() {

    Optional<Measure> persistedMeasure = Optional.empty();

    when(measureRepository.findById(eq("123"))).thenReturn(persistedMeasure);
    boolean result = measureService.grantAccess("123", "akinsgre", "gakins");

    assertFalse(result);
  }
}
