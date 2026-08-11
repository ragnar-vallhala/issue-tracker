package com.its.user.dto.request;

import com.its.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Sign-up payload (FR-USR-01).
 *
 * <p>{@code profile} is a short description of the person - "Front-end developer" - not
 * an image URL (SRS A-16).
 */
public record SignUpRequest(

        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 255, message = "must be between 2 and 255 characters")
        String name,

        @NotBlank(message = "must not be blank")
        @Email(message = "must be a well-formed email address")
        @Size(max = 255, message = "must be at most 255 characters")
        String email,

        @NotBlank(message = "must not be blank")
        @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
        String password,

        @Size(max = 255, message = "must be at most 255 characters")
        String profile,

        @NotNull(message = "must be PROJECT_OWNER or ASSIGNEE")
        Role role) {
}
