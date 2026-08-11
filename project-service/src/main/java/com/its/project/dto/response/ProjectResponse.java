package com.its.project.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.its.project.entity.Project;
import java.time.LocalDate;

/**
 * A project as returned to clients.
 *
 * @param ownerName resolved from the User Service where the caller asked for it, so the
 *        UI can show a name instead of an id without making its own second call. Null
 *        when not requested - hence NON_NULL, to keep it out of the JSON entirely rather
 *        than shipping a misleading null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectResponse(
        Integer projectId,
        String projectName,
        Integer projectOwnerId,
        String ownerName,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getProjectId(),
                project.getProjectName(),
                project.getProjectOwnerId(),
                null,
                project.getStartDate(),
                project.getEndDate());
    }

    public ProjectResponse withOwnerName(String name) {
        return new ProjectResponse(
                projectId, projectName, projectOwnerId, name, startDate, endDate);
    }
}
