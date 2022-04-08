package cms.gov.madie.measure.resources;

import lombok.Getter;

@Getter
public class DataIntegrityViolationException extends org.springframework.dao.DataIntegrityViolationException {
    private final String username;
    public DataIntegrityViolationException(String message, String username) {
        super(message);
        this.username = username;
    }
}
