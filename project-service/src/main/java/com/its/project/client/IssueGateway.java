package com.its.project.client;

import com.its.project.dto.response.IssueSummary;
import com.its.project.exception.ProjectExceptions.ServiceUnavailableException;
import feign.FeignException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Wraps {@link IssueFeignClient}, translating transport failures into 503s. */
@Component
public class IssueGateway {

    private static final Logger log = LoggerFactory.getLogger(IssueGateway.class);

    private final IssueFeignClient issueFeignClient;

    public IssueGateway(IssueFeignClient issueFeignClient) {
        this.issueFeignClient = issueFeignClient;
    }

    /**
     * Issues belonging to a project (FR-PRJ-13).
     *
     * <p>Not caught-and-emptied: an empty list would assert the project has no issues,
     * which during an outage is a claim we cannot support (SRS A-08).
     */
    public List<IssueSummary> findByProject(Integer projectId) {
        try {
            List<IssueSummary> issues = issueFeignClient.findByProject(projectId);
            return issues == null ? List.of() : issues;

        } catch (FeignException ex) {
            log.warn("Issue Service call failed for project {}: {}", projectId, ex.getMessage());
            throw new ServiceUnavailableException("Issue Service", ex);
        }
    }

    /**
     * Deletes every issue in a project, and by extension their comments (FR-PRJ-10).
     *
     * <p>Any failure propagates, and the caller must not proceed to delete the project
     * row - that ordering is the whole safety property of the cascade (FR-PRJ-11,
     * DESIGN 6.4). This is also why there is no retry here: the operation is idempotent
     * and safe to re-run, but retrying inside a request that is already failing just
     * lengthens the outage for the caller.
     */
    public void deleteByProject(Integer projectId) {
        try {
            issueFeignClient.deleteByProject(projectId);
            log.info("Cascade: issues for project {} deleted", projectId);

        } catch (FeignException ex) {
            log.error("Cascade delete failed for project {} - project row will be left "
                    + "intact so the operation can be retried: {}", projectId, ex.getMessage());
            throw new ServiceUnavailableException("Issue Service", ex);
        }
    }
}
