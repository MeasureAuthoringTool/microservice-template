package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.OrganizationRepository;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMeasureOrganizationsChangeUnitTest {

  @Mock
  private MeasureRepository measureRepository;
  @Mock
  private OrganizationRepository organizationRepository;
  @Mock private Measure measure;
  @InjectMocks
  private UpdateMeasureOrganizationsChangeUnit updateMeasureOrganizationsChangeUnit;

  private Measure testMeasure1;
  private Measure testMeasure2;
  private Measure testMeasure3;
  private Measure testMeasure4;
  private MeasureMetaData measureMetaData;

  public List<Organization> buildOrganizations() {
    return List.of(
        Organization.builder()
            .id("OrgId1")
            .name("SemanticBits")
            .build(),
        Organization.builder()
            .id("OrgId2")
            .name("ICF")
            .build(),
        Organization.builder()
            .id("OrgId3")
            .name("AltoPoint")
            .build()
    );
  }

  @BeforeEach
  void setUp() {

  }

  @Test
  void updateMeasureOrganizations() throws Exception {
    // given
    when(organizationRepository.findAll()).thenReturn(buildOrganizations());

    // when
    updateMeasureOrganizationsChangeUnit.updateMeasureOrganizations(measureRepository, organizationRepository);

    // then


  }
}
