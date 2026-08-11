package com.its.project.controller;

import com.its.project.dto.request.ProjectRequest;
import com.its.project.dto.response.IssueSummary;
import com.its.project.dto.response.ProjectResponse;
import com.its.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Project Service endpoints (SRS section 9.2). */
@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Project lifecycle and ownership")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create a new project",
            description = "The owner must exist and must hold the PROJECT_OWNER role, "
                    + "which is verified against the User Service (FR-PRJ-02).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project created"),
            @ApiResponse(responseCode = "400", description = "Validation failed, or the "
                    + "owner does not exist or is not a Project Owner"),
            @ApiResponse(responseCode = "409", description = "Project name already in use"),
            @ApiResponse(responseCode = "503", description = "User Service unreachable")})
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse created = projectService.create(request);

        return ResponseEntity
                .created(URI.create("/api/projects/" + created.projectId()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "Retrieve a list of all projects")
    public ResponseEntity<List<ProjectResponse>> findAll() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Retrieve details of a specific project by project ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "No such project")})
    public ResponseEntity<ProjectResponse> findById(@PathVariable Integer projectId) {
        return ResponseEntity.ok(projectService.findById(projectId));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update details of a specific project by project ID")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable Integer projectId,
            @Valid @RequestBody ProjectRequest request) {

        return ResponseEntity.ok(projectService.update(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a specific project by project ID",
            description = """
                    CASCADES. Deleting a project deletes every issue within it and every
                    comment on those issues (FR-PRJ-09). The cascade runs children-first,
                    so if any step fails the project row is left intact and the whole
                    operation can safely be retried - see DESIGN 6.4.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Project and descendants deleted"),
            @ApiResponse(responseCode = "404", description = "No such project"),
            @ApiResponse(responseCode = "503", description = "A cascade step failed; nothing "
                    + "was deleted at the project level and the request may be retried")})
    public ResponseEntity<Void> delete(@PathVariable Integer projectId) {
        projectService.delete(projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Retrieve projects owned by a specific user by owner ID")
    public ResponseEntity<List<ProjectResponse>> findByOwner(@PathVariable Integer ownerId) {
        return ResponseEntity.ok(projectService.findByOwner(ownerId));
    }

    @GetMapping("/{projectId}/issues")
    @Operation(summary = "Retrieve issues within a specific project by project ID",
            description = "INTER-SERVICE COMMUNICATION - delegates to the Issue Service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issues in the project"),
            @ApiResponse(responseCode = "404", description = "No such project"),
            @ApiResponse(responseCode = "503", description = "Issue Service unreachable")})
    public ResponseEntity<List<IssueSummary>> issuesByProjectId(@PathVariable Integer projectId) {
        return ResponseEntity.ok(projectService.findIssuesByProjectId(projectId));
    }

    @GetMapping("/projectName/{projectName}/issues")
    @Operation(summary = "Retrieve issues within a specific project by project name",
            description = "INTER-SERVICE COMMUNICATION. Project names are unique, which is "
                    + "what lets a name resolve to exactly one project (FR-PRJ-14).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issues in the project"),
            @ApiResponse(responseCode = "404", description = "No project with that name"),
            @ApiResponse(responseCode = "503", description = "Issue Service unreachable")})
    public ResponseEntity<List<IssueSummary>> issuesByProjectName(
            @PathVariable String projectName) {

        return ResponseEntity.ok(projectService.findIssuesByProjectName(projectName));
    }
}
