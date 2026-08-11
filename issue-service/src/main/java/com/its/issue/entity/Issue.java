package com.its.issue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * An issue within a project, assigned to a user.
 *
 * <p>Three columns - {@code project_id}, {@code assignee_id}, {@code created_by} - point
 * at rows in other services' databases. They are plain integers with no foreign keys
 * (SRS C-02); their validity is checked over HTTP when an issue is written (FR-ISS-02).
 *
 * <p>The indexes are not incidental. Each of the three inter-service retrieval endpoints
 * filters on one of these columns, so without them every project dashboard and every
 * assignee dashboard is a full table scan.
 *
 * <p>Column names follow the reference workbook (SRS A-15): {@code issue_id} not
 * {@code id}, {@code story_points} not {@code story_point}, {@code last_updated_on} not
 * {@code last_updated}.
 */
@Entity
@Table(name = "issue", indexes = {
        @Index(name = "idx_issue_project", columnList = "project_id"),
        @Index(name = "idx_issue_assignee", columnList = "assignee_id"),
        @Index(name = "idx_issue_created_by", columnList = "created_by")
})
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Integer issueId;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    /** Nullable: an issue can exist before anyone has been given it. */
    @Column(name = "assignee_id")
    private Integer assigneeId;

    /**
     * The user who raised the issue.
     *
     * <p>An integer user id, consistent with {@code assigneeId} and with the
     * {@code /api/issues/owner/{ownerId}} contract - even though the reference workbook's
     * two sample rows put the string {@code sam.lee} here. That sample is internally
     * inconsistent: no such user exists in the workbook's own User table, and the sibling
     * column in the very same rows is numeric. See SRS A-17.
     */
    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    // Enums are mapped to their names. EnumType.ORDINAL would store the position, which
    // reassigns the meaning of every stored row the moment a constant is inserted in the
    // middle - a live risk while the value sets remain provisional (SRS A-11).
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private IssueType type;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "sprint", length = 255)
    private String sprint;

    /** Comma-separated, as the workbook stores them: {@code profile,cache,update}. */
    @Column(name = "tags", length = 255)
    private String tags;

    /**
     * Set by Hibernate at insert and never accepted from a client (FR-ISS-03).
     *
     * <p>DATETIME rather than the DATE both sources specify: two issues raised on the
     * same day are otherwise unorderable, which breaks every "most recently updated"
     * listing in the UI (SRS A-12).
     */
    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @UpdateTimestamp
    @Column(name = "last_updated_on", nullable = false)
    private LocalDateTime lastUpdatedOn;

    protected Issue() {
        // Required by JPA.
    }

    public Issue(String summary, String description, Integer projectId, Integer assigneeId,
                 Integer createdBy, Status status, Priority priority, IssueType type,
                 Integer storyPoints, String sprint, String tags) {
        this.summary = summary;
        this.description = description;
        this.projectId = projectId;
        this.assigneeId = assigneeId;
        this.createdBy = createdBy;
        this.status = status;
        this.priority = priority;
        this.type = type;
        this.storyPoints = storyPoints;
        this.sprint = sprint;
        this.tags = tags;
    }

    public Integer getIssueId() {
        return issueId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        this.type = type;
    }

    public Integer getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(Integer storyPoints) {
        this.storyPoints = storyPoints;
    }

    public String getSprint() {
        return sprint;
    }

    public void setSprint(String sprint) {
        this.sprint = sprint;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public LocalDateTime getLastUpdatedOn() {
        return lastUpdatedOn;
    }
}
