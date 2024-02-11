package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.InvalidMeasureSetIdException;
import cms.gov.madie.measure.exceptions.InvalidRequestException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import cms.gov.madie.measure.utils.SequenceGeneratorUtil;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeasureSetServiceTest {

  @InjectMocks private MeasureSetService measureSetService;
  @Mock MeasureSetRepository measureSetRepository;
  @Mock private ActionLogService actionLogService;
  @Mock private SequenceGeneratorUtil sequenceGeneratorUtil;
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

  @Test
  public void testUpdateMeasureSetAcls() {
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
  public void testUpdateMeasureSetToAddSecondAcl() {
    AclSpecification aclSpec1 = new AclSpecification();
    measureSet.setAcls(
        new ArrayList<>() {
          {
            add(aclSpec1);
          }
        });
    AclSpecification aclSpec2 = new AclSpecification();
    aclSpec2.setUserId("john_1");
    aclSpec2.setRoles(List.of(RoleEnum.SHARED_WITH));
    MeasureSet updatedMeasureSet =
        MeasureSet.builder()
            .measureSetId("1")
            .owner("john_1")
            .acls(List.of(aclSpec1, aclSpec2))
            .build();
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(updatedMeasureSet);

    MeasureSet measureSet = measureSetService.updateMeasureSetAcls("1", aclSpec2);
    assertThat(measureSet.getId(), is(equalTo(updatedMeasureSet.getId())));
    assertThat(measureSet.getOwner(), is(equalTo(updatedMeasureSet.getOwner())));
    assertThat(measureSet.getAcls().size(), is(equalTo(2)));
  }

  @Test
  public void testUpdateMeasureSetAclsWhenMeasureSetNotFound() {
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.empty());

    Exception ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> measureSetService.updateMeasureSetAcls("1", new AclSpecification()));
    assertTrue(ex.getMessage().contains("measure set may not exists."));
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }

  @Test
  public void testUpdateOwnership() {
    MeasureSet updatedMeasureSet = measureSet;
    updatedMeasureSet.setOwner("testUser");
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(updatedMeasureSet);

    MeasureSet result = measureSetService.updateOwnership("1", "testUser");
    assertThat(result.getId(), is(equalTo(updatedMeasureSet.getId())));
    assertThat(result.getOwner(), is(equalTo(updatedMeasureSet.getOwner())));
  }

  @Test
  public void testUpdateOwnershipWhenMeasureSetNotFound() {
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.empty());

    Exception ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> measureSetService.updateOwnership("1", "testUser"));
    assertTrue(ex.getMessage().contains("measure set may not exist."));
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }

  @Test
  public void testCreateCmsId() {
    final MeasureSet measureSet1 =
        MeasureSet.builder()
            .id("f225481c-921e-4015-9e14-e5046bfac9ff")
            .measureSetId("msid-2")
            .cmsId(2)
            .owner("user-1")
            .build();

    when(measureSetRepository.findByMeasureSetId(anyString()))
        .thenReturn(Optional.ofNullable(measureSet));
    when(sequenceGeneratorUtil.generateSequenceNumber("cms_id")).thenReturn(2);
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(measureSet1);

    MeasureSet result = measureSetService.createCmsId("measureSetId", "cms_id", "testUser");
    assertThat(result.getCmsId(), is(equalTo(2)));
    assertThat(result.getId(), is(equalTo(measureSet1.getId())));
  }

  @Test
  public void testCreateCmsIdWhenMeasureSetIdIsNotValid() {
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.empty());

    Exception ex =
        assertThrows(
            InvalidMeasureSetIdException.class,
            () -> measureSetService.createCmsId("measureSetId", "cms_id", "testUser"));
    assertTrue(
        ex.getMessage()
            .contains("No measure set exists for measure with measure set id measureSetId"));
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }

  @Test
  public void testCreateCmsIdWhenCmsIdAlreadyExistsInMeasureSet() {
    measureSet.setCmsId(6);
    when(measureSetRepository.findByMeasureSetId(anyString()))
        .thenReturn(Optional.ofNullable(measureSet));

    Exception ex =
        assertThrows(
            InvalidRequestException.class,
            () -> measureSetService.createCmsId("measureSetId", "cms_id", "testUser"));
    assertTrue(
        ex.getMessage().contains("cms id exists for measure with measure set id measureSetId"));
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }

  @Test
  public void testCreateCmsIdWhenSequenceNameIsNotValid() {
    when(measureSetRepository.findByMeasureSetId(anyString()))
        .thenReturn(Optional.ofNullable(measureSet));

    Exception ex =
        assertThrows(
            InvalidRequestException.class,
            () -> measureSetService.createCmsId("measureSetId", "cms", "testUser"));
    assertTrue(ex.getMessage().contains("cms is not a valid sequence name for generating cms id"));
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }
}
