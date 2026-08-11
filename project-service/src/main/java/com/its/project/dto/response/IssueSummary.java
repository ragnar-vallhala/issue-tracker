package com.its.project.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * An issue, as far as the Project Service needs to understand one.
 *
 * <p>A local copy rather than a shared type - see DESIGN section 3. Unknown properties
 * are ignored so the Issue Service can evolve its response without breaking this one.
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
