package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cms.gov.madie.measure.repositories.CqmMeasureRepository;
import gov.cms.madie.models.cqm.CqmMeasure;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;

@ExtendWith(MockitoExtension.class)
class CqmMeasureServiceTest {

  @Mock MeasureService measureService;
  @Mock MeasureMetaData measureMetaData;
  @Mock QdmPackageService qdmPackageService;
  @Mock CqmMeasure cqmMeasure;
  @Mock Measure measure;
  @Mock CqmMeasureRepository cqmMeasureRepo;

  @InjectMocks private CqmMeasureService cqmMeasureService;

  @Test
  void testGetCqmMeasureForDraft() {

    doReturn(measure).when(measureService).findMeasureById(any(String.class));
    doReturn(measureMetaData).when(measure).getMeasureMetaData();
    doReturn(true).when(measureMetaData).isDraft();
    doReturn(cqmMeasure).when(qdmPackageService).convertCqm(eq(measure), eq("234"));

    CqmMeasure result = cqmMeasureService.getCqmMeasure("123", "234");
    assertNotNull(result);
  }

  @Test
  void testGetCqmMeasureForFinal() {

    doReturn(measure).when(measureService).findMeasureById(any(String.class));
    doReturn(measureMetaData).when(measure).getMeasureMetaData();
    doReturn("234").when(measure).getVersionId();
    doReturn("123").when(measure).getMeasureSetId();
    doReturn(false).when(measureMetaData).isDraft();

    doReturn(cqmMeasure)
        .when(cqmMeasureRepo)
        .findByHqmfSetIdAndHqmfVersionNumber(eq("123"), eq("234"));

    CqmMeasure result = cqmMeasureService.getCqmMeasure("123", "234");
    assertNotNull(result);
  }
}
