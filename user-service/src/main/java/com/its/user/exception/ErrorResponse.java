package com.its.user.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * The uniform error body returned by every service (FR-SYS-05).
 *
 * <p>Identical in shape across all four services. It is duplicated rather than shared,
 * for the reason given in DESIGN section 3 - the cost of a shared module is worse than
 * the cost of this repetition.
 *
 * @param fieldErrors present only for validation failures; omitted from the JSON otherwise
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse validation(String path, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(
                Instant.now(), 400, "Bad Request", "Validation failed", path, fieldErrors);
    }

    /** One rejected field, so a form can show the message beside the input that caused it. */
    public record FieldErrorDetail(String field, Object rejectedValue, String reason) {
    }
}
