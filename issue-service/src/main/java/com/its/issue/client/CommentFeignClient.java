package com.its.issue.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Declarative client for the Comment Service. */
@FeignClient(name = "comment-service")
public interface CommentFeignClient {

    @GetMapping("/api/comments/issue/{issueId}/count")
    Long countByIssue(@PathVariable("issueId") Integer issueId);

    /** Bulk delete, the deepest step of the cascade (FR-CMT-05). */
    @DeleteMapping("/api/comments/issue/{issueId}")
    void deleteByIssue(@PathVariable("issueId") Integer issueId);
}
