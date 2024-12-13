package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.InvalidRequestException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.GeneratorRepository;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import gov.cms.madie.models.access.AclOperation;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeasureSetServiceTest {

  @InjectMocks private MeasureSetService measureSetService;
  @Mock MeasureRepository measureRepository;
  @Mock MeasureSetRepository measureSetRepository;
  @Mock GeneratorRepository generatorRepository;
  @Mock private ActionLogService actionLogService;
  MeasureSet measureSet;

  @BeforeEach
  public void setUp() {
    AclSpecification aclSpec = new AclSpecification();
    aclSpec.setUserId("john");
    aclSpec.setRoles(
        new HashSet<>() {
          {
            add(RoleEnum.SHARED_WITH);
          }
        });

    measureSet =
        MeasureSet.builder()
            .measureSetId("msid-2")
            .owner("user-1")
            .acls(
                new ArrayList<>() {
                  {
                    add(aclSpec);
                  }
                })
            .build();
  }

  @Test
  public void testCreateMeasureSet() {
    when(measureSetRepository.existsByMeasureSetId("msid-2")).thenReturn(false);
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(measureSet);
    measureSetService.createMeasureSet("user-1", "msid-xyz-p12r-12ert", "msid-2", null);

    verify(measureSetRepository, times(1)).existsByMeasureSetId("msid-2");
    verify(measureSetRepository, times(1)).save(any(MeasureSet.class));
    verify(actionLogService, times(1))
        .logAction(measureSet.getId(), Measure.class, ActionType.CREATED, "user-1");
  }

  @Test
  public void testNotCreateMeasureSetWhenMeasureSetIdExists() {
    when(measureSetRepository.existsByMeasureSetId("msid-2")).thenReturn(true);
    measureSetService.createMeasureSet("user-1", "msid-xyz-p12r-12ert", "msid-2", "2");
    verify(measureSetRepository, times(1)).existsByMeasureSetId("msid-2");
    verify(measureSetRepository, times(0)).save(measureSet);
  }

  @Test
  public void testGrantOperationAsFirstNewAcl() {
    AclSpecification aclSpec = new AclSpecification();
    aclSpec.setUserId("john_1");
    aclSpec.setRoles(Set.of(RoleEnum.SHARED_WITH));
    AclOperation aclOperation =
        AclOperation.builder().acls(List.of(aclSpec)).action(AclOperation.AclAction.GRANT).build();
    MeasureSet updatedMeasureSet =
        MeasureSet.builder().measureSetId("1").owner("john_1").acls(List.of(aclSpec)).build();
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(updatedMeasureSet);

    MeasureSet measureSet = measureSetService.updateMeasureSetAcls("1", aclOperation);
    assertThat(measureSet.getId(), is(equalTo(updatedMeasureSet.getId())));
    assertThat(measureSet.getOwner(), is(equalTo(updatedMeasureSet.getOwner())));
    assertThat(measureSet.getAcls().size(), is(equalTo(1)));
  }

  @Test
  public void testGrantOperationAsSecondNewAcl() {
    AclSpecification aclSpec1 = new AclSpecification();
    aclSpec1.setUserId("john");
    AclSpecification aclSpec2 = new AclSpecification();
    aclSpec2.setUserId("jane");
    aclSpec2.setRoles(Set.of(RoleEnum.SHARED_WITH));
    AclOperation aclOperation =
        AclOperation.builder().acls(List.of(aclSpec2)).action(AclOperation.AclAction.GRANT).build();
    MeasureSet updatedMeasureSet =
        MeasureSet.builder()
            .measureSetId("1")
            .owner("john")
            .acls(List.of(aclSpec1, aclSpec2))
            .build();
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(updatedMeasureSet);

    MeasureSet measureSet = measureSetService.updateMeasureSetAcls("1", aclOperation);
    assertThat(measureSet.getId(), is(equalTo(updatedMeasureSet.getId())));
    assertThat(measureSet.getOwner(), is(equalTo(updatedMeasureSet.getOwner())));
    assertThat(measureSet.getAcls().size(), is(equalTo(2)));
  }

  @Test
  public void testGrantOperationUpdateAcl() {
    AclSpecification aclSpec1 = new AclSpecification();
    aclSpec1.setUserId("john");
    aclSpec1.setRoles(
        new HashSet<>() {
          {
            add(RoleEnum.SHARED_WITH);
          }
        });
    AclSpecification aclSpec2 = new AclSpecification();
    aclSpec2.setUserId("john");
    aclSpec2.setRoles(Set.of(RoleEnum.SHARED_WITH));
    AclOperation aclOperation =
        AclOperation.builder().acls(List.of(aclSpec2)).action(AclOperation.AclAction.GRANT).build();
    MeasureSet updatedMeasureSet =
        MeasureSet.builder().measureSetId("1").owner("john").acls(List.of(aclSpec1)).build();
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(updatedMeasureSet);

    MeasureSet measureSet = measureSetService.updateMeasureSetAcls("1", aclOperation);
    assertThat(measureSet.getId(), is(equalTo(updatedMeasureSet.getId())));
    assertThat(measureSet.getOwner(), is(equalTo(updatedMeasureSet.getOwner())));
    assertThat(measureSet.getAcls().size(), is(equalTo(1)));
    assertThat(measureSet.getAcls().get(0).getUserId(), is(equalTo(aclSpec2.getUserId())));
  }

  @Test
  public void testRevokeOperation() {
    AclSpecification aclSpec = new AclSpecification();
    aclSpec.setUserId("john");
    aclSpec.setRoles(
        new HashSet<>() {
          {
            add(RoleEnum.SHARED_WITH);
          }
        });
    AclOperation aclOperation =
        AclOperation.builder().acls(List.of(aclSpec)).action(AclOperation.AclAction.REVOKE).build();
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(measureSet);

    MeasureSet updatedMeasureSet = measureSetService.updateMeasureSetAcls("1", aclOperation);
    assertThat(updatedMeasureSet.getId(), is(equalTo(measureSet.getId())));
    assertThat(updatedMeasureSet.getOwner(), is(equalTo(measureSet.getOwner())));
    assertThat(updatedMeasureSet.getAcls().size(), is(equalTo(0)));
  }

  @Test
  public void testUpdateMeasureSetAclsWhenMeasureSetNotFound() {
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.empty());

    Exception ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> measureSetService.updateMeasureSetAcls("1", new AclOperation()));
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
        MeasureSet.builder().measureSetId("msid-2").cmsId(2).owner("user-1").build();

    when(measureSetRepository.findByMeasureSetId(anyString()))
        .thenReturn(Optional.ofNullable(measureSet));
    when(generatorRepository.findAndModify("cms_id")).thenReturn(2);
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(measureSet1);

    MeasureSet result = measureSetService.createAndUpdateCmsId("measureSetId", "testUser");
    assertThat(result.getCmsId(), is(equalTo(2)));
    assertThat(result.getId(), is(equalTo(measureSet1.getId())));
    verify(actionLogService, times(1))
        .logAction(measureSet.getId(), Measure.class, ActionType.CREATED, "testUser");
  }

  @Test
  public void testCreateCmsIdWhenMeasureSetIdIsNotValid() {
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.empty());

    Exception ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> measureSetService.createAndUpdateCmsId("measureSetId", "testUser"));
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
            () -> measureSetService.createAndUpdateCmsId("measureSetId", "testUser"));
    assertTrue(
        ex.getMessage()
            .contains(
                "CMS ID already exists. Once a CMS Identifier has been generated it may not be modified or removed for any draft or version of a measure."));
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }

  @Test
  public void testDeleteCmsId() {
    Integer cmsId = 1;
    Measure measure =
        Measure.builder().model(ModelType.QI_CORE.getValue()).measureSetId("measureSetId1").build();

    List<Measure> measures = Collections.singletonList(measure);
    MeasureSet measureSet = MeasureSet.builder().measureSetId("1").cmsId(1).build();
    String measureId = "measureId";

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));
    when(measureRepository.findAllByMeasureSetIdAndActive(anyString(), anyBoolean()))
        .thenReturn(measures);
    when(measureSetRepository.save(any(MeasureSet.class))).thenReturn(measureSet);

    String responseBody = measureSetService.deleteCmsId(measureId, cmsId);

    assertEquals(
        responseBody,
        String.format(
            "CMS id of %s was deleted successfully from measure set with measure set id of %s",
            cmsId, measure.getMeasureSetId()));
    verify(measureRepository, times(1)).findById(anyString());
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureRepository, times(1)).findAllByMeasureSetIdAndActive(anyString(), anyBoolean());
    verify(measureSetRepository, times(1)).save(any(MeasureSet.class));
  }

  @Test
  public void testDeleteCmsIdWhenMeasureWithMeasureIdIsNotFound() {
    String measureId = "measureId";

    when(measureRepository.findById(anyString())).thenReturn(Optional.empty());

    Exception ex =
        assertThrows(
            ResourceNotFoundException.class, () -> measureSetService.deleteCmsId(measureId, 1));

    assertTrue(
        ex.getMessage()
            .contains(String.format("No measure exists with measure id of %s", measureId)));
    verify(measureRepository, times(1)).findById(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }

  @Test
  public void testDeleteCmsIdWhenMeasureSetIsNotFound() {
    Measure measure =
        Measure.builder().model(ModelType.QI_CORE.getValue()).measureSetId("measureSetId").build();

    String measureId = "measureId";

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.empty());

    Exception ex =
        assertThrows(
            ResourceNotFoundException.class, () -> measureSetService.deleteCmsId(measureId, 1));

    assertTrue(
        ex.getMessage()
            .contains(
                String.format(
                    "No measure set exists for measure with measure set id of %s",
                    measure.getMeasureSetId())));
    verify(measureRepository, times(1)).findById(anyString());
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }

  @Test
  public void testDeleteCmsIdWhenCmsIdIsNotFoundInMeasureSet() {
    Integer cmsId = 1;
    Measure measure =
        Measure.builder().model(ModelType.QI_CORE.getValue()).measureSetId("measureSetId").build();

    MeasureSet measureSet = MeasureSet.builder().measureSetId("1").build();
    String measureId = "measureId";

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));

    Exception ex =
        assertThrows(
            ResourceNotFoundException.class, () -> measureSetService.deleteCmsId(measureId, cmsId));

    assertTrue(
        ex.getMessage()
            .contains(
                String.format(
                    "No CMS id of %s exists to be deleted within measure set with measure set id of %s",
                    cmsId, measure.getMeasureSetId())));
    verify(measureRepository, times(1)).findById(anyString());
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }

  @Test
  public void testDeleteCmsIdWhenCmsIdToDeleteDoesNotMatchCmsIdInMeasureSet() {
    Integer cmsId = 1;
    Measure measure =
        Measure.builder().model(ModelType.QI_CORE.getValue()).measureSetId("measureSetId").build();

    MeasureSet measureSet = MeasureSet.builder().measureSetId("1").cmsId(2).build();
    String measureId = "measureId";

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure));
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));

    Exception ex =
        assertThrows(
            InvalidIdException.class, () -> measureSetService.deleteCmsId(measureId, cmsId));

    assertTrue(
        ex.getMessage()
            .contains(
                String.format(
                    "CMS id of %s passed in does not match CMS id of %s within measure set with measure set id of %s",
                    cmsId, measureSet.getCmsId(), measure.getMeasureSetId())));
    verify(measureRepository, times(1)).findById(anyString());
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }

  @Test
  public void testDeleteCmsIdWhenMeasureHasMultipleVersions() {
    Integer cmsId = 1;
    Measure measure1 =
        Measure.builder().model(ModelType.QI_CORE.getValue()).measureSetId("measureSetId1").build();

    Measure measure2 =
        Measure.builder().model(ModelType.QI_CORE.getValue()).measureSetId("measureSetId2").build();

    List<Measure> measures = Arrays.asList(measure1, measure2);
    MeasureSet measureSet = MeasureSet.builder().measureSetId("1").cmsId(1).build();
    String measureId = "measureId";

    when(measureRepository.findById(anyString())).thenReturn(Optional.of(measure1));
    when(measureSetRepository.findByMeasureSetId(anyString())).thenReturn(Optional.of(measureSet));
    when(measureRepository.findAllByMeasureSetIdAndActive(anyString(), anyBoolean()))
        .thenReturn(measures);

    Exception ex =
        assertThrows(
            InvalidRequestException.class, () -> measureSetService.deleteCmsId(measureId, cmsId));

    assertTrue(
        ex.getMessage()
            .contains(
                String.format(
                    String.format(
                        "Measure set with measure set id of %s contains more than 1 measure. Cannot delete CMS id when measure set has more than 1 version of measure.",
                        measure1.getMeasureSetId()))));
    verify(measureRepository, times(1)).findById(anyString());
    verify(measureSetRepository, times(1)).findByMeasureSetId(anyString());
    verify(measureRepository, times(1)).findAllByMeasureSetIdAndActive(anyString(), anyBoolean());
    verify(measureSetRepository, times(0)).save(any(MeasureSet.class));
  }
}
