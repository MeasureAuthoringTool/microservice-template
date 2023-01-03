package cms.gov.madie.measure.utils;

import gov.cms.madie.models.measure.Measure;

public class ExportFileNamesUtil {
  public static String getExportZipName(Measure measure) {
    return measure.getEcqmTitle() + "-v" + measure.getVersion() + "-" + measure.getModel();
  }

  public static String getExportFileName(Measure measure) {
    return measure.getEcqmTitle() + "-v" + measure.getVersion() + "-" + measure.getModel();
  }
}
