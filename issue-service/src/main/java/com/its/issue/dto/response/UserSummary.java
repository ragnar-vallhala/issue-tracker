package com.its.issue.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A user, as far as the Issue Service needs one - a local copy, see DESIGN section 3. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSummary(
        Integer userId,
        String name,
        String email,
        String username,
        String role) {

    public boolean isAssignee() {
        return "ASSIGNEE".equals(role);
    }
}
