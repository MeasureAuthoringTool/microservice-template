package cms.gov.madie.measure.services;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cms.gov.madie.measure.utils.ExportFileNamesUtil;
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

  public void generateExports(Measure measure, String accessToken, OutputStream outputStream) {
    String fileName = ExportFileNamesUtil.getExportFileName(measure);
    log.info("Generating exports for " + fileName);

    List<String> exportFiles = new ArrayList<>();
    exportFiles.add(fileName + ".json");
    exportFiles.add("/cql/" + measure.getCqlLibraryName() + ".cql");

    try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
      for (String filePath : exportFiles) {
        String data = getData(filePath, measure, accessToken);
        if (StringUtils.isNotBlank(data)) {
          addBytesToZip(filePath, data.getBytes(), zos);
        }
      }
    } catch (Exception ex) {
      log.error(ex.getMessage());
      throw new RuntimeException(
          "Unexpected error while generating exports for measureID: " + measure.getId());
    }
  }

  /**
   * Adds the bytes to zip.
   *
   * @param path file name along with path and extension
   * @param input the input byte array
   * @param zipOutputStream the zip
   * @throws Exception the exception
   */
  private void addBytesToZip(String path, byte[] input, ZipOutputStream zipOutputStream)
      throws IOException {
    ZipEntry entry = new ZipEntry(path);
    entry.setSize(input.length);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(input);
    zipOutputStream.closeEntry();
  }

  private String getData(String filePath, Measure measure, String accessToken) {
    if (filePath.contains(".cql")) {
      return measure.getCql();
    } else if (filePath.contains(".json")) {
      return bundleService.bundleMeasure(measure, accessToken);
    }
    return null;
  }
}
