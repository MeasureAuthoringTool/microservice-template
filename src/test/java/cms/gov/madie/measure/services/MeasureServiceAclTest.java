package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.AggregateMethodType;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;

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
