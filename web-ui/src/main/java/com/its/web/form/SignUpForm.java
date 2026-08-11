package com.its.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sign-up form backing bean (FR-UI-06).
 *
 * <p>The constraints mirror the User Service's, but they do not replace them. Validation
 * here exists to give immediate feedback; the service validates independently because it
 * has to - this tier is a client, and a client's checks are advice, not enforcement.
 *
 * <p>A mutable class rather than a record: Spring's {@code form:} tags bind through
 * setters, and a rejected submission has to be re-rendered with the user's input intact.
 */
public class SignUpForm {

    @NotBlank(message = "Please enter your name")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Please enter your email address")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Please choose a password")
    @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
    private String password;

    /** A short description of the person - "Front-end developer" - not an image (SRS A-16). */
    @Size(max = 255, message = "Profile must be at most 255 characters")
    private String profile;

    @NotBlank(message = "Please choose a role")
    private String role;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
