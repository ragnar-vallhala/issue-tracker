package com.its.user.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Profile update payload (FR-USR-06 addition, SRS A-06).
 *
 * <p>Null fields are left unchanged. Email is deliberately absent: it is the login
 * identifier and the basis of the derived username (SRS A-18), so changing it is an
 * account-migration concern rather than a profile edit. Password is absent for the same
 * reason - a password change needs the current password, which is a different endpoint.
 */
public record UpdateUserRequest(

        @Size(min = 2, max = 255, message = "must be between 2 and 255 characters")
        String name,

        @Size(max = 255, message = "must be at most 255 characters")
        String profile) {
}
