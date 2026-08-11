package com.its.project.client;

import com.its.project.dto.response.IssueSummary;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Declarative client for the Issue Service, which owns all issue data and queries. */
@FeignClient(name = "issue-service")
public interface IssueFeignClient {

    @GetMapping("/api/issues/project/{projectId}")
    List<IssueSummary> findByProject(@PathVariable("projectId") Integer projectId);

    /**
     * Bulk delete used by the project cascade (FR-PRJ-10).
     *
     * <p>The Issue Service is responsible for deleting each issue's comments before
     * removing the issues themselves - this service does not know the Comment Service
     * exists, and should not.
     */
    @DeleteMapping("/api/issues/project/{projectId}")
    void deleteByProject(@PathVariable("projectId") Integer projectId);
}
