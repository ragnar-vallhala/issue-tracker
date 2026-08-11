package com.its.issue.service;

import com.its.issue.client.CommentGateway;
import com.its.issue.client.ReferenceGateway;
import com.its.issue.dto.request.IssueRequest;
import com.its.issue.dto.request.IssueUpdateRequest;
import com.its.issue.dto.response.IssueResponse;
import com.its.issue.dto.response.UserSummary;
import com.its.issue.entity.Issue;
import com.its.issue.entity.Status;
import com.its.issue.exception.IssueExceptions.ForbiddenOperationException;
import com.its.issue.exception.IssueExceptions.IllegalStateTransitionException;
import com.its.issue.exception.IssueExceptions.InvalidReferenceException;
import com.its.issue.exception.IssueExceptions.ResourceNotFoundException;
import com.its.issue.repository.IssueRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IssueServiceImpl implements IssueService {

    private static final Logger log = LoggerFactory.getLogger(IssueServiceImpl.class);

    private final IssueRepository issueRepository;
    private final ReferenceGateway referenceGateway;
    private final CommentGateway commentGateway;

    public IssueServiceImpl(IssueRepository issueRepository,
                            ReferenceGateway referenceGateway,
                            CommentGateway commentGateway) {
        this.issueRepository = issueRepository;
        this.referenceGateway = referenceGateway;
        this.commentGateway = commentGateway;
    }

    @Override
    @Transactional
    public IssueResponse create(IssueRequest request) {
        validateReferences(request.projectId(), request.assigneeId(), request.createdBy());

        Issue issue = new Issue(
                request.summary().trim(),
                request.description(),
                request.projectId(),
                request.assigneeId(),
                request.createdBy(),
                request.status(),
                request.priority(),
                request.type(),
                request.storyPoints(),
                request.sprint(),
                request.tags());

        // createdOn and lastUpdatedOn are stamped by Hibernate, not taken from the
        // request - a client cannot backdate an issue (FR-ISS-03).
        Issue saved = issueRepository.save(issue);
        log.info("Created issue {} in project {}", saved.getIssueId(), saved.getProjectId());

        return IssueResponse.from(saved);
    }

    @Override
    public List<IssueResponse> findAll() {
        return issueRepository.findAll().stream().map(IssueResponse::from).toList();
    }

    @Override
    public IssueResponse findById(Integer issueId) {
        IssueResponse response = IssueResponse.from(requireIssue(issueId));

        // Null when the Comment Service is unreachable, which omits the field rather
        // than claiming a count of zero. See CommentGateway.
        return response.withCommentCount(commentGateway.countByIssue(issueId));
    }

    @Override
    public List<IssueResponse> findByProject(Integer projectId) {
        return issueRepository.findByProjectId(projectId).stream()
                .map(IssueResponse::from)
                .toList();
    }

    @Override
    public List<IssueResponse> findByCreator(Integer ownerId) {
        return issueRepository.findByCreatedBy(ownerId).stream()
                .map(IssueResponse::from)
                .toList();
    }

    @Override
    public List<IssueResponse> findByAssignee(Integer assigneeId) {
        return issueRepository.findByAssigneeId(assigneeId).stream()
                .map(IssueResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public IssueResponse update(Integer issueId, IssueUpdateRequest request,
                                CallerIdentity caller) {

        Issue issue = requireIssue(issueId);

        enforceAssigneeRestrictions(issue, request, caller);

        if (request.status() != null) {
            applyStatusChange(issue, request.status());
        }
        if (request.summary() != null) {
            issue.setSummary(request.summary().trim());
        }
        if (request.description() != null) {
            issue.setDescription(request.description());
        }
        if (request.projectId() != null && !request.projectId().equals(issue.getProjectId())) {
            requireProject(request.projectId());
            issue.setProjectId(request.projectId());
        }
        if (request.assigneeId() != null && !request.assigneeId().equals(issue.getAssigneeId())) {
            requireAssignee(request.assigneeId());
            issue.setAssigneeId(request.assigneeId());
        }
        if (request.priority() != null) {
            issue.setPriority(request.priority());
        }
        if (request.type() != null) {
            issue.setType(request.type());
        }
        if (request.storyPoints() != null) {
            issue.setStoryPoints(request.storyPoints());
        }
        if (request.sprint() != null) {
            issue.setSprint(request.sprint());
        }
        if (request.tags() != null) {
            issue.setTags(request.tags());
        }

        return IssueResponse.from(issueRepository.save(issue));
    }

    /**
     * Deletes an issue and its comments (FR-ISS-08).
     *
     * <p>Comments first, then the issue - the same children-first ordering as the project
     * cascade, for the same reason. If the comment delete fails, the issue survives and
     * the operation can be retried; the reverse order would leave comments attached to an
     * issue id that no longer exists, with nothing left to find them by.
     */
    @Override
    @Transactional
    public void delete(Integer issueId) {
        Issue issue = requireIssue(issueId);

        commentGateway.deleteByIssue(issueId);
        issueRepository.delete(issue);

        log.info("Deleted issue {} and its comments", issueId);
    }

    /**
     * Deletes every issue in a project, and each one's comments (FR-ISS-09).
     *
     * <p>The middle step of the project cascade. Comments for all affected issues are
     * removed before any issue row is deleted, so a failure part-way leaves every issue
     * still present and the whole cascade repeatable. Deleting the issues first would
     * strand their comments permanently - nothing else in the system knows those issue
     * ids ever existed.
     *
     * <p>A no-op when the project has no issues, which is what makes it safe for the
     * Project Service to call unconditionally.
     */
    @Override
    @Transactional
    public void deleteByProject(Integer projectId) {
        List<Integer> issueIds = issueRepository.findIdsByProjectId(projectId);

        if (issueIds.isEmpty()) {
            log.info("Cascade: project {} has no issues - nothing to delete", projectId);
            return;
        }

        for (Integer issueId : issueIds) {
            commentGateway.deleteByIssue(issueId);
        }

        int deleted = issueRepository.deleteByProjectId(projectId);
        log.info("Cascade: deleted {} issues and their comments for project {}",
                deleted, projectId);
    }

    private Issue requireIssue(Integer issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> ResourceNotFoundException.issue(issueId));
    }

    /**
     * Applies a status change, refusing an illegal transition (FR-ISS-14).
     *
     * <p>Only DONE is constrained: a completed issue must be explicitly reopened to TO_DO
     * rather than sliding back into IN_PROGRESS, so that reopening is a visible decision.
     */
    private void applyStatusChange(Issue issue, Status target) {
        Status current = issue.getStatus();

        if (!current.canTransitionTo(target)) {
            throw new IllegalStateTransitionException(
                    "Cannot move an issue from " + current + " to " + target
                            + ". A completed issue must be reopened to TO_DO first.");
        }

        issue.setStatus(target);
    }

    /**
     * Enforces FR-ISS-07: an Assignee may change the status of their own issue, and
     * nothing else.
     *
     * <p>Two separate refusals, because they are different mistakes. Touching another
     * field is an attempt at an operation the role does not have. Touching an issue
     * assigned to somebody else is an attempt on a resource that is not theirs - and
     * without that second check, any Assignee could close every issue in the system.
     *
     * <p>A caller with no identity headers is unrestricted; see {@link CallerIdentity}.
     */
    private void enforceAssigneeRestrictions(Issue issue, IssueUpdateRequest request,
                                             CallerIdentity caller) {

        if (!caller.isKnown() || !caller.isAssignee()) {
            return;
        }

        if (request.touchesFieldsBeyondStatus()) {
            throw new ForbiddenOperationException(
                    "An Assignee may only update the status of an issue");
        }

        if (!java.util.Objects.equals(issue.getAssigneeId(), caller.userId())) {
            throw new ForbiddenOperationException(
                    "An Assignee may only update issues assigned to them");
        }
    }

    /** Confirms every cross-service reference on a new issue (FR-ISS-02). */
    private void validateReferences(Integer projectId, Integer assigneeId, Integer createdBy) {
        requireProject(projectId);

        if (assigneeId != null) {
            requireAssignee(assigneeId);
        }

        referenceGateway.findUser(createdBy)
                .orElseThrow(() -> new InvalidReferenceException("createdBy",
                        "No user exists with id " + createdBy));
    }

    private void requireProject(Integer projectId) {
        referenceGateway.findProject(projectId)
                .orElseThrow(() -> new InvalidReferenceException("projectId",
                        "No project exists with id " + projectId));
    }

    /**
     * An assignee must exist and must actually hold the Assignee role.
     *
     * <p>The role check mirrors the one the Project Service makes on owners: assigning
     * work to a Project Owner would put an issue on a dashboard built for the other role.
     */
    private void requireAssignee(Integer assigneeId) {
        UserSummary user = referenceGateway.findUser(assigneeId)
                .orElseThrow(() -> new InvalidReferenceException("assigneeId",
                        "No user exists with id " + assigneeId));

        if (!user.isAssignee()) {
            throw new InvalidReferenceException("assigneeId",
                    "User " + assigneeId + " is not an Assignee");
        }
    }
}
