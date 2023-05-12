package cms.gov.madie.measure.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cms.gov.madie.measure.exceptions.InternalServerException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZipUtility {
  public byte[] zipEntries(Map<String, byte[]> entries) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);

    try {
      for (Entry<String, byte[]> reporte : entries.entrySet()) {
        ZipEntry entry = new ZipEntry(reporte.getKey());
        entry.setSize(reporte.getValue().length);
        zos.putNextEntry(entry);
        zos.write(reporte.getValue());
      }
      zos.closeEntry();

    } catch (IOException e) {
      log.error("Failure to create Zip File", e);
      throw new InternalServerException(e.getMessage());
    } finally {
      try {
        zos.close();
      } catch (IOException e) {
        log.error("Failure to create Zip File", e);
        throw new InternalServerException(e.getMessage());
      }
    }
    return baos.toByteArray();
  }
}
