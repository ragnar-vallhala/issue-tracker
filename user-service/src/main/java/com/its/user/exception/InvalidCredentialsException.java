package com.its.user.exception;

/**
 * Maps to 401. Login failed.
 *
 * <p>Carries a fixed, deliberately uninformative message. Distinguishing "no such email"
 * from "wrong password" turns the login endpoint into an account enumeration oracle
 * (FR-USR-05).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
