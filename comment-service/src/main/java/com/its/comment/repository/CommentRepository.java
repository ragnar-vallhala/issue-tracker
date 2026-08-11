package com.its.comment.repository;

import com.its.comment.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    /** Newest first (FR-CMT-02), served by the composite index on the entity. */
    List<Comment> findByIssueIdOrderByCreatedOnDesc(Integer issueId);

    long countByIssueId(Integer issueId);

    /** Bulk delete for the cascade (FR-CMT-05) - one statement, not a per-row loop. */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.issueId = :issueId")
    int deleteByIssueId(@Param("issueId") Integer issueId);
}
