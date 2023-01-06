package cms.gov.madie.measure.services;

import ca.uhn.fhir.context.FhirContext;
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

  public void generateExports(Measure measure, String accessToken, OutputStream outputStream) {
    String exportFileName = ExportFileNamesUtil.getExportFileName(measure);
    log.info("Generating exports for " + exportFileName);

    String measureBundle = bundleService.bundleMeasure(measure, accessToken);
    Bundle bundle = createFhirResourceFromJson(measureBundle, Bundle.class);
    try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
      addMeasureBundleToExport(zos, exportFileName, measureBundle);
      addLibraryCqlFilesToExport(zos, bundle);
    } catch (Exception ex) {
      log.error(ex.getMessage());
      throw new RuntimeException(
          "Unexpected error while generating exports for measureID: " + measure.getId());
    }
  }

  public <T extends Resource> T createFhirResourceFromJson(String json, Class<T> clazz) {
    return fhirContext.newJsonParser().parseResource(clazz, json);
  }

  public void addMeasureBundleToExport(ZipOutputStream zos, String fileName, String measureBundle)
      throws IOException {
    String measureBundleFile = fileName + ".json";
    addBytesToZip(measureBundleFile, measureBundle.getBytes(), zos);
  }

  public void addLibraryCqlFilesToExport(ZipOutputStream zos, Bundle measureBundle)
      throws IOException {
    Map<String, String> cqlMap = getCQLForLibraries(measureBundle);
    for (Map.Entry<String, String> entry : cqlMap.entrySet()) {
      String filePath = CQL_DIRECTORY + entry.getKey() + ".cql";
      String data = entry.getValue();
      addBytesToZip(filePath, data.getBytes(), zos);
    }
  }

  private Map<String, String> getCQLForLibraries(Bundle measureBundle) {
    Map<String, String> libraryCqlMap = new HashMap<>();
    measureBundle.getEntry().stream()
        .filter(
            entry -> StringUtils.equals("Library", entry.getResource().getResourceType().name()))
        .forEach(
            entry -> {
              Library library = (Library) entry.getResource();
              Attachment attachment = getCqlAttachment(library);
              String cql = new String(attachment.getData());
              String key = library.getName() + "-v" + library.getVersion();
              libraryCqlMap.put(key, cql);
            });
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
  private void addBytesToZip(String path, byte[] input, ZipOutputStream zipOutputStream)
      throws IOException {
    ZipEntry entry = new ZipEntry(path);
    entry.setSize(input.length);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(input);
    zipOutputStream.closeEntry();
  }
}
