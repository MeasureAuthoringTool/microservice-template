package cms.gov.madie.measure.config;

import org.springframework.data.convert.ReadingConverter;
import org.springframework.core.convert.converter.Converter;

import gov.cms.madie.models.common.Version;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ReadingConverter
public class VersionConverter implements Converter<String, Version> {

  @Override
  public Version convert(String source) {
    log.debug("Entering convert(): source = " + source);
    Version version = new Version();
    if (source != null) {
      String[] parts = source.split("\\.");

      if (parts != null && parts.length == 1) {
        version = Version.builder().major(0).minor(0).revisionNumber(0).build();
      } else if (parts.length > 1) {

        try {
          version =
              Version.builder()
                  .major(Integer.valueOf(parts[0]))
                  .minor(Integer.valueOf(parts[1]))
                  .revisionNumber(0)
                  .build();

          if (parts.length > 2) {
            version.setRevisionNumber(Integer.valueOf(parts[2]));
          }
        } catch (NumberFormatException nfe) {
          version = Version.builder().major(0).minor(0).revisionNumber(0).build();
        }
      }
    }
    log.debug("Exiting convert(): version = " + version.toString());
    return version;
  }
}
