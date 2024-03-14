package cms.gov.madie.measure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import gov.cms.madie.models.common.Organization;

class StringOrganizationConverterTest {

  @Test
  void testNull() {
    StringOrganizationConverter converter = new StringOrganizationConverter();
    Organization org = converter.convert(null);
    assertNull(org);
  }

  @Test
  void testValidOrganization() {
    StringOrganizationConverter converter = new StringOrganizationConverter();
    Organization org = converter.convert("org1");
    assertNotNull(org);
    assertEquals("org1", org.getName());
  }
}
