package bg.deck.santaseservice.exception;

import bg.deck.santaseservice.constant.ExceptionConstants;
import bg.deck.santaseservice.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

import static bg.deck.santaseservice.constant.ExceptionConstants.COMMA_DELIMITER;
import static bg.deck.santaseservice.constant.ExceptionConstants.INCORRECT_CREDENTIALS_MESSAGE;
import static bg.deck.santaseservice.constant.ExceptionConstants.LOG_FORMAT_SECURITY;
import static bg.deck.santaseservice.constant.ExceptionConstants.VALIDATION_DETAILS_FORMAT;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn(String.format(LOG_FORMAT_SECURITY, ex.getMessage(), request.getRemoteAddr(), request.getRequestURI()));

        return buildResponse(HttpStatus.BAD_REQUEST, INCORRECT_CREDENTIALS_MESSAGE, request.getRequestURI());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        log.error(String.format(ExceptionConstants.LOG_FORMAT_SECURITY,
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI()));

        return buildResponse(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.getReasonPhrase(), request.getRequestURI());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest request) {
        log.warn(String.format(ExceptionConstants.LOG_FORMAT_SECURITY, ex.getMessage(), request.getRemoteAddr(), request.getRequestURI()));

        return buildResponse(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String validationDetails = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> String.format(VALIDATION_DETAILS_FORMAT, error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(COMMA_DELIMITER));

        log.error(String.format(ExceptionConstants.LOG_FORMAT_ERROR,
                ExceptionConstants.VALIDATION_ERROR_TITLE, request.getRequestURI(), validationDetails));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ExceptionConstants.VALIDATION_ERROR_TITLE,
                validationDetails,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error(String.format(ExceptionConstants.LOG_FORMAT_UNHANDLED, request.getRemoteAddr(), request.getRequestURI(),
                ex.getClass().getSimpleName(), request.getRequestURI()), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ExceptionConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                ex.getClass().getSimpleName(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, Object details, String path) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .details(details)
                        .path(path)
                        .build()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        return buildResponse(status, message, null, path);
    }
}
