package cms.gov.madie.measure.poc.dto;

import cms.gov.madie.measure.poc.dto.model.Measure;
import java.security.Principal;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DtoController {

  private final Translator translator;

  @PostMapping("/dto")
  public ResponseEntity<Measure> addMeasure(
      @RequestBody MeasureDTO measure, @RequestHeader("Authorization") String accessToken) {

    Measure result = translator.translate(measure);
    validate(result);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  private void validate(Measure measure) {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<Measure>> violations = validator.validate(measure);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
