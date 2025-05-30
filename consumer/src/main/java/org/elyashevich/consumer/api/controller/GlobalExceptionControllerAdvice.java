package org.elyashevich.consumer.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.elyashevich.consumer.api.dto.exception.ExceptionBodyDto;
import org.elyashevich.consumer.exception.ResourceAlreadyExistException;
import org.elyashevich.consumer.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionControllerAdvice {

    private static final String NOT_SUPPORTED_MESSAGE = "Http method with this URL not found.";
    private static final String FAILED_VALIDATION_MESSAGE = "Validation failed.";
    private static final String UNEXPECTED_ERROR_MESSAGE = "Something went wrong.";
    private static final String NOT_FOUND_MESSAGE = "Resource was not found.";
    private static final String RESOURCE_ALREADY_EXISTS_MESSAGE = "Resource already exists.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionBodyDto> handleResourceNotFoundException(
            final ResourceNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(this.handleException(exception, NOT_FOUND_MESSAGE));
    }

    @ExceptionHandler(ResourceAlreadyExistException.class)
    public ResponseEntity<ExceptionBodyDto> handleResourceAlreadyExistsException(
            final ResourceAlreadyExistException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(this.handleException(exception, RESOURCE_ALREADY_EXISTS_MESSAGE));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ExceptionBodyDto> handleHttpRequestMethodNotSupportedException(
            final HttpRequestMethodNotSupportedException exception
    ) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(this.handleException(exception, NOT_SUPPORTED_MESSAGE));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ExceptionBodyDto> handleNoResourceFoundException(
            final NoResourceFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(this.handleException(exception, NOT_SUPPORTED_MESSAGE));
    }

    @SuppressWarnings("all")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionBodyDto> handleMethodArgumentNotValidException
            (final MethodArgumentNotValidException exception
            ) {
        var errors = exception.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                                FieldError::getField,
                                fieldError -> fieldError.getDefaultMessage(),
                                (exist, newMessage) -> exist + " " + newMessage + "."
                        )
                );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionBodyDto(FAILED_VALIDATION_MESSAGE, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionBodyDto> handleException(final Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(this.handleException(exception, UNEXPECTED_ERROR_MESSAGE));
    }

    private ExceptionBodyDto handleException(final Exception exception, final String defaultMessage) {
        var message = exception.getMessage() == null ? defaultMessage : exception.getMessage();
        log.warn("{} '{}'.", defaultMessage, message);
        return new ExceptionBodyDto(message);
    }
}
