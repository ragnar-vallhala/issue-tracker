package com.its.comment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A comment on an issue.
 *
 * <p>Neither source defines this table - the case study lists a Comment Service in its
 * microservices breakdown and refers to a section 7.4 that does not exist, and the
 * reference workbook has no comment sheet. This shape is therefore this project's own
 * (SRS A-01).
 *
 * <p>{@code issueId} and {@code authorId} live in other services' databases and are plain
 * integers. This service does not verify them, and that is deliberate rather than lax:
 * the Issue Service already calls in here for counts and cascade deletes, so an outbound
 * call from here would close a cycle. The cost is that a comment can outlive a
 * mis-typed issue id; the cascade is what keeps that from happening in practice.
 */
@Entity
@Table(name = "comment", indexes = {
        @Index(name = "idx_comment_issue", columnList = "issue_id, created_on")
})
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Integer commentId;

    @Column(name = "issue_id", nullable = false)
    private Integer issueId;

    @Column(name = "author_id", nullable = false)
    private Integer authorId;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    protected Comment() {
        // Required by JPA.
    }

    public Comment(Integer issueId, Integer authorId, String body) {
        this.issueId = issueId;
        this.authorId = authorId;
        this.body = body;
    }

    public Integer getCommentId() {
        return commentId;
    }

    public Integer getIssueId() {
        return issueId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }
}
