package gov.cms.madie.measure.utilities.qicore411;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import cms.gov.madie.measure.utils.PackagingUtility;
import gov.cms.madie.models.measure.Export;

class PackagingUtilityImplTest {

  @Test
  void testGetZipBundle() {
    PackagingUtility utility = new PackagingUtilityImpl();
    Export export = new Export();
    export.setMeasureBundleJson(cms.gov.madie.measure.JsonBits.BUNDLE) ; 
        
    byte[] results = null;
    try {
      results = utility.getZipBundle(export, "file");
    } catch (Exception e) {
      fail(e);
    }

    assertNotNull(results);
  }
}
