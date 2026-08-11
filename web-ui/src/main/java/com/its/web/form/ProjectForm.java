package com.its.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Project create/edit form backing bean (FR-UI-10).
 *
 * <p>There is no owner field. The owner is taken from the session, so a Project Owner
 * cannot create a project in someone else's name by editing a hidden input.
 */
public class ProjectForm {

    private Integer projectId;

    @NotBlank(message = "Please enter a project name")
    @Size(max = 255, message = "Project name must be at most 255 characters")
    private String projectName;

    @NotNull(message = "Please choose a start date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /**
     * The start/end ordering rule (FR-PRJ-03), checked here for immediate feedback and
     * again by the Project Service, which is where it is actually enforced.
     */
    public boolean hasValidDateRange() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
