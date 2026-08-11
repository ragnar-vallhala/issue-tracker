package com.its.issue.dto.request;

import com.its.issue.entity.IssueType;
import com.its.issue.entity.Priority;
import com.its.issue.entity.Status;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Update an issue (FR-ISS-06). Every field is optional; null means "leave unchanged".
 *
 * <p>This shape is what makes FR-ISS-07 enforceable. An Assignee may change only the
 * status, and because absent fields are distinguishable from supplied ones, the service
 * can tell the difference between "did not mention priority" and "set priority to null" -
 * and reject the latter from a caller who is not permitted it.
 */
public record IssueUpdateRequest(

        @Size(max = 255, message = "must be at most 255 characters")
        String summary,

        @Size(max = 255, message = "must be at most 255 characters")
        String description,

        Integer projectId,

        Integer assigneeId,

        Status status,

        Priority priority,

        IssueType type,

        @Min(value = 0, message = "must not be negative")
        Integer storyPoints,

        @Size(max = 255, message = "must be at most 255 characters")
        String sprint,

        @Size(max = 255, message = "must be at most 255 characters")
        String tags) {

    /**
     * True if this request touches anything other than status.
     *
     * <p>Used to enforce FR-ISS-07: an Assignee submitting only a status change is
     * allowed; the same Assignee slipping a reassignment or a priority bump into the same
     * payload is not.
     */
    public boolean touchesFieldsBeyondStatus() {
        return summary != null || description != null || projectId != null
                || assigneeId != null || priority != null || type != null
                || storyPoints != null || sprint != null || tags != null;
    }
}
