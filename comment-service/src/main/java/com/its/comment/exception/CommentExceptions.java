package com.its.comment.exception;

/** The exceptions this service raises, and the status codes they map to (SRS 9.5). */
public final class CommentExceptions {

    private CommentExceptions() {
    }

    /** 404 - the addressed comment does not exist. */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(Object id) {
            super("Comment not found: " + id);
        }
    }

    /**
     * 403 - the caller is not the comment's author (FR-CMT-03).
     *
     * <p>Not a 404. Hiding the comment's existence would be the stricter choice, but a
     * comment thread is visible to everyone who can see the issue, so its existence is
     * not a secret - only the right to change it is.
     */
    public static class ForbiddenOperationException extends RuntimeException {
        public ForbiddenOperationException(String message) {
            super(message);
        }
    }
}
