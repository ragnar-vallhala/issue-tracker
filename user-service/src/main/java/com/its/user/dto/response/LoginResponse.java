package com.its.user.dto.response;

import com.its.user.entity.Role;

/**
 * Successful login (FR-USR-05).
 *
 * <p>{@code role} is returned so the caller can route to the correct dashboard
 * (FR-USR-06). The web tier keeps the token in its server-side session and never hands
 * it to the browser (SRS A-14).
 *
 * @param expiresIn token lifetime in seconds
 */
public record LoginResponse(
        String token,
        Integer userId,
        String name,
        Role role,
        long expiresIn) {
}
