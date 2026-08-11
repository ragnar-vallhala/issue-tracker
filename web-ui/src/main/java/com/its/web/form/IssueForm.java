package com.its.web.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

/** Issue create/edit form backing bean (FR-UI-13, FR-UI-14). */
public class IssueForm {

    private Integer issueId;

    @NotBlank(message = "Please enter a summary")
    @Size(max = 255, message = "Summary must be at most 255 characters")
    private String summary;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @NotNull(message = "Please choose a project")
    private Integer projectId;

    /** Optional: an issue can be raised before anyone is given it. */
    private Integer assigneeId;

    @NotBlank(message = "Please choose a status")
    private String status;

    @NotBlank(message = "Please choose a priority")
    private String priority;

    @NotBlank(message = "Please choose a type")
    private String type;

    @Min(value = 0, message = "Story points cannot be negative")
    private Integer storyPoints;

    @Size(max = 255, message = "Sprint must be at most 255 characters")
    private String sprint;

    /** Comma-separated, matching how the reference workbook stores them. */
    @Size(max = 255, message = "Tags must be at most 255 characters")
    private String tags;

    /**
     * The payload for a create call.
     *
     * @param createdBy the logged-in user, taken from the session rather than the form -
     *        authorship is not something a client gets to assert
     */
    public Map<String, Object> toCreatePayload(Integer createdBy) {
        Map<String, Object> body = commonFields();
        body.put("createdBy", createdBy);
        return body;
    }

    /**
     * The payload for an update call.
     *
     * <p>Sends every field, because a Project Owner's edit form shows every field. An
     * Assignee never reaches this form - their status change goes through the dedicated
     * status endpoint, which sends only the one field the service will accept from them
     * (FR-ISS-07).
     */
    public Map<String, Object> toUpdatePayload() {
        return commonFields();
    }

    private Map<String, Object> commonFields() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", summary);
        body.put("description", description);
        body.put("projectId", projectId);
        body.put("assigneeId", assigneeId);
        body.put("status", status);
        body.put("priority", priority);
        body.put("type", type);
        body.put("storyPoints", storyPoints);
        body.put("sprint", sprint);
        body.put("tags", tags);
        return body;
    }

    public Integer getIssueId() {
        return issueId;
    }

    public void setIssueId(Integer issueId) {
        this.issueId = issueId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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
}
