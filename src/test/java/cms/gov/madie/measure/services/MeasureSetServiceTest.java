package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.MeasureSetRepository;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    when(measureSetRepository.findByMeasureSetId("msid-2")).thenReturn(Optional.empty());
    when(measureSetRepository.save(measureSet)).thenReturn(measureSet);
    measureSetService.createMeasureSet("user-1", "msid-xyz-p12r-12ert", "msid-2");

    verify(measureSetRepository, times(1)).findByMeasureSetId("msid-2");
    verify(measureSetRepository, times(1)).save(measureSet);
    verify(actionLogService, times(1))
        .logAction(measureSet.getId(), Measure.class, ActionType.CREATED, "user-1");
  }

  @Test
  public void testNotCreateMeasureSetWhenMeasureSetIdExists() {
    when(measureSetRepository.findByMeasureSetId("msid-2")).thenReturn(Optional.of(measureSet));
    measureSetService.createMeasureSet("user-1", "msid-xyz-p12r-12ert", "msid-2");
    verify(measureSetRepository, times(1)).findByMeasureSetId("msid-2");
    verify(measureSetRepository, times(0)).save(measureSet);
  }

  @Test
  public void testMeasureSetAcls() {
    AclSpecification aclSpec = new AclSpecification();
    aclSpec.setUserId("john_1");
    aclSpec.setRoles(List.of(RoleEnum.SHARED_WITH));
    MeasureSet updatedMeasureSet =
        MeasureSet.builder().measureSetId("1").owner("john_1").acls(List.of(aclSpec)).build();
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(updatedMeasureSet);

    MeasureSet measureSet = measureSetService.updateMeasureSetAcls("1", aclSpec);
    assertThat(measureSet.getId(), is(equalTo(updatedMeasureSet.getId())));
    assertThat(measureSet.getOwner(), is(equalTo(updatedMeasureSet.getOwner())));
    assertThat(measureSet.getAcls().size(), is(equalTo(1)));
  }

  @Test
  public void testMeasureSetAclsWhenMeasureSetNotFound() {
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.empty());

    MeasureSet measureSet = measureSetService.updateMeasureSetAcls("1", new AclSpecification());
    assertThat(measureSet, is(equalTo(null)));
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }
}
