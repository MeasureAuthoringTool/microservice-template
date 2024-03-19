package cms.gov.madie.measure.utils;

import gov.cms.madie.models.measure.Measure;

public class ExportFileNamesUtil {

  public static String getExportFileName(Measure measure) {
    if (measure.getModel().startsWith("QI-Core")) {
      return measure.getEcqmTitle() + "-v" + measure.getVersion() + "-FHIR";
    }
    return measure.getEcqmTitle() + "-v" + measure.getVersion() + "-" + measure.getModel();
  }

  public static String getTestCaseExportZipName(Measure measure) {
    return measure.getEcqmTitle() + "-v" + measure.getVersion().toString() + "-TestCases";
  }
}
