package com.its.issue.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.its.issue.entity.Issue;
import com.its.issue.entity.IssueType;
import com.its.issue.entity.Priority;
import com.its.issue.entity.Status;
import java.time.LocalDateTime;

/**
 * An issue as returned to clients.
 *
 * @param commentCount populated from the Comment Service on the single-issue endpoint
 *        only (FR-CMT-04). It is deliberately absent from list responses: fetching it per
 *        row would be one HTTP call per issue, which is the N+1 problem with a network
 *        hop attached.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IssueResponse(
        Integer issueId,
        String summary,
        String description,
        Integer projectId,
        Integer assigneeId,
        Integer createdBy,
        Status status,
        Priority priority,
        IssueType type,
        Integer storyPoints,
        String sprint,
        String tags,
        LocalDateTime createdOn,
        LocalDateTime lastUpdatedOn,
        Long commentCount) {

    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
                issue.getIssueId(),
                issue.getSummary(),
                issue.getDescription(),
                issue.getProjectId(),
                issue.getAssigneeId(),
                issue.getCreatedBy(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getType(),
                issue.getStoryPoints(),
                issue.getSprint(),
                issue.getTags(),
                issue.getCreatedOn(),
                issue.getLastUpdatedOn(),
                null);
    }

    public IssueResponse withCommentCount(Long count) {
        return new IssueResponse(issueId, summary, description, projectId, assigneeId,
                createdBy, status, priority, type, storyPoints, sprint, tags,
                createdOn, lastUpdatedOn, count);
    }
}
