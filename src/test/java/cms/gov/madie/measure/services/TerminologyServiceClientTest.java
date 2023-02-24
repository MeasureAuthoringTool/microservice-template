package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.TerminologyServiceConfig;
import cms.gov.madie.measure.dto.ValueSetsSearchCriteria;
import gov.cms.madie.models.cql.terminology.CqlCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TerminologyServiceClientTest {
  @Mock private TerminologyServiceConfig terminologyServiceConfig;
  @Mock private RestTemplate terminologyRestTemplate;

  @InjectMocks private TerminologyServiceClient terminologyServiceClient;

  @Test
  public void testFetchValueSets() {
    var testVs = "two test value sets";
    var oids = List.of("1.2.3.4", "4.5.6.7");
    var criteria =
        ValueSetsSearchCriteria.builder()
            .includeDraft(true)
            .valueSetParams(
                oids.stream()
                    .map(oid -> ValueSetsSearchCriteria.ValueSetParams.builder().oid(oid).build())
                    .toList())
            .build();
    when(terminologyRestTemplate.exchange(any(RequestEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(testVs));
    String response = terminologyServiceClient.fetchValueSets(criteria, "token");
    assertThat(response, is(equalTo(testVs)));
  }

  @Test
  public void testValidateCodes() {
    var codeSystem =
        CqlCode.CqlCodeSystem.builder()
            .oid("http://loinc.org")
            .name("LOINC")
            .version("1.2.3")
            .build();
    var cqlCode1 =
        CqlCode.builder()
            .name("Blood pressure")
            .codeId("55284-4")
            .text("Blood pressure")
            .codeSystem(codeSystem)
            .build();

    var cqlCode2 =
        CqlCode.builder()
            .name("Systolic blood pressure")
            .codeId("8480-6")
            .text("Systolic blood pressure")
            .codeSystem(codeSystem)
            .build();

    // create mock cql response codes
    var validCode = cqlCode1.builder().isValid(true).build();
    var invalidCode = cqlCode2.builder().isValid(false).build();

    when(terminologyRestTemplate.exchange(
            any(RequestEntity.class), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(List.of(validCode, invalidCode)));
    List<CqlCode> validatedCodes =
        terminologyServiceClient.validateCodes(List.of(cqlCode1, cqlCode2), "token");
    assertThat(validatedCodes.size(), is(equalTo(2)));
    assertThat(validatedCodes.get(0).isValid(), is(equalTo(true)));
    assertThat(validatedCodes.get(1).isValid(), is(equalTo(false)));
  }
}
