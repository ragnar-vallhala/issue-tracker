package com.its.issue.dto.request;

import com.its.issue.entity.IssueType;
import com.its.issue.entity.Priority;
import com.its.issue.entity.Status;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create an issue (FR-ISS-01).
 *
 * <p>{@code createdOn} and {@code lastUpdatedOn} are absent by design - both are set
 * server-side, and a client-supplied value would be ignored anyway (FR-ISS-03). Leaving
 * them off the contract is clearer than accepting and silently discarding them.
 */
public record IssueRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must be at most 255 characters")
        String summary,

        @Size(max = 255, message = "must be at most 255 characters")
        String description,

        @NotNull(message = "must be supplied")
        Integer projectId,

        /* Nullable: an issue may be raised before anyone is assigned to it. */
        Integer assigneeId,

        @NotNull(message = "must be supplied")
        Integer createdBy,

        @NotNull(message = "must be TO_DO, IN_PROGRESS, IN_REVIEW or DONE")
        Status status,

        @NotNull(message = "must be LOW, MEDIUM, HIGH or CRITICAL")
        Priority priority,

        @NotNull(message = "must be BUG, TASK, STORY or EPIC")
        IssueType type,

        @Min(value = 0, message = "must not be negative")
        Integer storyPoints,

        @Size(max = 255, message = "must be at most 255 characters")
        String sprint,

        @Size(max = 255, message = "must be at most 255 characters")
        String tags) {
}
