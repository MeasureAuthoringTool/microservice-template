package gov.cms.madie.measure.utilities.qicore411;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import cms.gov.madie.measure.exceptions.InternalServerException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class ResourceUtils {
  public static String getData(String resource) {
    try (InputStream inputStream = ResourceUtils.class.getResourceAsStream(resource)) {
      if (inputStream == null) {
        throw new InternalServerException("Unable to fetch resource " + resource);
      }
      return new String(inputStream.readAllBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NullPointerException nullPointerException) {
      throw new InternalServerException("Resource name cannot be null");
    }
  }

  /**
   * @param bundleResource Bundle resource
   * @return r4 resource
   */
  public static Resource getResource(Bundle bundleResource, String resourceType) {
    if (bundleResource == null || resourceType == null) {
      return null;
    }
    var measureEntry =
        bundleResource.getEntry().stream()
            .filter(
                entry ->
                    StringUtils.equalsIgnoreCase(
                        resourceType, entry.getResource().getResourceType().toString()))
            .findFirst();
    return measureEntry.map(Bundle.BundleEntryComponent::getResource).orElse(null);
  }
}
