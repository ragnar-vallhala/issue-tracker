package com.its.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A project, owned by exactly one Project Owner.
 *
 * <p>{@code projectOwnerId} references a user in a different service's database, so it is
 * a plain integer and not a JPA association - there is no foreign key, and there cannot
 * be one (SRS C-02). Whether the id is real, and whether that user actually holds the
 * PROJECT_OWNER role, is checked over HTTP at write time (FR-PRJ-02).
 *
 * <p>Column names follow the reference workbook (SRS A-15).
 */
@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Integer projectId;

    /**
     * Unique, because {@code GET /api/projects/projectName/{name}/issues} has to resolve
     * a name to exactly one project (FR-PRJ-14). Without the constraint that endpoint is
     * ambiguous the first time two projects share a name.
     */
    @Column(name = "project_name", nullable = false, unique = true, length = 255)
    private String projectName;

    @Column(name = "project_owner_id", nullable = false)
    private Integer projectOwnerId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    protected Project() {
        // Required by JPA.
    }

    public Project(String projectName, Integer projectOwnerId,
                   LocalDate startDate, LocalDate endDate) {
        this.projectName = projectName;
        this.projectOwnerId = projectOwnerId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Integer getProjectOwnerId() {
        return projectOwnerId;
    }

    public void setProjectOwnerId(Integer projectOwnerId) {
        this.projectOwnerId = projectOwnerId;
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
