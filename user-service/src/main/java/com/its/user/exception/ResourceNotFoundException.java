package com.its.user.exception;

/** Maps to 404. The addressed resource does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException user(Object identifier) {
        return new ResourceNotFoundException("User not found: " + identifier);
    }
}
