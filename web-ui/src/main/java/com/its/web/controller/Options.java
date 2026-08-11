package com.its.web.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The enumerated values the forms offer.
 *
 * <p>Mirrors the Issue Service's enums (SRS FR-ISS-13). Only {@code TO_DO}, {@code HIGH}
 * and {@code BUG} are attested by the reference workbook; the rest are provisional
 * (SRS A-11), which is exactly why they are listed in one place rather than typed into
 * each JSP.
 *
 * <p>Insertion-ordered maps, so the dropdowns read in workflow order rather than
 * alphabetically.
 */
public final class Options {

    private Options() {
    }

    public static Map<String, String> statuses() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("TO_DO", "To Do");
        options.put("IN_PROGRESS", "In Progress");
        options.put("IN_REVIEW", "In Review");
        options.put("DONE", "Done");
        return options;
    }

    public static Map<String, String> priorities() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("LOW", "Low");
        options.put("MEDIUM", "Medium");
        options.put("HIGH", "High");
        options.put("CRITICAL", "Critical");
        return options;
    }

    public static Map<String, String> types() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("BUG", "Bug");
        options.put("TASK", "Task");
        options.put("STORY", "Story");
        options.put("EPIC", "Epic");
        return options;
    }

    /**
     * The statuses an issue may legally move to from its current one (FR-ISS-14).
     *
     * <p>Offering only the legal targets means an Assignee cannot pick a transition the
     * service will reject - the rule is visible in the control rather than discovered
     * through an error message.
     */
    public static Map<String, String> transitionsFrom(String current) {
        Map<String, String> all = statuses();

        if (!"DONE".equals(current)) {
            return all;
        }

        Map<String, String> allowed = new LinkedHashMap<>();
        allowed.put("DONE", "Done");
        allowed.put("TO_DO", "To Do (reopen)");
        return allowed;
    }

    public static List<String> roles() {
        return List.of("PROJECT_OWNER", "ASSIGNEE");
    }
}
