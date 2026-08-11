package com.its.web.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * The shapes this tier deserialises from the API and hands to the JSPs.
 *
 * <p>Local copies of the services' response contracts, for the reason given in DESIGN
 * section 3 - and {@code ignoreUnknown} throughout, so a service adding a field never
 * breaks a page.
 */
public final class Views {

    private Views() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserView(
            Integer userId,
            String name,
            String email,
            String username,
            String profile,
            String role) {

        public boolean isProjectOwner() {
            return "PROJECT_OWNER".equals(role);
        }

        /** "Project Owner" / "Assignee" - the enum name is not something to show a person. */
        public String roleLabel() {
            return isProjectOwner() ? "Project Owner" : "Assignee";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SignUpResult(UserView user, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginResult(
            String token,
            Integer userId,
            String name,
            String role,
            Long expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectView(
            Integer projectId,
            String projectName,
            Integer projectOwnerId,
            LocalDate startDate,
            LocalDate endDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IssueView(
            Integer issueId,
            String summary,
            String description,
            Integer projectId,
            Integer assigneeId,
            Integer createdBy,
            String status,
            String priority,
            String type,
            Integer storyPoints,
            String sprint,
            String tags,
            LocalDateTime createdOn,
            LocalDateTime lastUpdatedOn,
            Long commentCount) {

        /** "IN_PROGRESS" is a database value, not a label. */
        public String statusLabel() {
            return humanise(status);
        }

        public String priorityLabel() {
            return humanise(priority);
        }

        public String typeLabel() {
            return humanise(type);
        }

        /** CSS class suffix, so the stylesheet can colour badges without string logic. */
        public String statusSlug() {
            return status == null ? "unknown" : status.toLowerCase().replace('_', '-');
        }

        public String prioritySlug() {
            return priority == null ? "unknown" : priority.toLowerCase();
        }

        public List<String> tagList() {
            if (tags == null || tags.isBlank()) {
                return List.of();
            }
            return java.util.Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .toList();
        }

        private static String humanise(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            String[] words = value.toLowerCase().split("_");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return result.toString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentView(
            Integer commentId,
            Integer issueId,
            Integer authorId,
            String body,
            LocalDateTime createdOn) {
    }

    /**
     * A dashboard's numbers (FR-UI-08, FR-UI-15).
     *
     * <p>Counted in this tier from data the API already returned, rather than from a
     * bespoke statistics endpoint - the figures are therefore always consistent with the
     * lists shown beside them.
     */
    public record DashboardSummary(
            int projectCount,
            int issueCount,
            Map<String, Long> byStatus,
            Map<String, Long> byPriority) {
    }
}
