package com.interview.urlshortener.api;

import com.interview.urlshortener.api.dto.ApiError;
import com.interview.urlshortener.domain.exception.InvalidUrlException;
import com.interview.urlshortener.domain.exception.ShortUrlConflictException;
import com.interview.urlshortener.domain.exception.ShortUrlGoneException;
import com.interview.urlshortener.domain.exception.ShortUrlNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            InvalidUrlException.class,
            IllegalArgumentException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(
            RuntimeException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()));

        return build(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                errors);
    }

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ShortUrlNotFoundException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(ShortUrlConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ShortUrlConflictException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(ShortUrlGoneException.class)
    public ResponseEntity<ApiError> handleGone(
            ShortUrlGoneException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.GONE,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getRequestURI(),
                Map.of());
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors) {

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                fieldErrors);

        return ResponseEntity.status(status).body(error);
    }
}