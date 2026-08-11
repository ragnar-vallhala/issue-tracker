package com.its.comment.dto;

import com.its.comment.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Request and response shapes for the Comment Service (SRS 9.4). */
public final class CommentDtos {

    private CommentDtos() {
    }

    /** Create a comment (FR-CMT-01). */
    public record CreateCommentRequest(

            @NotNull(message = "must be supplied")
            Integer issueId,

            @NotNull(message = "must be supplied")
            Integer authorId,

            @NotBlank(message = "must not be blank")
            @Size(max = 5000, message = "must be at most 5000 characters")
            String body) {
    }

    /** Update a comment's text (FR-CMT-03). Only the body is mutable. */
    public record UpdateCommentRequest(

            @NotBlank(message = "must not be blank")
            @Size(max = 5000, message = "must be at most 5000 characters")
            String body) {
    }

    public record CommentResponse(
            Integer commentId,
            Integer issueId,
            Integer authorId,
            String body,
            LocalDateTime createdOn) {

        public static CommentResponse from(Comment comment) {
            return new CommentResponse(
                    comment.getCommentId(),
                    comment.getIssueId(),
                    comment.getAuthorId(),
                    comment.getBody(),
                    comment.getCreatedOn());
        }
    }
}
