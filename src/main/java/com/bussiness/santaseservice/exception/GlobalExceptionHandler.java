package com.bussiness.santaseservice.exception;

import com.bussiness.santaseservice.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception", ex);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                ex.getClass().getSimpleName(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            Object details,
            String path
    ) {
        return ResponseEntity
                .status(status)
                .body(
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
}
