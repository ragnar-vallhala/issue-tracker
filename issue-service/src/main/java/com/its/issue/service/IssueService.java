package com.its.issue.service;

import com.its.issue.dto.request.IssueRequest;
import com.its.issue.dto.request.IssueUpdateRequest;
import com.its.issue.dto.response.IssueResponse;
import java.util.List;

/** Business operations for issue management (SRS section 5). */
public interface IssueService {

    IssueResponse create(IssueRequest request);

    List<IssueResponse> findAll();

    /** Single issue, decorated with its comment count where the Comment Service answers. */
    IssueResponse findById(Integer issueId);

    List<IssueResponse> findByProject(Integer projectId);

    List<IssueResponse> findByCreator(Integer ownerId);

    List<IssueResponse> findByAssignee(Integer assigneeId);

    IssueResponse update(Integer issueId, IssueUpdateRequest request, CallerIdentity caller);

    /** Deletes an issue and its comments (FR-ISS-08). */
    void delete(Integer issueId);

    /** Deletes every issue in a project and their comments; the project cascade (FR-ISS-09). */
    void deleteByProject(Integer projectId);
}
