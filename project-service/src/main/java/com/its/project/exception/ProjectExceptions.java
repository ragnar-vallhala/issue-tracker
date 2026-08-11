package com.its.project.exception;

/**
 * The exceptions this service raises, grouped so the mapping to HTTP status codes can be
 * read in one place. Each is translated by {@link GlobalExceptionHandler} to the contract
 * in SRS 9.5.
 */
public final class ProjectExceptions {

    private ProjectExceptions() {
    }

    /** 404 - the addressed project does not exist. */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }

        public static ResourceNotFoundException project(Object id) {
            return new ResourceNotFoundException("Project not found: " + id);
        }
    }

    /** 409 - a uniqueness rule would be violated, here the project name. */
    public static class DuplicateResourceException extends RuntimeException {
        private final String field;

        public DuplicateResourceException(String field, String message) {
            super(message);
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }

    /**
     * 400 - the request points at something in another service that does not exist, or
     * does not qualify.
     *
     * <p>Distinct from {@link ResourceNotFoundException} on purpose: the project being
     * created is not missing, the <em>reference</em> inside it is wrong. That is a bad
     * request, not a missing resource, and conflating the two would have a client
     * retrying a 404 that will never succeed.
     */
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
     * 503 - a service this request depends on is unreachable.
     *
     * <p>Also raised when a step of the delete cascade fails, which is what guarantees
     * the project row survives a partial delete (FR-PRJ-11).
     */
    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String service, Throwable cause) {
            super(service + " is currently unreachable, so this request cannot be completed",
                    cause);
        }

        public ServiceUnavailableException(String message) {
            super(message);
        }
    }
}
