package cms.gov.madie.measure.resources;

import java.util.HashMap;
import java.util.Map;

import javax.validation.ConstraintViolationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@ControllerAdvice
public class ErrorHandlingControllerAdvice {

  @Autowired private final ErrorAttributes errorAttributes;

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  Map<String, Object> onConstraintValidationException(
      ConstraintViolationException ex, WebRequest request) {
    // Collect simplified validation errors
    Map<String, String> validationErrors = new HashMap<>();
    ex.getConstraintViolations()
        .forEach(
            (error) -> {
              validationErrors.put(error.getPropertyPath().toString(), error.getMessage());
            });
    Map<String, Object> errorAttributes = getErrorAttributes(request);
    errorAttributes.put("validationErrors", validationErrors);
    return errorAttributes;
  }

  @ExceptionHandler(DuplicateKeyException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  Map<String, Object> onDuplicateKeyExceptionException(
      DuplicateKeyException ex, WebRequest request) {
    Map<String, Object> errorAttributes = getErrorAttributes(request);
    errorAttributes.put("duplicateKey", ex.getKey());
    return errorAttributes;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  Map<String, Object> onMethodArgumentNotValidException(
      MethodArgumentNotValidException ex, WebRequest request) {
    // Collect simplified validation errors
    Map<String, String> validationErrors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              validationErrors.put(fieldName, errorMessage);
            });
    Map<String, Object> errorAttributes = getErrorAttributes(request);
    errorAttributes.put("validationErrors", validationErrors);
    return errorAttributes;
  }

  private Map<String, Object> getErrorAttributes(WebRequest request) {
    // BINDING_ERRORS and STACK_TRACE are too detailed and confusing to parse
    // Let's just add a list of simplified validation errors
    ErrorAttributeOptions errorOptions =
        ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE);
    Map<String, Object> errorAttributes =
        this.errorAttributes.getErrorAttributes(request, errorOptions);
    errorAttributes.put("status", HttpStatus.BAD_REQUEST.value());
    errorAttributes.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    return errorAttributes;
  }
}
