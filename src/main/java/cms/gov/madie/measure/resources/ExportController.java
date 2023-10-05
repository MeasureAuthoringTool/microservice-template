package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.BundleService;
import cms.gov.madie.measure.services.FhirServicesClient;
import cms.gov.madie.measure.utils.ControllerUtil;
import cms.gov.madie.measure.utils.ExportFileNamesUtil;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.commons.lang3.SerializationUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Configuration;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExportController {

  private final MeasureRepository measureRepository;

  private final BundleService bundleService;

  private final FhirServicesClient fhirServicesClient;

  @GetMapping(path = "/measures/{id}/exports", produces = "application/zip")
  public ResponseEntity<byte[]> getZip(
      Principal principal,
      @PathVariable("id") String id,
      @RequestHeader("Authorization") String accessToken) {

    final String username = principal.getName();
    log.info("User [{}] is attempting to export measure [{}]", username, id);

    Optional<Measure> measureOptional = measureRepository.findById(id);

    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", id);
    }

    Measure measure = measureOptional.get();

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment;filename=\"" + ExportFileNamesUtil.getExportFileName(measure) + ".zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(bundleService.exportBundleMeasure(measure, accessToken));
  }

  @PutMapping(path = ControllerUtil.TEST_CASES + "/exports", produces = "application/zip")
  public ResponseEntity<byte[]> getTestCaseExport(
      Principal principal,
      @RequestHeader("Authorization") String accessToken,
      @PathVariable String measureId,
      @PathVariable Optional<String> bundleType,
      @RequestBody List<String> testCaseId) {

    final String username = principal.getName();
    log.info("User [{}] is attempting to export test cases for [{}]", username, measureId);

    Optional<Measure> measureOptional = measureRepository.findById(measureId);

    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", measureId);
    }

    Measure measure = measureOptional.get();
    // change Measure Bundle Type to "type" for export
    switch (BundleType.valueOf(bundleType.orElse(("COLLECTION")))) {
      case COLLECTION:
        log.debug("You're exporting a Collection");
        // update bundle type for each entry MAT 6405
        // break ;
      case TRANSACTION:
        log.debug("You're exporting a Transaction");
        // update bundle type and add entry.request for each entry
        if (measure.getTestCases() != null) {
          measure.getTestCases().stream().forEach(testCase -> updateEntry(testCase));
        }
        break;
      default:
    }

    return fhirServicesClient.getTestCaseExports(measure, accessToken, testCaseId);
  }

  private void updateEntry(TestCase testCase) {
    String json =
        JsonPath.parse(testCase.getJson())
            .map(
                "$.type",
                ((curVal, config) -> {
                  return "transaction";
                }))
            .map(
                "$.entry",
                (curVal, config) -> {
                  log.info("Entries ", curVal);
                  JSONArray entryArr = (JSONArray) curVal;

                  entryArr.forEach(
                      entry -> {
                        JSONObject request = new JSONObject();

                        request.put("method", "PUT");
                        String resourceType =
                            (String)
                                ((LinkedHashMap<String, LinkedHashMap>) entry)
                                    .get("resource")
                                    .get("resourceType");
                        String id =
                            (String)
                                ((LinkedHashMap<String, LinkedHashMap>) entry)
                                    .get("resource")
                                    .get("id");
                        // ((LinkedHashMap<String, Object>) entry)
                        request.put("url", String.format("%s/%s", resourceType, id));

                        ((LinkedHashMap<String, Object>) entry).put("request", request);
                        log.info("Entry ${}", entry);
                      });

                  return entryArr;
                })
            .jsonString();

    testCase.setJson(json);
  }
}

enum BundleType {
  TRANSACTION("transaction"),
  COLLECTION("collection");

  private final String bundleType;

  private BundleType(String s) {
    bundleType = s;
  }

  public boolean equalsType(String otherType) {
    return bundleType.equals(otherType);
  }

  public String toString() {
    return this.bundleType;
  }
}
