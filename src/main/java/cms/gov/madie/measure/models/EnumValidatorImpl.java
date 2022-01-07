package cms.gov.madie.measure.models;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EnumValidatorImpl implements ConstraintValidator<EnumValidator, String> {

  private List<String> acceptedValues;

  @Override
  public void initialize(EnumValidator annotation) {
    /*
    acceptedValues = new ArrayList<String>();
      for (String val : constraintAnnotation.acceptedValues()) {
        acceptedValues.add(val.toUpperCase());
      }
    */

    acceptedValues =
        Stream.of(annotation.enumClass().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toList());
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    List<String> enumValues = new ArrayList<String>();
    for (String v : acceptedValues) {
      enumValues.add(v.replace("_", "-").toLowerCase());
    }

    return enumValues.contains(value.toLowerCase());
  }
}
