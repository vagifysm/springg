package com.example.app.exception;

import com.example.app.dto.response.ApiError;
import com.example.app.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Stream;

/**
 * Converts every exception into the standard {@link ApiResponse} envelope.
 * Extending ResponseEntityExceptionHandler also covers Spring MVC's own exceptions
 * (malformed JSON, wrong method, missing params, unknown route, ...).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---- Our own exceptions ----------------------------------------------------------------

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    // ---- Security --------------------------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: insufficient permissions"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed"));
    }

    // ---- Validation / bad input ------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiError> errors = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(fe -> new ApiError(fe.getField(), fe.getDefaultMessage())),
                        ex.getBindingResult().getGlobalErrors().stream()
                                .map(ge -> new ApiError(ge.getObjectName(), ge.getDefaultMessage())))
                .toList();
        return ResponseEntity.status(status).headers(headers)
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ApiError> errors = ex.getConstraintViolations().stream()
                .map(v -> new ApiError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    // ---- Persistence -----------------------------------------------------------------------

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidSort(PropertyReferenceException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid sort or filter property: " + ex.getPropertyName()));
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex) {
        if (ex.getCause() instanceof PropertyReferenceException pre) {
            return handleInvalidSort(pre);
        }
        return handleUnexpected(ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("The request conflicts with existing data"));
    }

    // ---- Spring MVC's own exceptions (404 route, 405, 415, malformed JSON, ...) ------------

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message;
        if (body instanceof ProblemDetail pd && pd.getDetail() != null) {
            message = pd.getDetail();
        } else if (ex instanceof ErrorResponse er && er.getBody().getDetail() != null) {
            message = er.getBody().getDetail();
        } else {
            HttpStatus resolved = HttpStatus.resolve(statusCode.value());
            message = resolved != null ? resolved.getReasonPhrase() : "Request failed";
        }
        if (statusCode.is5xxServerError()) {
            log.error("Server error handling request", ex);
        }
        return ResponseEntity.status(statusCode).headers(headers).body(ApiResponse.error(message));
    }

    // ---- Safety net ------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
