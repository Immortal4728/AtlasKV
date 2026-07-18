package com.atlaskv.server.api;

import com.atlaskv.server.config.ConfigValidationException;
import com.atlaskv.server.lifecycle.NodeLifecycleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Global exception handler using RFC 7807 Problem Details format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles write operations sent to non-leader nodes.
     *
     * @param ex the not-leader exception
     * @return RFC 7807 problem detail
     */
    @ExceptionHandler(NotLeaderException.class)
    public ProblemDetail handleNotLeader(NotLeaderException ex) {
        LOG.warn("Write rejected — not leader: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        detail.setTitle("Not Leader");
        detail.setType(URI.create("https://atlaskv.dev/errors/not-leader"));
        if (ex.getLeaderId() != null) {
            detail.setProperty("leaderId", ex.getLeaderId());
        }
        if (ex.getLeaderAddress() != null) {
            detail.setProperty("leaderAddress", ex.getLeaderAddress());
        }
        return detail;
    }

    /**
     * Handles command timeout exceptions.
     *
     * @param ex the timeout exception
     * @return RFC 7807 problem detail
     */
    @ExceptionHandler(CommandTimeoutException.class)
    public ProblemDetail handleCommandTimeout(CommandTimeoutException ex) {
        LOG.warn("Command timed out: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
        detail.setTitle("Command Timeout");
        detail.setType(URI.create("https://atlaskv.dev/errors/command-timeout"));
        return detail;
    }

    /**
     * Handles validation errors from request body validation.
     *
     * @param ex the validation exception
     * @return RFC 7807 problem detail
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                errors.append(err.getField()).append(": ")
                        .append(err.getDefaultMessage()).append("; "));

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, errors.toString().trim());
        detail.setTitle("Validation Failed");
        detail.setType(URI.create("https://atlaskv.dev/errors/validation"));
        return detail;
    }

    /**
     * Handles configuration validation failures.
     *
     * @param ex the config validation exception
     * @return RFC 7807 problem detail
     */
    @ExceptionHandler(ConfigValidationException.class)
    public ProblemDetail handleConfigValidation(ConfigValidationException ex) {
        LOG.error("Configuration validation failed: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        detail.setTitle("Configuration Error");
        detail.setType(URI.create("https://atlaskv.dev/errors/config"));
        return detail;
    }

    /**
     * Handles node lifecycle exceptions.
     *
     * @param ex the lifecycle exception
     * @return RFC 7807 problem detail
     */
    @ExceptionHandler(NodeLifecycleException.class)
    public ProblemDetail handleLifecycleException(NodeLifecycleException ex) {
        LOG.error("Lifecycle error: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        detail.setTitle("Node Lifecycle Error");
        detail.setType(URI.create("https://atlaskv.dev/errors/lifecycle"));
        return detail;
    }

    /**
     * Handles illegal argument exceptions (e.g., invalid keys).
     *
     * @param ex the exception
     * @return RFC 7807 problem detail
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Invalid Argument");
        detail.setType(URI.create("https://atlaskv.dev/errors/invalid-argument"));
        return detail;
    }

    /**
     * Catch-all handler for unexpected exceptions.
     *
     * @param ex the exception
     * @return RFC 7807 problem detail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        LOG.error("Unexpected error", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage());
        detail.setTitle("Internal Server Error");
        detail.setType(URI.create("https://atlaskv.dev/errors/internal"));
        return detail;
    }
}
