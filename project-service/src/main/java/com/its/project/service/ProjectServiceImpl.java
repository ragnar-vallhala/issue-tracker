package com.its.project.service;

import com.its.project.client.IssueGateway;
import com.its.project.client.UserGateway;
import com.its.project.dto.request.ProjectRequest;
import com.its.project.dto.response.IssueSummary;
import com.its.project.dto.response.ProjectResponse;
import com.its.project.dto.response.UserSummary;
import com.its.project.entity.Project;
import com.its.project.exception.ProjectExceptions.DuplicateResourceException;
import com.its.project.exception.ProjectExceptions.InvalidReferenceException;
import com.its.project.exception.ProjectExceptions.ResourceNotFoundException;
import com.its.project.repository.ProjectRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectRepository projectRepository;
    private final UserGateway userGateway;
    private final IssueGateway issueGateway;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              UserGateway userGateway,
                              IssueGateway issueGateway) {
        this.projectRepository = projectRepository;
        this.userGateway = userGateway;
        this.issueGateway = issueGateway;
    }

    @Override
    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        validateDates(request);
        requireOwner(request.projectOwnerId());

        if (projectRepository.existsByProjectName(request.projectName().trim())) {
            throw new DuplicateResourceException("projectName",
                    "A project with this name already exists");
        }

        Project project = new Project(
                request.projectName().trim(),
                request.projectOwnerId(),
                request.startDate(),
                request.endDate());

        Project saved = projectRepository.save(project);
        log.info("Created project {} owned by user {}",
                saved.getProjectId(), saved.getProjectOwnerId());

        return ProjectResponse.from(saved);
    }

    @Override
    public List<ProjectResponse> findAll() {
        return projectRepository.findAll().stream().map(ProjectResponse::from).toList();
    }

    @Override
    public ProjectResponse findById(Integer projectId) {
        return ProjectResponse.from(requireProject(projectId));
    }

    @Override
    public List<ProjectResponse> findByOwner(Integer ownerId) {
        return projectRepository.findByProjectOwnerId(ownerId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ProjectResponse update(Integer projectId, ProjectRequest request) {
        Project project = requireProject(projectId);
        validateDates(request);

        // Only re-check ownership if it is actually changing: a needless HTTP call on
        // every edit is latency spent to confirm something already known.
        if (!project.getProjectOwnerId().equals(request.projectOwnerId())) {
            requireOwner(request.projectOwnerId());
            project.setProjectOwnerId(request.projectOwnerId());
        }

        String newName = request.projectName().trim();
        if (!project.getProjectName().equals(newName)
                && projectRepository.existsByProjectName(newName)) {
            throw new DuplicateResourceException("projectName",
                    "A project with this name already exists");
        }

        project.setProjectName(newName);
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());

        return ProjectResponse.from(projectRepository.save(project));
    }

    /**
     * Deletes a project and everything beneath it (FR-PRJ-09, FR-PRJ-10, FR-PRJ-11).
     *
     * <p><strong>The order of the two statements below is the entire safety property of
     * this operation</strong>, and it is deliberately children-first:
     *
     * <ol>
     *   <li>Ask the Issue Service to delete the project's issues, which in turn deletes
     *       their comments. If this fails it throws, and the method exits before the
     *       project row is touched.
     *   <li>Only then delete the project row itself.
     * </ol>
     *
     * <p>Reversing this would be simpler and faster and would produce exactly the failure
     * that matters: a crash between the two leaves issues pointing at a project id that
     * no longer exists - invisible to every screen, unreachable by every endpoint, and
     * with nothing left to trigger a retry. In the order written here, the same crash
     * leaves the project still listed and still deletable, so the operation is simply
     * repeated. The remote delete is idempotent, so repeating it is safe.
     *
     * <p>This is not atomic and does not pretend to be - there is no transaction spanning
     * three databases (DESIGN 6.4). What the ordering buys is that every partial state is
     * a recoverable one. The residual cost is a narrow window in which an issue's
     * comments are gone while the issue survives.
     *
     * <p>Note also that {@code @Transactional} here covers only the local row. It has no
     * authority over the remote call and cannot roll it back.
     */
    @Override
    @Transactional
    public void delete(Integer projectId) {
        Project project = requireProject(projectId);

        issueGateway.deleteByProject(projectId);
        projectRepository.delete(project);

        log.info("Deleted project {} and cascaded to its issues", projectId);
    }

    @Override
    public List<IssueSummary> findIssuesByProjectId(Integer projectId) {
        // Existence is confirmed here so an unknown project is a 404 rather than an
        // empty list that reads as "this project has no issues".
        requireProject(projectId);
        return issueGateway.findByProject(projectId);
    }

    @Override
    public List<IssueSummary> findIssuesByProjectName(String projectName) {
        Project project = projectRepository.findByProjectName(projectName)
                .orElseThrow(() -> ResourceNotFoundException.project(projectName));

        return issueGateway.findByProject(project.getProjectId());
    }

    private Project requireProject(Integer projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.project(projectId));
    }

    /**
     * Confirms the owner exists and actually holds the Project Owner role (FR-PRJ-02).
     *
     * <p>Both halves matter. The first stops a project being created against a user id
     * that was never real; the second stops an Assignee being recorded as an owner, which
     * would give them a project they can administer through a role check they do not pass.
     */
    private void requireOwner(Integer ownerId) {
        UserSummary owner = userGateway.findById(ownerId)
                .orElseThrow(() -> new InvalidReferenceException("projectOwnerId",
                        "No user exists with id " + ownerId));

        if (!owner.isProjectOwner()) {
            throw new InvalidReferenceException("projectOwnerId",
                    "User " + ownerId + " is not a Project Owner");
        }
    }

    /**
     * Enforces the start/end ordering (FR-PRJ-03).
     *
     * <p>A cross-field rule, so it cannot live on the DTO as an annotation - both values
     * have to be in hand at once.
     */
    private void validateDates(ProjectRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException(
                    "endDate must not be earlier than startDate");
        }
    }
}
