package com.tangluck.common.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Clock clock;

    public GlobalExceptionHandler() {
        this(Clock.systemUTC());
    }

    GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        return ResponseEntity
                .status(statusFor(exception.getCode()))
                .body(new ApiError(
                        exception.getCode().name(),
                        exception.getMessage(),
                        traceId(),
                        exception.getDetails()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(new ApiError(
                        ErrorCode.VALIDATION_FAILED.name(),
                        "Request validation failed.",
                        traceId(),
                        details
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        ErrorCode.INTERNAL_ERROR.name(),
                        "Internal server error.",
                        traceId(),
                        Map.of()
                ));
    }

    private HttpStatus statusFor(ErrorCode code) {
        return switch (code) {
            case REGION_BLOCKED, AGE_NOT_ALLOWED, SC_POLICY_BLOCKED, PAYMENT_NOT_ALLOWED,
                    KYC_REQUIRED, REDEMPTION_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            case AUTH_INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case EMAIL_EXISTS, CLAIM_DUPLICATED, BUDGET_EXHAUSTED, IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case RISK_REVIEW_REQUIRED -> HttpStatus.ACCEPTED;
            case CONSENT_REQUIRED, CAMPAIGN_NOT_ACTIVE, LEGAL_APPROVAL_REQUIRED,
                    VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String traceId() {
        var date = DateTimeFormatter.BASIC_ISO_DATE.format(clock.instant().atZone(clock.getZone()));
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        return "trc_" + date + "_" + suffix;
    }
}
