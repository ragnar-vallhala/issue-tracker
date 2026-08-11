package com.its.issue.client;

import com.its.issue.dto.response.ProjectSummary;
import com.its.issue.dto.response.UserSummary;
import com.its.issue.exception.IssueExceptions.ServiceUnavailableException;
import feign.FeignException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Confirms that the users and projects an issue points at actually exist (FR-ISS-02).
 *
 * <p>The distinction this class draws, in both methods: a 404 is a fact - that user or
 * project is genuinely absent, so the caller's request is invalid (400). Anything else -
 * a timeout, a refused connection, a 500 - means we do not know, and reporting "no such
 * project" would blame the caller for our own outage (503).
 */
@Component
public class ReferenceGateway {

    private static final Logger log = LoggerFactory.getLogger(ReferenceGateway.class);

    private final UserFeignClient userFeignClient;
    private final ProjectFeignClient projectFeignClient;

    public ReferenceGateway(UserFeignClient userFeignClient,
                            ProjectFeignClient projectFeignClient) {
        this.userFeignClient = userFeignClient;
        this.projectFeignClient = projectFeignClient;
    }

    public Optional<UserSummary> findUser(Integer userId) {
        try {
            return Optional.ofNullable(userFeignClient.findById(userId));

        } catch (FeignException.NotFound ex) {
            return Optional.empty();

        } catch (FeignException ex) {
            log.warn("User Service call failed for user {}: {}", userId, ex.getMessage());
            throw new ServiceUnavailableException("User Service", ex);
        }
    }

    public Optional<ProjectSummary> findProject(Integer projectId) {
        try {
            return Optional.ofNullable(projectFeignClient.findById(projectId));

        } catch (FeignException.NotFound ex) {
            return Optional.empty();

        } catch (FeignException ex) {
            log.warn("Project Service call failed for project {}: {}",
                    projectId, ex.getMessage());
            throw new ServiceUnavailableException("Project Service", ex);
        }
    }
}
