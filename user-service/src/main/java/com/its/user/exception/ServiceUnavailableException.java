package com.its.user.exception;

/**
 * Maps to 503. A downstream service needed to answer this request is unreachable.
 *
 * <p>This exists so that an inter-service failure is never silently rendered as an empty
 * result: an empty list would be indistinguishable from "this user genuinely has no
 * issues", which is a materially different fact (SRS A-08, FR-USR-11).
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String service, Throwable cause) {
        super(service + " is currently unreachable, so this request cannot be completed", cause);
    }
}
