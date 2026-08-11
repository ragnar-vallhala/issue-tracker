package com.its.comment.controller;

import com.its.comment.dto.CommentDtos.CommentResponse;
import com.its.comment.dto.CommentDtos.CreateCommentRequest;
import com.its.comment.dto.CommentDtos.UpdateCommentRequest;
import com.its.comment.service.CommentService;
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

/** Comment Service endpoints (SRS section 9.4). */
@RestController
@RequestMapping("/api/comments")
@Tag(name = "Comments", description = "Comment threads on issues")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @Operation(summary = "Create a comment on an issue")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")})
    public ResponseEntity<CommentResponse> create(
            @Valid @RequestBody CreateCommentRequest request) {

        CommentResponse created = commentService.create(request);

        return ResponseEntity
                .created(URI.create("/api/comments/" + created.commentId()))
                .body(created);
    }

    @GetMapping("/issue/{issueId}")
    @Operation(summary = "Retrieve the comments on an issue, newest first")
    public ResponseEntity<List<CommentResponse>> findByIssue(@PathVariable Integer issueId) {
        return ResponseEntity.ok(commentService.findByIssue(issueId));
    }

    @GetMapping("/issue/{issueId}/count")
    @Operation(summary = "Count the comments on an issue",
            description = "INTER-SERVICE COMMUNICATION - the Issue Service calls this to "
                    + "decorate an issue detail response (FR-CMT-04).")
    public ResponseEntity<Long> countByIssue(@PathVariable Integer issueId) {
        return ResponseEntity.ok(commentService.countByIssue(issueId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update your own comment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "403", description = "Not the author"),
            @ApiResponse(responseCode = "404", description = "No such comment")})
    public ResponseEntity<CommentResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCommentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId) {

        return ResponseEntity.ok(commentService.update(id, request, callerId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete your own comment")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Not the author"),
            @ApiResponse(responseCode = "404", description = "No such comment")})
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId) {

        commentService.delete(id, callerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issue/{issueId}")
    @Operation(summary = "Delete every comment on an issue",
            description = """
                    INTER-SERVICE COMMUNICATION - the deepest step of the delete cascade
                    (FR-CMT-05), called by the Issue Service.

                    No authorship check applies: this is the system removing an issue's
                    thread, not a user editing their own words. Idempotent, so the cascade
                    can be retried.
                    """)
    @ApiResponse(responseCode = "204", description = "Comments deleted, or there were none")
    public ResponseEntity<Void> deleteByIssue(@PathVariable Integer issueId) {
        commentService.deleteByIssue(issueId);
        return ResponseEntity.noContent().build();
    }
}
