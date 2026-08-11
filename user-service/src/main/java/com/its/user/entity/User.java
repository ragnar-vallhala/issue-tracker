package com.its.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A system user - either a Project Owner or an Assignee.
 *
 * <p>Column names follow the reference workbook rather than the PDF's ER diagram where
 * the two disagree (SRS A-15). This entity never leaves the service layer: controllers
 * return {@code UserResponse}, which has no password field at all. That is a structural
 * guarantee rather than a discipline - there is no path by which the hash can be
 * serialised to a client (FR-USR-03).
 */
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt hash. The workbook's plaintext sample values are illustrative only. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * A short free-text description of the person, e.g. "Front-end developer".
     *
     * <p>Not an image path. The case study prose says "profile image" throughout, but the
     * reference workbook's actual values are biographies, and the workbook wins (SRS A-16).
     */
    @Column(name = "profile", length = 255)
    private String profile;

    @Convert(converter = RoleConverter.class)
    @Column(name = "role", nullable = false)
    private Role role;

    protected User() {
        // Required by JPA.
    }

    public User(String name, String email, String password, String profile, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.profile = profile;
        this.role = role;
    }

    public Integer getUserId() {
        return userId;
    }

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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * The username is the local part of the email address (SRS A-18).
     *
     * <p>Neither source defines a username column, but the endpoint
     * {@code GET /api/users/username/{username}/issues} requires one. The workbook's
     * {@code sam.lee} matches the local part of every sample address, so it is derived
     * rather than stored - no new column, no second uniqueness rule to keep in step.
     */
    public String getUsername() {
        int at = email.indexOf('@');
        return at < 0 ? email : email.substring(0, at);
    }
}
