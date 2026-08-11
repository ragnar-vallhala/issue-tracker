package com.its.issue.entity;

import java.util.Set;

/**
 * Issue workflow state.
 *
 * <p>{@code TO_DO} is taken directly from the reference workbook, which also fixes the
 * casing convention as SCREAMING_SNAKE_CASE. The remaining three members complete the
 * set along conventional lines and are <em>not</em> attested by either source (SRS A-11)
 * - if the workbook is ever superseded, these are the values to revisit.
 *
 * <p>Persisted as a string, never an ordinal, precisely because the set is provisional:
 * inserting a member in the middle of an ordinal-mapped enum silently reinterprets every
 * existing row.
 */
public enum Status {

    TO_DO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE;

    /**
     * States reachable from this one (FR-ISS-14).
     *
     * <p>{@code DONE} is the only constrained state: work that has been completed cannot
     * drift back into review or progress by accident. Reopening is still possible, but
     * only as a deliberate return to {@code TO_DO}, which makes the intent legible in the
     * issue's history rather than looking like a routine status change.
     */
    public Set<Status> allowedTransitions() {
        if (this == DONE) {
            return Set.of(DONE, TO_DO);
        }
        return Set.of(values());
    }

    public boolean canTransitionTo(Status target) {
        return allowedTransitions().contains(target);
    }
}
