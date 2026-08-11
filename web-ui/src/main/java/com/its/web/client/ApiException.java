package com.its.web.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.http.HttpStatusCode;

/**
 * A non-2xx answer from the API, carrying the uniform error body the services return.
 *
 * <p>Transport failures - a refused connection, a timeout - are translated into this too,
 * as a synthetic 503. That way every controller and the error advice deal with one
 * exception type, and no controller needs a {@code try/catch} for {@code
 * ResourceAccessException}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatusCode status;
    private final ApiError error;

    public ApiException(HttpStatusCode status, ApiError error) {
        super(error == null ? "API call failed with " + status : error.message());
        this.status = status;
        this.error = error;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public ApiError getError() {
        return error;
    }

    public boolean isUnauthorized() {
        return status.value() == 401;
    }

    /** Field-level detail, empty when the failure was not a validation one. */
    public List<ApiError.FieldError> fieldErrors() {
        if (error == null || error.fieldErrors() == null) {
            return List.of();
        }
        return error.fieldErrors();
    }

    public String messageOrDefault(String fallback) {
        return error == null || error.message() == null ? fallback : error.message();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiError(
            String timestamp,
            Integer status,
            String error,
            String message,
            String path,
            List<FieldError> fieldErrors) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record FieldError(String field, Object rejectedValue, String reason) {
        }
    }
}
