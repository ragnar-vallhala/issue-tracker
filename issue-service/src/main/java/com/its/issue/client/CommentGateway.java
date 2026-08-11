package com.its.issue.client;

import com.its.issue.exception.IssueExceptions.ServiceUnavailableException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Talks to the Comment Service - for the comment count on an issue, and for the deepest
 * step of the delete cascade.
 *
 * <p>The two methods handle failure differently, deliberately.
 */
@Component
public class CommentGateway {

    private static final Logger log = LoggerFactory.getLogger(CommentGateway.class);

    private final CommentFeignClient commentFeignClient;

    public CommentGateway(CommentFeignClient commentFeignClient) {
        this.commentFeignClient = commentFeignClient;
    }

    /**
     * The number of comments on an issue (FR-CMT-04).
     *
     * <p>Degrades rather than failing: if the Comment Service is unreachable this returns
     * null, the field is omitted from the response, and the issue itself is still served.
     * That is not a violation of SRS A-08 - the rule there is that a failure must never be
     * dressed up as a fact. Returning {@code 0} would assert the issue has no comments,
     * which might be false; omitting the field asserts nothing at all. The issue is the
     * resource being requested, and refusing to serve it because a decoration is
     * unavailable trades a working page for an error page.
     */
    public Long countByIssue(Integer issueId) {
        try {
            return commentFeignClient.countByIssue(issueId);

        } catch (FeignException ex) {
            log.warn("Comment count unavailable for issue {} - serving the issue without it: {}",
                    issueId, ex.getMessage());
            return null;
        }
    }

    /**
     * Deletes every comment on an issue (FR-CMT-05).
     *
     * <p>Failure propagates, unlike {@link #countByIssue}. This is a step of the cascade,
     * and the caller must not proceed to delete the issue if its comments could not be
     * removed - that ordering is what keeps a partial delete recoverable (DESIGN 6.4).
     */
    public void deleteByIssue(Integer issueId) {
        try {
            commentFeignClient.deleteByIssue(issueId);

        } catch (FeignException ex) {
            log.error("Cascade: comment delete failed for issue {} - the issue will be left "
                    + "intact so the operation can be retried: {}", issueId, ex.getMessage());
            throw new ServiceUnavailableException("Comment Service", ex);
        }
    }
}
