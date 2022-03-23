package cms.gov.madie.measure.validations;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GroupScoringPopulationValidator.class)
@Documented
public @interface ValidGroupScoringPopulation {

  String message() default "Populations do not match Scoring";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
