package com.its.project.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Create or update a project (FR-PRJ-01, FR-PRJ-07).
 *
 * <p>The start/end ordering rule (FR-PRJ-03) is not expressible with field-level
 * annotations, so it lives in the service layer where both values are visible at once.
 */
public record ProjectRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must be at most 255 characters")
        String projectName,

        @NotNull(message = "must be supplied")
        Integer projectOwnerId,

        @NotNull(message = "must be supplied")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate) {
}
