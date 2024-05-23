package cms.gov.madie.measure.resources;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import cms.gov.madie.measure.services.CqmMeasureService;
import gov.cms.madie.models.cqm.CqmMeasure;

@ExtendWith(MockitoExtension.class)
class CqmMeasureControllerTest {

  @Mock private CqmMeasureService cqmMeasureService;

  @InjectMocks private CqmMeasureController cqmMeasureController;

  @Test
  void testGetCqmMeasure() {
    CqmMeasure cqmMeasure = new CqmMeasure();
    cqmMeasure.setCms_id("Twelve");
    doReturn(cqmMeasure)
        .when(cqmMeasureService)
        .getCqmMeasure(any(String.class), any(String.class));

    ResponseEntity<CqmMeasure> response = cqmMeasureController.getCqmMeasure("123", "345");

    assertNotNull(response);
    assertNotNull(response.getBody());
    assertTrue(response.getBody() instanceof CqmMeasure);
    CqmMeasure result = response.getBody();
    assertEquals(result.getCms_id(), "Twelve");
  }
}
