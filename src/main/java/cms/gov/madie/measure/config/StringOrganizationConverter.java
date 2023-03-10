package cms.gov.madie.measure.config;

import gov.cms.madie.models.common.Organization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@Slf4j
@ReadingConverter
public class StringOrganizationConverter implements Converter<String, Organization> {

  @Override
  public Organization convert(String source) {
    return source == null ? null : Organization.builder().name(source).build();
  }
}
