package com.its.user.dto.response;

import com.its.user.entity.Role;
import com.its.user.entity.User;

/**
 * A user as returned to clients.
 *
 * <p>There is no password field. Not "the password is nulled out before sending" - the
 * field does not exist on this type, so no future change to a mapper or a serialiser can
 * reintroduce it (FR-USR-03).
 *
 * <p>{@code role} is the enum name, never the stored digit: the 0/1 encoding is confined
 * to the persistence layer (SRS A-04).
 */
public record UserResponse(
        Integer userId,
        String name,
        String email,
        String username,
        String profile,
        Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getProfile(),
                user.getRole());
    }
}
