package com.its.web.form;

import jakarta.validation.constraints.NotBlank;

/**
 * Login form backing bean (FR-UI-07).
 *
 * <p>Email and password only. The case study's login section also lists name, profile
 * image and role, but that is its sign-up list duplicated (SRS A-03) - and a role field
 * on a login form would be an invitation to claim one.
 */
public class LoginForm {

    @NotBlank(message = "Please enter your email address")
    private String email;

    @NotBlank(message = "Please enter your password")
    private String password;

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
}
