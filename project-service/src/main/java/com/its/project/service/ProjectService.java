package com.its.project.service;

import com.its.project.dto.request.ProjectRequest;
import com.its.project.dto.response.IssueSummary;
import com.its.project.dto.response.ProjectResponse;
import java.util.List;

/** Business operations for project management (SRS section 4). */
public interface ProjectService {

    ProjectResponse create(ProjectRequest request);

    List<ProjectResponse> findAll();

    ProjectResponse findById(Integer projectId);

    List<ProjectResponse> findByOwner(Integer ownerId);

    ProjectResponse update(Integer projectId, ProjectRequest request);

    /** Deletes the project and cascades to its issues and their comments (FR-PRJ-09). */
    void delete(Integer projectId);

    /** Inter-service: issues within a project (FR-PRJ-13). */
    List<IssueSummary> findIssuesByProjectId(Integer projectId);

    /** Inter-service: issues within a project, addressed by name (FR-PRJ-14). */
    List<IssueSummary> findIssuesByProjectName(String projectName);
}
