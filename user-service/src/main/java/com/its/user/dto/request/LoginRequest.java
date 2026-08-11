package com.its.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Login payload (FR-USR-05).
 *
 * <p>Email and password only. The case study's login section also lists name, profile
 * image and role, but that is the sign-up field list duplicated by a copy-paste in the
 * source document (SRS A-03). Role is something login <em>returns</em>, never something
 * the caller asserts - accepting a claimed role here would let anyone elect themselves
 * a Project Owner.
 */
public record LoginRequest(

        @NotBlank(message = "must not be blank")
        String email,

        @NotBlank(message = "must not be blank")
        String password) {
}
