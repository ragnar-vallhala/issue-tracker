package com.its.web.session;

import java.io.Serializable;

/**
 * The logged-in user, held in the server-side {@code HttpSession}.
 *
 * <p><strong>The JWT lives here and nowhere else</strong> (SRS A-14). The browser receives
 * only a {@code JSESSIONID} cookie; the token itself never reaches the client, so no
 * script on the page can read it and no XSS can exfiltrate it. The cost is that this tier
 * is stateful - acceptable, because it is the one component that is not expected to scale
 * horizontally (NFR-03).
 *
 * <p>Immutable: a session's identity is established at login and replaced wholesale at
 * the next one, never edited in place.
 */
public record SessionUser(
        Integer userId,
        String name,
        String role,
        String token) implements Serializable {

    public static final String SESSION_KEY = "its.sessionUser";

    public boolean isProjectOwner() {
        return "PROJECT_OWNER".equals(role);
    }

    public boolean isAssignee() {
        return "ASSIGNEE".equals(role);
    }

    /** Where this user belongs after login, and where a stray request is sent back to. */
    public String dashboardPath() {
        return isProjectOwner() ? "/owner/dashboard" : "/assignee/dashboard";
    }
}
