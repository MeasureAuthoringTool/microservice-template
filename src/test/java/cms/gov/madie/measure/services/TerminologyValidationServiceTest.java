package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.ValueSetsSearchCriteria;
import cms.gov.madie.measure.exceptions.InvalidTerminologyException;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.cql.terminology.CqlCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TerminologyValidationServiceTest implements ResourceUtil {

  @Mock private TerminologyServiceClient terminologyServiceClient;

  @InjectMocks private TerminologyValidationService terminologyValidationService;

  private String elmJson;

  @BeforeEach
  public void setup() throws Exception {
    elmJson = getData("/test_elm.json");
  }

  @Test
  public void testValidateTerminologyWhenNoErrors() throws Exception {
    when(terminologyServiceClient.fetchValueSets(any(ValueSetsSearchCriteria.class), anyString()))
        .thenReturn("[json]");

    when(terminologyServiceClient.validateCodes(any(), anyString()))
        .thenReturn(List.of(CqlCode.builder().isValid(true).build()));
    terminologyValidationService.validateTerminology(elmJson, "token");
    verify(terminologyServiceClient, times(1)).fetchValueSets(any(), any());
    verify(terminologyServiceClient, times(1)).validateCodes(any(), any());
  }

  @Test
  public void testValidateTerminologyWhenErrorsWithValueSets() throws Exception {
    when(terminologyServiceClient.fetchValueSets(any(ValueSetsSearchCriteria.class), anyString()))
        .thenThrow(InvalidTerminologyException.class);

    assertThrows(
        InvalidTerminologyException.class,
        () -> terminologyValidationService.validateTerminology(elmJson, "token"),
        "Invalid value set");
    verify(terminologyServiceClient, times(1)).fetchValueSets(any(), any());
  }

  @Test
  public void testValidateTerminologyWhenErrorsWithCqlCodes() throws Exception {
    when(terminologyServiceClient.fetchValueSets(any(ValueSetsSearchCriteria.class), anyString()))
        .thenReturn("[json]");

    when(terminologyServiceClient.validateCodes(any(), anyString()))
        .thenReturn(List.of(CqlCode.builder().isValid(false).build()));
    assertThrows(
        InvalidTerminologyException.class,
        () -> terminologyValidationService.validateTerminology(elmJson, "token"),
        "Invalid Cql code");
    verify(terminologyServiceClient, times(1)).fetchValueSets(any(), any());
    verify(terminologyServiceClient, times(1)).validateCodes(any(), any());
  }
}
