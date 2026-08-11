package com.its.comment.service;

import com.its.comment.dto.CommentDtos.CommentResponse;
import com.its.comment.dto.CommentDtos.CreateCommentRequest;
import com.its.comment.dto.CommentDtos.UpdateCommentRequest;
import com.its.comment.entity.Comment;
import com.its.comment.exception.CommentExceptions.ForbiddenOperationException;
import com.its.comment.exception.CommentExceptions.ResourceNotFoundException;
import com.its.comment.repository.CommentRepository;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business operations for comments (SRS section 6). */
@Service
@Transactional(readOnly = true)
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Transactional
    public CommentResponse create(CreateCommentRequest request) {
        Comment comment = new Comment(request.issueId(), request.authorId(),
                request.body().trim());

        return CommentResponse.from(commentRepository.save(comment));
    }

    public List<CommentResponse> findByIssue(Integer issueId) {
        return commentRepository.findByIssueIdOrderByCreatedOnDesc(issueId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    public long countByIssue(Integer issueId) {
        return commentRepository.countByIssueId(issueId);
    }

    /**
     * Updates a comment's text, if the caller wrote it (FR-CMT-03).
     *
     * @param callerId the author id from the gateway's {@code X-User-Id} header, or null
     *        when the service is called directly in development
     */
    @Transactional
    public CommentResponse update(Integer commentId, UpdateCommentRequest request,
                                  Integer callerId) {

        Comment comment = requireComment(commentId);
        requireAuthor(comment, callerId, "edit");

        comment.setBody(request.body().trim());
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void delete(Integer commentId, Integer callerId) {
        Comment comment = requireComment(commentId);
        requireAuthor(comment, callerId, "delete");

        commentRepository.delete(comment);
    }

    /**
     * Deletes every comment on an issue - the deepest step of the cascade (FR-CMT-05).
     *
     * <p>No author check: this is not a user editing their own words, it is the system
     * removing an issue that no longer exists. Applying the authorship rule here would
     * make the cascade fail on any thread with more than one participant.
     *
     * <p>Idempotent, and silent when there is nothing to remove, so the Issue Service can
     * call it for every issue without first asking whether comments exist - and can retry
     * the whole cascade safely.
     */
    @Transactional
    public void deleteByIssue(Integer issueId) {
        int deleted = commentRepository.deleteByIssueId(issueId);

        if (deleted > 0) {
            log.info("Cascade: deleted {} comments for issue {}", deleted, issueId);
        }
    }

    private Comment requireComment(Integer commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(commentId));
    }

    private void requireAuthor(Comment comment, Integer callerId, String action) {
        // A call with no identity header is unrestricted - it has arrived directly rather
        // than through the gateway, which in practice means development tooling. See
        // DESIGN section 9 on why the service ports must not be publicly routable.
        if (callerId == null) {
            return;
        }

        if (!Objects.equals(comment.getAuthorId(), callerId)) {
            throw new ForbiddenOperationException(
                    "Only the author of a comment may " + action + " it");
        }
    }
}
