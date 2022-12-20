package cms.gov.madie.measure.services;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
public class MeasureBundleService {

  private List<String> bundleFiles = new ArrayList<String>();

  public void zipFile(String zipFileName, Measure measure) throws IOException {
    log.info("Entering of zipFile(): zipFileName = " + zipFileName);
    String fhirBundleJson = zipFileName + ".json";
    bundleFiles.add(fhirBundleJson);
    String cql = "\\cql\\" + zipFileName + ".cql";
    bundleFiles.add(cql);

    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName + ".zip"))) {

      for (String filePath : bundleFiles) {
        ZipEntry zipEntry = new ZipEntry(filePath);
        zos.putNextEntry(zipEntry);

        String data = getData(filePath, measure);
        if (StringUtils.isNotBlank(data)) {
          ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes());

          // play safe
          byte[] buffer = new byte[4 * 1024];
          int len;
          while ((len = bais.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
          }
        }
      }

      zos.closeEntry();
    } catch (Exception ex) {
      log.error(ex.getMessage());
    }
    log.info("end of zipFile");
  }

  protected String getData(String filePath, Measure measure) {
  	//test data for .json
    String data = "Test data \n123\n456\n789";
    if (filePath.contains(".cql")) {
      return measure.getCql();
    } else if (filePath.contains(".json")) {
      // test data
      return data;
    }
    return null;
  }
}
