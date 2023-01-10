package cms.gov.madie.measure.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import cms.gov.madie.measure.utils.ExportFileNamesUtil;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@AllArgsConstructor
public class ExportService {

  private final BundleService bundleService;
  private FhirContext fhirContext;

  private static final String TEXT_CQL = "text/cql";
  private static final String CQL_DIRECTORY = "/cql/";
  private static final String RESOURCE_DIRECTORY = "/resources/";

  public void generateExports(Measure measure, String accessToken, OutputStream outputStream) {
    String exportFileName = ExportFileNamesUtil.getExportFileName(measure);
    log.info("Generating exports for " + exportFileName);

    String measureBundle = bundleService.bundleMeasure(measure, accessToken);
    Bundle bundle = createFhirResourceFromJson(measureBundle);
    try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
      addMeasureBundleToExport(zos, exportFileName, measureBundle);
      addLibraryCqlFilesToExport(zos, bundle);
      addLibraryResourcesToExport(zos, bundle);
    } catch (Exception ex) {
      log.error(ex.getMessage());
      throw new RuntimeException(
          "Unexpected error while generating exports for measureID: " + measure.getId());
    }
  }

  private void addMeasureBundleToExport(ZipOutputStream zos, String fileName, String measureBundle)
      throws IOException {
    String measureBundleFile = fileName + ".json";
    addBytesToZip(measureBundleFile, measureBundle.getBytes(), zos);
  }

  private void addLibraryCqlFilesToExport(ZipOutputStream zos, Bundle measureBundle)
      throws IOException {
    Map<String, String> cqlMap = getCQLForLibraries(measureBundle);
    for (Map.Entry<String, String> entry : cqlMap.entrySet()) {
      String filePath = CQL_DIRECTORY + entry.getKey() + ".cql";
      String data = entry.getValue();
      addBytesToZip(filePath, data.getBytes(), zos);
    }
  }

  private void addLibraryResourcesToExport(ZipOutputStream zos, Bundle measureBundle)
      throws IOException {
    List<Library> libraries = getLibraryResources(measureBundle);
    for (Library library : libraries) {
      String jsonString = convertFhirResourceToJsonString(library);
      String xmlString = convertFhirResourceToXmlString(library);
      String fileName = RESOURCE_DIRECTORY + "library-" + library.getName();
      addBytesToZip(fileName + ".json", jsonString.getBytes(), zos);
      addBytesToZip(fileName + ".xml", xmlString.getBytes(), zos);
    }
  }

  private List<Library> getLibraryResources(Bundle measureBundle) {
    return measureBundle.getEntry().stream()
        .filter(
            entry -> StringUtils.equals("Library", entry.getResource().getResourceType().name()))
        .map(entry -> (Library) entry.getResource())
        .toList();
  }

  private Map<String, String> getCQLForLibraries(Bundle measureBundle) {
    Map<String, String> libraryCqlMap = new HashMap<>();
    List<Library> libraries = getLibraryResources(measureBundle);
    for (Library library : libraries) {
      Attachment attachment = getCqlAttachment(library);
      String cql = new String(attachment.getData());
      libraryCqlMap.put(library.getName(), cql);
    }
    return libraryCqlMap;
  }

  private Attachment getCqlAttachment(Library library) {
    return library.getContent().stream()
        .filter(content -> StringUtils.equals(TEXT_CQL, content.getContentType()))
        .findAny()
        .orElse(null);
  }

  /**
   * Adds the bytes to zip.
   *
   * @param path file name along with path and extension
   * @param input the input byte array
   * @param zipOutputStream the zip
   * @throws IOException the exception
   */
  void addBytesToZip(String path, byte[] input, ZipOutputStream zipOutputStream)
      throws IOException {
    ZipEntry entry = new ZipEntry(path);
    entry.setSize(input.length);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(input);
    zipOutputStream.closeEntry();
  }

  private IParser fhirJsonParser() {
    return fhirContext.newJsonParser();
  }

  private Bundle createFhirResourceFromJson(String json) {
    return fhirJsonParser().parseResource(Bundle.class, json);
  }

  private String convertFhirResourceToJsonString(Resource resource) {
    return fhirJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
  }

  private String convertFhirResourceToXmlString(Resource resource) {
    return fhirContext.newXmlParser().setPrettyPrint(true).encodeResourceToString(resource);
  }
}
