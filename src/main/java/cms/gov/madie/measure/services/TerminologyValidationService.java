package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.ValueSetsSearchCriteria;
import cms.gov.madie.measure.exceptions.InvalidTerminologyException;
import gov.cms.madie.models.cql.terminology.CqlCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminologyValidationService {
  private final TerminologyServiceClient terminologyServiceClient;

  public void validateTerminology(String elm, String accessToken) {
    if (StringUtils.isBlank(elm)) {
      return;
    }
    validateValueSets(elm, accessToken);
    validateCodes(elm, accessToken);
  }

  public void validateValueSets(String elm, String accessToken) {
    List<String> oids = getValueSetOids(elm);
    if (CollectionUtils.isEmpty(oids)) {
      return;
    }
    var searchCriteria =
        ValueSetsSearchCriteria.builder()
            .includeDraft(true)
            .valueSetParams(
                oids.stream()
                    .map(oid -> ValueSetsSearchCriteria.ValueSetParams.builder().oid(oid).build())
                    .toList())
            .build();
    terminologyServiceClient.fetchValueSets(searchCriteria, accessToken);
  }

  public void validateCodes(String elm, String accessToken) {
    List<CqlCode> cqlCodes = getCqlCodes(elm);
    if (CollectionUtils.isEmpty(cqlCodes)) {
      return;
    }
    List<CqlCode> validatedCodes = terminologyServiceClient.validateCodes(cqlCodes, accessToken);
    // throw error if there is at least one invalid code
    CqlCode invalidCode =
        validatedCodes.stream().filter(cqlCode -> !cqlCode.isValid()).findFirst().orElse(null);
    if (invalidCode != null) {
      log.error("Invalid cql codes: " + invalidCode.getCodeId());
      throw new InvalidTerminologyException("CQL Code", invalidCode.getCodeId());
    }
  }

  public List<String> getValueSetOids(String elm) {
    var library = getLibrary(elm);
    var valueSets = (Map<String, Object>) library.get("valueSets");
    if (valueSets == null) {
      return null;
    }
    var valueSetDefs = (List<Map<String, String>>) valueSets.get("def");
    if (CollectionUtils.isEmpty(valueSetDefs)) {
      return null;
    }
    return valueSetDefs.stream()
        .map(valueSetDef -> getOidFromValueSetId(valueSetDef.get("id")))
        .collect(Collectors.toList());
  }

  public List<CqlCode> getCqlCodes(String elm) {
    var library = getLibrary(elm);
    var codeSystems = (Map<String, Object>) library.get("codeSystems");
    var codes = (Map<String, Object>) library.get("codes");
    if (CollectionUtils.isEmpty(codes)) {
      return null;
    }
    var codeDefs = (List<Map<String, Object>>) codes.get("def");
    return codeDefs.stream().map(codeDef -> getCqlCode(codeDef, codeSystems)).toList();
  }

  private Map<String, Object> getLibrary(String elm) {
    GsonJsonParser jsonParser = new GsonJsonParser();
    var elmMap = jsonParser.parseMap(elm);
    return (Map<String, Object>) elmMap.get("library");
  }

  private String getOidFromValueSetId(String valueSetId) {
    String valueSetBaseUrl = "http://cts.nlm.nih.gov/fhir/ValueSet/";
    var oidParts = valueSetId.split(valueSetBaseUrl);
    if (oidParts.length == 2) {
      return oidParts[1];
    } else {
      log.error("Invalid value set: " + valueSetId);
      throw new InvalidTerminologyException("ValueSet", valueSetId);
    }
  }

  private CqlCode getCqlCode(Map<String, Object> codeDefinition, Map<String, Object> codeSystems) {
    if (codeDefinition == null) {
      return null;
    }
    var codeSystemName = ((Map<String, String>) codeDefinition.get("codeSystem")).get("name");
    return CqlCode.builder()
        .name((String) codeDefinition.get("name"))
        .codeId((String) codeDefinition.get("id"))
        .text((String) codeDefinition.get("display"))
        .codeSystem(getCodeSystem(codeSystems, codeSystemName))
        .build();
  }

  private CqlCode.CqlCodeSystem getCodeSystem(
      Map<String, Object> codeSystems, String codeSystemName) {
    if (codeSystems == null) {
      return null;
    }
    var codeSystemDefs = (List<Map<String, String>>) codeSystems.get("def");
    if (CollectionUtils.isEmpty(codeSystemDefs)) {
      return null;
    }
    return codeSystemDefs.stream()
        .filter(codeSystemDef -> StringUtils.equals(codeSystemDef.get("name"), codeSystemName))
        .map(
            codeSystemDef ->
                CqlCode.CqlCodeSystem.builder()
                    .oid(codeSystemDef.get("id"))
                    .name(codeSystemDef.get("name"))
                    .version(codeSystemDef.get("version"))
                    .build())
        .findFirst()
        .orElse(null);
  }
}
