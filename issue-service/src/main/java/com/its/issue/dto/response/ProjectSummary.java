package com.its.issue.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A project, as far as the Issue Service needs one - a local copy, see DESIGN section 3. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectSummary(
        Integer projectId,
        String projectName,
        Integer projectOwnerId) {
}
