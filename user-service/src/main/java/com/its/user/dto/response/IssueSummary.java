package com.its.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * An issue as this service understands it - which is to say, barely.
 *
 * <p>Used only to deserialise the Issue Service's response when serving the two
 * inter-service endpoints (FR-USR-09, FR-USR-10). This is a local copy on purpose: a
 * shared DTO module would couple every service to one release cycle and turn the system
 * into a distributed monolith (DESIGN section 3).
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} is the practical consequence of
 * that choice, and the point of it: the Issue Service can add fields without breaking
 * this service's deserialisation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueSummary(
        Integer issueId,
        String summary,
        String description,
        Integer projectId,
        Integer assigneeId,
        Integer createdBy,
        String status,
        String priority,
        String type,
        Integer storyPoints,
        String sprint,
        String tags,
        LocalDateTime createdOn,
        LocalDateTime lastUpdatedOn) {
}
