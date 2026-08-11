package com.its.issue.client;

import com.its.issue.dto.response.ProjectSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative client for the Project Service, used to confirm an issue's project exists.
 *
 * <p>Only the single-project lookup is declared. This service must never call the Project
 * Service's {@code /issues} endpoints - those delegate straight back here, and wiring
 * them up would close a loop.
 */
@FeignClient(name = "project-service")
public interface ProjectFeignClient {

    @GetMapping("/api/projects/{projectId}")
    ProjectSummary findById(@PathVariable("projectId") Integer projectId);
}
