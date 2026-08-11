package com.its.issue.entity;

/**
 * Issue type. {@code BUG} is attested by the reference workbook; the rest complete the
 * set conventionally (SRS A-11). Persisted as a string.
 *
 * <p>Named {@code IssueType} rather than {@code Type} to avoid colliding with
 * {@link java.lang.reflect.Type}, which is imported often enough that the clash would be
 * a recurring nuisance.
 */
public enum IssueType {
    BUG,
    TASK,
    STORY,
    EPIC
}
