package com.its.issue.exception;

/** The exceptions this service raises, and the status codes they map to (SRS 9.5). */
public final class IssueExceptions {

    private IssueExceptions() {
    }

    /** 404 - the addressed issue does not exist. */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }

        public static ResourceNotFoundException issue(Object id) {
            return new ResourceNotFoundException("Issue not found: " + id);
        }
    }

    /** 400 - the request references a project or user that does not exist. */
    public static class InvalidReferenceException extends RuntimeException {
        private final String field;

        public InvalidReferenceException(String field, String message) {
            super(message);
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }

    /**
     * 409 - the requested status change is not legal from the issue's current state
     * (FR-ISS-14).
     */
    public static class IllegalStateTransitionException extends RuntimeException {
        public IllegalStateTransitionException(String message) {
            super(message);
        }
    }

    /**
     * 403 - the caller is authenticated but not permitted this action.
     *
     * <p>Raised when an Assignee tries to change anything on an issue beyond its status
     * (FR-ISS-07). Distinct from a 401: the caller is known, they simply may not do this.
     */
    public static class ForbiddenOperationException extends RuntimeException {
        public ForbiddenOperationException(String message) {
            super(message);
        }
    }

    /** 503 - a service this request depends on is unreachable. */
    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String service, Throwable cause) {
            super(service + " is currently unreachable, so this request cannot be completed",
                    cause);
        }
    }
}
