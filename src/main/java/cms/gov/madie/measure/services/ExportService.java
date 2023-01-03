package cms.gov.madie.measure.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class ExportService {

  private final BundleService bundleService;

  public void zipFile(Measure measure, String accessToken, OutputStream outputStream) {
    String zipFileName =
        measure.getEcqmTitle() + "-v" + measure.getVersion() + "-" + measure.getModel();

    log.info("Entering of zipFile(): zipFileName = " + zipFileName);
    String fhirBundleJson = zipFileName + ".json";
    List<String> exportFiles = new ArrayList<>(Collections.singleton(fhirBundleJson));
    String cql = "/cql/" + measure.getMeasureName() + ".cql";
    exportFiles.add(cql);

    try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
      for (String filePath : exportFiles) {
        String data = getData(filePath, measure, accessToken);
        if (StringUtils.isNotBlank(data)) {
          addBytesToZip(filePath, data.getBytes(), zos);
        }
      }
    } catch (Exception ex) {
      log.error(ex.getMessage());
    }
  }

  /**
   * Adds the bytes to zip.
   *
   * @param path  file name along with path and extension
   * @param input the input byte array
   * @param zipOutputStream the zip
   * @throws Exception the exception
   */
  public void addBytesToZip(String path, byte[] input, ZipOutputStream zipOutputStream) throws IOException {
    ZipEntry entry = new ZipEntry(path);
    entry.setSize(input.length);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(input);
    zipOutputStream.closeEntry();
  }


  protected String getData(String filePath, Measure measure, String accessToken) {

    String measureBundleJson = bundleService.bundleMeasure(measure, accessToken);
    if (filePath.contains(".cql")) {
      return measure.getCql();
    } else if (filePath.contains(".json")) {
      // test data
      return measureBundleJson;
    }
    return null;
  }
}
