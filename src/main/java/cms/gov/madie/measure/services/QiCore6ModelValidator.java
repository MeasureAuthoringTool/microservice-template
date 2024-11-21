package cms.gov.madie.measure.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service(ServiceConstants.QICORE_6_VALIDATOR)
public class QiCore6ModelValidator extends QiCoreModelValidator {
  // Intentionally empty class. QI-Core 6 validation is the same as QI-Core 4.1.1.
}
