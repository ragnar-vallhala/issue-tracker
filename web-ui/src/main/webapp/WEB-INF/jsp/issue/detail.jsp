<%--
  Issue detail (FR-UI-18), status control (FR-UI-17) and comment thread (FR-UI-19).

  Shared by both roles. A Project Owner gets the full edit link; an Assignee gets a status
  control offering only the transitions the service will accept, and everything else
  read-only. The rendering follows the rule - the Issue Service enforces it.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Issue ${issue.issueId()}"/>
<c:set var="activeNav" value=""/>
<jsp:include page="../layout/head.jsp"/>

<div class="page-head">
    <div>
        <h1><c:out value="${issue.summary()}"/></h1>
        <p class="subtitle">
            Issue #${issue.issueId()} in
            <c:choose>
                <c:when test="${user.isProjectOwner()}">
                    <a href="${pageContext.request.contextPath}/owner/projects/${project.projectId()}">
                        <c:out value="${project.projectName()}"/></a>
                </c:when>
                <c:otherwise><c:out value="${project.projectName()}"/></c:otherwise>
            </c:choose>
        </p>
    </div>
    <c:if test="${canEditEverything}">
        <a class="btn btn-secondary"
           href="${pageContext.request.contextPath}/owner/issues/${issue.issueId()}/edit">
            Edit issue</a>
    </c:if>
</div>

<div class="card">
    <div class="detail-grid">
        <div class="detail-item">
            <div class="detail-label">Status</div>
            <div class="detail-value">
                <span class="badge badge-${issue.statusSlug()}">
                    <c:out value="${issue.statusLabel()}"/>
                </span>
            </div>
        </div>
        <div class="detail-item">
            <div class="detail-label">Priority</div>
            <div class="detail-value">
                <span class="badge badge-${issue.prioritySlug()}">
                    <c:out value="${issue.priorityLabel()}"/>
                </span>
            </div>
        </div>
        <div class="detail-item">
            <div class="detail-label">Type</div>
            <div class="detail-value"><c:out value="${issue.typeLabel()}"/></div>
        </div>
        <div class="detail-item">
            <div class="detail-label">Assignee</div>
            <div class="detail-value">
                <c:choose>
                    <c:when test="${empty issue.assigneeId()}">
                        <span class="muted">Unassigned</span>
                    </c:when>
                    <c:otherwise><c:out value="${userNames[issue.assigneeId()]}"/></c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="detail-item">
            <div class="detail-label">Reported by</div>
            <div class="detail-value"><c:out value="${userNames[issue.createdBy()]}"/></div>
        </div>
        <div class="detail-item">
            <div class="detail-label">Story points</div>
            <div class="detail-value">
                <c:out value="${empty issue.storyPoints() ? '—' : issue.storyPoints()}"/>
            </div>
        </div>
        <div class="detail-item">
            <div class="detail-label">Sprint</div>
            <div class="detail-value">
                <c:out value="${empty issue.sprint() ? '—' : issue.sprint()}"/>
            </div>
        </div>
        <div class="detail-item">
            <div class="detail-label">Last updated</div>
            <div class="detail-value">
                <c:if test="${not empty issue.lastUpdatedOn()}">
                    ${issue.lastUpdatedOn().toLocalDate()}
                </c:if>
            </div>
        </div>
    </div>

    <c:if test="${not empty issue.description()}">
        <div style="margin-top:20px">
            <div class="detail-label">Description</div>
            <p style="margin:4px 0 0"><c:out value="${issue.description()}"/></p>
        </div>
    </c:if>

    <c:if test="${not empty issue.tagList()}">
        <div style="margin-top:16px">
            <div class="detail-label">Tags</div>
            <div style="margin-top:4px">
                <c:forEach var="tag" items="${issue.tagList()}">
                    <span class="tag"><c:out value="${tag}"/></span>
                </c:forEach>
            </div>
        </div>
    </c:if>
</div>

<c:if test="${canChangeStatus}">
    <div class="card">
        <h2>Update status</h2>
        <form method="post"
              action="${pageContext.request.contextPath}/issues/${issue.issueId()}/status"
              class="filters">
            <div class="form-row">
                <label for="status">New status</label>
                <%-- Only legal transitions are offered, so the rule is visible in the
                     control instead of being discovered through a 409 (FR-ISS-14). --%>
                <select name="status" id="status">
                    <c:forEach var="entry" items="${transitions}">
                        <option value="${entry.key}"
                                <c:if test="${entry.key eq issue.status()}">selected</c:if>>
                            <c:out value="${entry.value}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <button type="submit" class="btn">Update</button>
        </form>
        <c:if test="${issue.status() eq 'DONE'}">
            <p class="hint" style="margin-top:10px">
                This issue is done. It can be reopened to To Do, but not moved straight
                back into progress.
            </p>
        </c:if>
    </div>
</c:if>

<div class="card card-tight">
    <h2>
        Comments
        <c:if test="${not empty issue.commentCount()}">(${issue.commentCount()})</c:if>
    </h2>

    <c:choose>
        <c:when test="${empty comments}">
            <div class="empty">No comments yet.</div>
        </c:when>
        <c:otherwise>
            <c:forEach var="comment" items="${comments}">
                <div class="comment">
                    <div class="comment-head">
                        <span class="comment-author">
                            <c:out value="${userNames[comment.authorId()]}"/>
                        </span>
                        <span class="comment-time">
                            <c:if test="${not empty comment.createdOn()}">
                                ${comment.createdOn().toLocalDate()}
                            </c:if>
                            <%-- Edit and delete appear only on your own comments
                                 (FR-CMT-03); the service enforces it regardless. --%>
                            <c:if test="${comment.authorId() eq user.userId()}">
                                &nbsp;·&nbsp;
                                <form method="post" style="display:inline"
                                      action="${pageContext.request.contextPath}/comments/${comment.commentId()}/delete">
                                    <input type="hidden" name="issueId" value="${issue.issueId()}">
                                    <button type="submit" class="btn-link"
                                            style="color:var(--danger)">delete</button>
                                </form>
                            </c:if>
                        </span>
                    </div>
                    <div class="comment-body"><c:out value="${comment.body()}"/></div>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>

    <div style="padding:16px 20px; border-top:1px solid var(--border)">
        <form method="post"
              action="${pageContext.request.contextPath}/issues/${issue.issueId()}/comments">
            <div class="form-row">
                <label for="body">Add a comment</label>
                <textarea name="body" id="body" required
                          placeholder="Reproduced on Safari 17."></textarea>
            </div>
            <button type="submit" class="btn">Post comment</button>
        </form>
    </div>
</div>

<jsp:include page="../layout/foot.jsp"/>
