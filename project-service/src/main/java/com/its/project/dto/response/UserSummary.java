package com.its.project.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A user, as far as the Project Service needs to understand one.
 *
 * <p>{@code role} arrives as the enum <em>name</em> - the User Service confines the 0/1
 * encoding to its own persistence layer (SRS A-04), so this service never sees a digit
 * and cannot get the mapping wrong.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSummary(
        Integer userId,
        String name,
        String email,
        String username,
        String role) {

    public boolean isProjectOwner() {
        return "PROJECT_OWNER".equals(role);
    }
}
