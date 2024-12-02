package co.edu.ufps.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FacturaException extends RuntimeException {
    private final HttpStatus status;

    public FacturaException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}