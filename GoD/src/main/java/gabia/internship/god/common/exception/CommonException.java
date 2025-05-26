package gabia.internship.god.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CommonException extends RuntimeException {

    private final HttpStatus status;

    public CommonException(String message, Throwable e, HttpStatus status) {
        super(message, e);
        this.status = status;
    }

}
