package cms.gov.madie.measure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import gov.cms.madie.models.common.Version;

public class VersionConverterTest {

  @Test
  public void testUpdateMatMeasureVersion() {

    Version version = new VersionConverter().convert("0.001");

    assertEquals(0, version.getMajor());
    assertEquals(1, version.getMinor());
    assertEquals(0, version.getRevisionNumber());
  }

  @Test
  public void testUpdateMadieMeasureVersion() {

    Version version = new VersionConverter().convert("0.001.003");

    assertEquals(0, version.getMajor());
    assertEquals(1, version.getMinor());
    assertEquals(3, version.getRevisionNumber());
  }

  @Test
  public void testUpdateMeasureVersionNullInput() {

    Version version = new VersionConverter().convert(null);

    assertEquals(0, version.getMajor());
    assertEquals(0, version.getMinor());
    assertEquals(0, version.getRevisionNumber());
  }

  @Test
  public void testUpdateMeasureVersionNoSplit() {

    Version version = new VersionConverter().convert("1");

    assertEquals(0, version.getMajor());
    assertEquals(0, version.getMinor());
    assertEquals(0, version.getRevisionNumber());
  }

  @Test
  public void testUpdateMeasureVersionInvalidInput() {

    Version version = new VersionConverter().convert("a.b.ccc");

    assertEquals(0, version.getMajor());
    assertEquals(0, version.getMinor());
    assertEquals(0, version.getRevisionNumber());
  }
}
