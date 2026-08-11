package com.its.user.exception;

/** Maps to 409. A uniqueness rule would be violated - here, the email (FR-USR-02). */
public class DuplicateResourceException extends RuntimeException {

    private final String field;

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** The request field the conflict should be reported against, so the UI can highlight it. */
    public String getField() {
        return field;
    }
}
