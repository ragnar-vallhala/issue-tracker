package com.its.user.client;

import com.its.user.dto.response.IssueSummary;
import com.its.user.exception.ServiceUnavailableException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Reaches the Issue Service to answer the two issue-bearing user endpoints
 * (FR-USR-09, FR-USR-10).
 *
 * <p>The Issue Service is the sole owner of issue queries. This service never filters
 * issues itself - it holds no issue data to filter. Both endpoints here are facades that
 * delegate, which is what keeps the read-direction call graph acyclic (DESIGN 6.1).
 */
@Component
public class IssueClient {

    private static final Logger log = LoggerFactory.getLogger(IssueClient.class);

    /** Resolved against Eureka by the load-balanced RestTemplate, not a hard-coded host. */
    private static final String ISSUES_BY_ASSIGNEE = "http://issue-service/api/issues/assignee/{id}";

    private final RestTemplate restTemplate;

    public IssueClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches the issues assigned to a user.
     *
     * @throws ServiceUnavailableException if the Issue Service cannot be reached. This is
     *         deliberately not caught-and-emptied: an empty list would claim the user has
     *         no assigned work, which is a different and possibly false statement
     *         (SRS A-08).
     */
    public List<IssueSummary> findByAssignee(Integer userId) {
        try {
            ResponseEntity<IssueSummary[]> response =
                    restTemplate.getForEntity(ISSUES_BY_ASSIGNEE, IssueSummary[].class, userId);

            IssueSummary[] body = response.getBody();
            return body == null ? List.of() : Arrays.asList(body);

        } catch (RestClientException ex) {
            log.warn("Issue Service call failed for assignee {}: {}", userId, ex.getMessage());
            throw new ServiceUnavailableException("Issue Service", ex);
        }
    }
}
