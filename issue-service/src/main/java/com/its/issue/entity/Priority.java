package com.its.issue.entity;

/**
 * Issue priority. {@code HIGH} is attested by the reference workbook; the rest complete
 * the set conventionally (SRS A-11). Persisted as a string.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
