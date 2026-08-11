package com.its.user.entity;

/**
 * User role, as stored in {@code user.role TINYINT}.
 *
 * <p><strong>The encoding is 0 = PROJECT_OWNER, 1 = ASSIGNEE.</strong> This is not
 * arbitrary and it is not the intuitive ordering - it comes from the reference workbook
 * {@code DB-reference_ITS_v0.1.xlsx}, where the two project owners (Emily Sinha, Priya
 * Jackson) carry role 0 and the two engineers (Michael Patel, Carlos Singh) carry role 1.
 * It is corroborated twice over: every {@code project_owner_id} in the sample Project
 * table resolves to a role-0 user, and every {@code assignee_id} in the sample Issue
 * table resolves to a role-1 user. See SRS assumption A-04.
 *
 * <p>Getting this backwards inverts the entire permission model while still passing a
 * naive smoke test, so the integer appears in exactly two places in this codebase: here,
 * and in {@link RoleConverter}. Everything above the persistence boundary - the API, the
 * JWT claim, every authorisation check - uses this enum and never the digit.
 */
public enum Role {

    /** Creates and manages projects and issues; assigns work. Stored as 0. */
    PROJECT_OWNER(0),

    /** Works issues assigned to them; may update only their status. Stored as 1. */
    ASSIGNEE(1);

    private final int code;

    Role(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * Resolves the stored TINYINT back to a role.
     *
     * @throws IllegalArgumentException if the database holds a value outside {0, 1},
     *         which would mean the column has been written by something that does not
     *         share this mapping - worth failing loudly rather than defaulting.
     */
    public static Role fromCode(int code) {
        for (Role role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        throw new IllegalArgumentException(
                "Unknown role code: " + code + " (expected 0 = PROJECT_OWNER or 1 = ASSIGNEE)");
    }
}
