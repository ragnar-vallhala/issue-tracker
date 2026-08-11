package com.its.issue.controller;

import com.its.issue.dto.request.IssueRequest;
import com.its.issue.dto.request.IssueUpdateRequest;
import com.its.issue.dto.response.IssueResponse;
import com.its.issue.service.CallerIdentity;
import com.its.issue.service.IssueService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Issue Service endpoints (SRS section 9.3). */
@RestController
@RequestMapping("/api/issues")
@Tag(name = "Issues", description = "Issue lifecycle, assignment and workflow")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    @Operation(summary = "Create a new issue",
            description = "The project, assignee and creator are all verified against their "
                    + "owning services before the issue is written (FR-ISS-02).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Issue created"),
            @ApiResponse(responseCode = "400", description = "Validation failed, or a "
                    + "referenced project or user does not exist"),
            @ApiResponse(responseCode = "503", description = "A referenced service is "
                    + "unreachable, so the references could not be checked")})
    public ResponseEntity<IssueResponse> create(@Valid @RequestBody IssueRequest request) {
        IssueResponse created = issueService.create(request);

        return ResponseEntity
                .created(URI.create("/api/issues/" + created.issueId()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "Retrieve a list of all issues")
    public ResponseEntity<List<IssueResponse>> findAll() {
        return ResponseEntity.ok(issueService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve details of a specific issue by issue ID",
            description = "Carries commentCount when the Comment Service answers. If it "
                    + "does not, the field is omitted rather than reported as zero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "No such issue")})
    public ResponseEntity<IssueResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(issueService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update details of a specific issue by issue ID",
            description = """
                    Partial update - omitted fields are left unchanged.

                    A caller whose X-User-Role is ASSIGNEE may change only the status, and
                    only on an issue assigned to them (FR-ISS-07). Moving an issue out of
                    DONE is refused with 409; it must be reopened to TO_DO (FR-ISS-14).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "403", description = "An Assignee attempted more "
                    + "than a status change, or touched an issue that is not theirs"),
            @ApiResponse(responseCode = "404", description = "No such issue"),
            @ApiResponse(responseCode = "409", description = "Illegal status transition")})
    public ResponseEntity<IssueResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody IssueUpdateRequest request,
            // Injected by the gateway after it verifies the JWT (DESIGN section 9).
            // Absent when the service is called directly, e.g. Postman in development.
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {

        return ResponseEntity.ok(
                issueService.update(id, request, CallerIdentity.of(callerId, callerRole)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an issue and its comments",
            description = "Comments are deleted first; if that fails the issue is left "
                    + "intact and the call can be retried.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Issue and comments deleted"),
            @ApiResponse(responseCode = "404", description = "No such issue"),
            @ApiResponse(responseCode = "503", description = "Comment Service unreachable; "
                    + "nothing was deleted")})
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        issueService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Retrieve issues within a specific project by project ID",
            description = "INTER-SERVICE COMMUNICATION - this is the endpoint the Project "
                    + "Service delegates to.")
    public ResponseEntity<List<IssueResponse>> findByProject(@PathVariable Integer projectId) {
        return ResponseEntity.ok(issueService.findByProject(projectId));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Retrieve issues owned by a specific user by owner ID",
            description = "INTER-SERVICE COMMUNICATION. Matches on created_by - the user "
                    + "who raised the issue, as distinct from the one assigned to it.")
    public ResponseEntity<List<IssueResponse>> findByOwner(@PathVariable Integer ownerId) {
        return ResponseEntity.ok(issueService.findByCreator(ownerId));
    }

    @GetMapping("/assignee/{assigneeId}")
    @Operation(summary = "Retrieve issues assigned to a specific user by assignee ID",
            description = "INTER-SERVICE COMMUNICATION - this is the endpoint the User "
                    + "Service delegates to for both of its issue-bearing routes.")
    public ResponseEntity<List<IssueResponse>> findByAssignee(@PathVariable Integer assigneeId) {
        return ResponseEntity.ok(issueService.findByAssignee(assigneeId));
    }

    @DeleteMapping("/project/{projectId}")
    @Operation(summary = "Delete every issue in a project and their comments",
            description = """
                    INTER-SERVICE COMMUNICATION - the middle step of the project delete
                    cascade (FR-ISS-09), called by the Project Service.

                    Idempotent, and a no-op for a project with no issues, which is what lets
                    the Project Service call it unconditionally and retry it safely.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Issues and comments deleted"),
            @ApiResponse(responseCode = "503", description = "Comment Service unreachable; "
                    + "no issues were deleted and the call may be retried")})
    public ResponseEntity<Void> deleteByProject(@PathVariable Integer projectId) {
        issueService.deleteByProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
