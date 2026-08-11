<%--
  Issue create/edit (FR-UI-13, FR-UI-14).

  The assignee dropdown lists only ASSIGNEE users: the Issue Service checks an assignee's
  role before accepting one, so offering Project Owners here would build a request
  guaranteed to be rejected.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<c:set var="creating" value="${mode eq 'create'}"/>
<c:set var="pageTitle" value="${creating ? 'New issue' : 'Edit issue'}"/>
<c:set var="activeNav" value="${creating ? 'new-issue' : 'projects'}"/>
<jsp:include page="../layout/head.jsp"/>

<div class="page-head">
    <div>
        <h1><c:out value="${creating ? 'New issue' : 'Edit issue'}"/></h1>
        <p class="subtitle">
            <c:choose>
                <c:when test="${creating}">You will be recorded as the reporter.</c:when>
                <c:otherwise>Issue #${issueForm.issueId}</c:otherwise>
            </c:choose>
        </p>
    </div>
</div>

<div class="card">
    <form:form modelAttribute="issueForm" method="post"
               action="${creating
                   ? pageContext.request.contextPath.concat('/owner/issues')
                   : pageContext.request.contextPath.concat('/owner/issues/').concat(issueForm.issueId)}">

        <div class="form-row">
            <label for="summary">Summary</label>
            <form:input path="summary" id="summary"
                        placeholder="Profile cache not updating after changes"/>
            <form:errors path="summary" cssClass="field-error" element="span"/>
        </div>

        <div class="form-row">
            <label for="description">Description</label>
            <form:textarea path="description" id="description"/>
            <form:errors path="description" cssClass="field-error" element="span"/>
        </div>

        <div class="form-grid">
            <div class="form-row">
                <label for="projectId">Project</label>
                <form:select path="projectId" id="projectId">
                    <form:option value="" label="Choose a project"/>
                    <c:forEach var="project" items="${projects}">
                        <form:option value="${project.projectId()}"
                                     label="${project.projectName()}"/>
                    </c:forEach>
                </form:select>
                <form:errors path="projectId" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="assigneeId">Assignee</label>
                <form:select path="assigneeId" id="assigneeId">
                    <form:option value="" label="Unassigned"/>
                    <c:forEach var="assignee" items="${assignees}">
                        <form:option value="${assignee.userId()}" label="${assignee.name()}"/>
                    </c:forEach>
                </form:select>
                <div class="hint">Only Assignees can hold issues.</div>
                <form:errors path="assigneeId" cssClass="field-error" element="span"/>
            </div>
        </div>

        <div class="form-grid">
            <div class="form-row">
                <label for="status">Status</label>
                <form:select path="status" id="status">
                    <c:forEach var="entry" items="${statuses}">
                        <form:option value="${entry.key}" label="${entry.value}"/>
                    </c:forEach>
                </form:select>
                <form:errors path="status" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="priority">Priority</label>
                <form:select path="priority" id="priority">
                    <c:forEach var="entry" items="${priorities}">
                        <form:option value="${entry.key}" label="${entry.value}"/>
                    </c:forEach>
                </form:select>
                <form:errors path="priority" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="type">Type</label>
                <form:select path="type" id="type">
                    <c:forEach var="entry" items="${types}">
                        <form:option value="${entry.key}" label="${entry.value}"/>
                    </c:forEach>
                </form:select>
                <form:errors path="type" cssClass="field-error" element="span"/>
            </div>
        </div>

        <div class="form-grid">
            <div class="form-row">
                <label for="storyPoints">Story points</label>
                <form:input path="storyPoints" id="storyPoints" type="number" min="0"/>
                <form:errors path="storyPoints" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="sprint">Sprint</label>
                <form:input path="sprint" id="sprint" placeholder="Sprint 42"/>
                <form:errors path="sprint" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="tags">Tags</label>
                <form:input path="tags" id="tags" placeholder="profile,cache,update"/>
                <div class="hint">Comma-separated.</div>
                <form:errors path="tags" cssClass="field-error" element="span"/>
            </div>
        </div>

        <div class="actions" style="margin-top:8px">
            <button type="submit" class="btn">
                <c:out value="${creating ? 'Create issue' : 'Save changes'}"/>
            </button>
            <c:choose>
                <c:when test="${creating}">
                    <a class="btn btn-secondary"
                       href="${pageContext.request.contextPath}/owner/dashboard">Cancel</a>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-secondary"
                       href="${pageContext.request.contextPath}/issues/${issueForm.issueId}">
                        Cancel</a>
                </c:otherwise>
            </c:choose>
        </div>
    </form:form>
</div>

<c:if test="${not creating}">
    <div class="card">
        <h2>Delete this issue</h2>
        <p class="muted" style="margin-top:0">
            Deleting an issue also deletes every comment on it. This cannot be undone.
        </p>
        <form method="post"
              action="${pageContext.request.contextPath}/owner/issues/${issueForm.issueId}/delete"
              onsubmit="return confirm('Delete this issue and all of its comments?');">
            <input type="hidden" name="projectId" value="${issueForm.projectId}">
            <button type="submit" class="btn btn-danger btn-small">Delete issue</button>
        </form>
    </div>
</c:if>

<jsp:include page="../layout/foot.jsp"/>
