<%-- The Assignee's full issue list, with filters (FR-UI-16). --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="My issues"/>
<c:set var="activeNav" value="issues"/>
<jsp:include page="../layout/head.jsp"/>

<div class="page-head">
    <div>
        <h1>My issues</h1>
        <p class="subtitle">
            Showing ${fn:length(issues)} of ${totalCount} issues assigned to you.
        </p>
    </div>
</div>

<div class="card">
    <form method="get" action="${pageContext.request.contextPath}/assignee/issues"
          class="filters">
        <div class="form-row">
            <label for="status">Status</label>
            <select name="status" id="status">
                <option value="">All</option>
                <c:forEach var="entry" items="${statuses}">
                    <option value="${entry.key}"
                            <c:if test="${entry.key eq selectedStatus}">selected</c:if>>
                        <c:out value="${entry.value}"/>
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="form-row">
            <label for="priority">Priority</label>
            <select name="priority" id="priority">
                <option value="">All</option>
                <c:forEach var="entry" items="${priorities}">
                    <option value="${entry.key}"
                            <c:if test="${entry.key eq selectedPriority}">selected</c:if>>
                        <c:out value="${entry.value}"/>
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="form-row">
            <label for="projectId">Project</label>
            <select name="projectId" id="projectId">
                <option value="">All</option>
                <c:forEach var="entry" items="${projectNames}">
                    <option value="${entry.key}"
                            <c:if test="${entry.key eq selectedProjectId}">selected</c:if>>
                        <c:out value="${entry.value}"/>
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="actions">
            <button type="submit" class="btn">Filter</button>
            <a class="btn btn-secondary"
               href="${pageContext.request.contextPath}/assignee/issues">Clear</a>
        </div>
    </form>
</div>

<div class="card card-tight">
    <c:choose>
        <c:when test="${empty issues}">
            <div class="empty">
                <c:choose>
                    <c:when test="${totalCount eq 0}">Nothing is assigned to you yet.</c:when>
                    <c:otherwise>No issues match these filters.</c:otherwise>
                </c:choose>
            </div>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th>#</th>
                    <th>Summary</th>
                    <th>Project</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Sprint</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="issue" items="${issues}">
                    <tr>
                        <td class="mono muted">${issue.issueId()}</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/issues/${issue.issueId()}">
                                <c:out value="${issue.summary()}"/>
                            </a>
                        </td>
                        <td class="muted">
                            <c:out value="${projectNames[issue.projectId()]}"/>
                        </td>
                        <td>
                            <span class="badge badge-${issue.statusSlug()}">
                                <c:out value="${issue.statusLabel()}"/>
                            </span>
                        </td>
                        <td>
                            <span class="badge badge-${issue.prioritySlug()}">
                                <c:out value="${issue.priorityLabel()}"/>
                            </span>
                        </td>
                        <td class="muted">
                            <c:out value="${empty issue.sprint() ? '—' : issue.sprint()}"/>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="../layout/foot.jsp"/>
