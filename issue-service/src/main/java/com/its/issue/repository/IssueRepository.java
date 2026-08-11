package com.its.issue.repository;

import com.its.issue.entity.Issue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends JpaRepository<Issue, Integer> {

    List<Issue> findByProjectId(Integer projectId);

    List<Issue> findByAssigneeId(Integer assigneeId);

    List<Issue> findByCreatedBy(Integer createdBy);

    /** Ids only, so the cascade can fan out to the Comment Service without loading rows. */
    @Query("SELECT i.issueId FROM Issue i WHERE i.projectId = :projectId")
    List<Integer> findIdsByProjectId(@Param("projectId") Integer projectId);

    /**
     * Bulk delete for the project cascade (FR-ISS-09).
     *
     * <p>A single statement rather than a load-then-delete loop: the caller has already
     * collected the ids it needs for the comment cascade, and iterating entities here
     * would issue one DELETE per row for no benefit.
     */
    @Modifying
    @Query("DELETE FROM Issue i WHERE i.projectId = :projectId")
    int deleteByProjectId(@Param("projectId") Integer projectId);
}
