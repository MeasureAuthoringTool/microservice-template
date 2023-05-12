package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.Times;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class MoveMeasureAclsToMeasureSetChangeUnitTest {
  private MeasureSetRepository measureSetRepository;
  private MeasureRepository measureRepository;
  private Measure measure1;
  private Measure measure2;
  private Measure measure3;
  private Measure measure4;
  private Measure measure5;

  @BeforeEach
  void setup() {
    AclSpecification spec1 = new AclSpecification();
    spec1.setUserId("john");
    AclSpecification spec2 = new AclSpecification();
    spec2.setUserId("jane");
    List<AclSpecification> aclSpecs1 = new ArrayList<>() {
      {
        add(spec1);
      }
    };

    List<AclSpecification> aclSpecs2 = new ArrayList<>() {
      {
        add(spec2);
      }
    };
    measure1 = Measure.builder().id("1").measureSetId("1").build(); // Set 1, no ACL
    measure2 =
        Measure.builder().id("2").measureSetId("1").acls(aclSpecs1).build(); // Set: 1, ACL: spec1
    measure3 =
        Measure.builder().id("3").measureSetId("1").acls(aclSpecs1).build(); // Set: 1, ACL: spec1
    measure4 =
        Measure.builder().id("4").measureSetId("1").acls(aclSpecs2).build(); // Set: 1, ACL: spec2
    measure5 =
        Measure.builder().id("5").measureSetId("2").acls(aclSpecs1).build(); // Set: 2, ACL: spec1

    measureSetRepository = mock(MeasureSetRepository.class);
    measureRepository = mock(MeasureRepository.class);
  }

  @Test
  void testMoveMeasureAClsToMeasureSet() {
    MeasureSet set1 = MeasureSet.builder().measureSetId("1").build();
    MeasureSet set2 = MeasureSet.builder().measureSetId("2").build();
    when(measureRepository.findAll())
      .thenReturn((List.of(measure1, measure2, measure3, measure4, measure5)));
    when(measureSetRepository.findByMeasureSetId(anyString()))
        .thenReturn(Optional.of(set1))
        .thenReturn(Optional.of(set1))
        .thenReturn(Optional.of(set1))
        .thenReturn(Optional.of(set2));

    new MoveMeasureAclsToMeasureSetChangeUnit()
        .moveMeasureAClsToMeasureSet(measureSetRepository, measureRepository);

    verify(measureRepository, new Times(1)).findAll();
    verify(measureSetRepository, new Times(4)).findByMeasureSetId(anyString());
    verify(measureSetRepository, new Times(3)).save(any(MeasureSet.class));
  }

  @Test
  void testRollback() {
    new MoveMeasureAclsToMeasureSetChangeUnit().rollbackExecution(measureSetRepository);
    verifyNoInteractions(measureSetRepository);
  }
}
