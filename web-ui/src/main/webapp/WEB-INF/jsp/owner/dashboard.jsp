<%-- Project Owner dashboard (FR-UI-08). --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Dashboard"/>
<c:set var="activeNav" value="dashboard"/>
<jsp:include page="../layout/head.jsp"/>

<div class="page-head">
    <div>
        <h1>Dashboard</h1>
        <p class="subtitle">Your projects and the work inside them.</p>
    </div>
    <div class="actions">
        <a class="btn btn-secondary" href="${pageContext.request.contextPath}/owner/projects/new">
            New project
        </a>
        <a class="btn" href="${pageContext.request.contextPath}/owner/issues/new">New issue</a>
    </div>
</div>

<div class="grid grid-4" style="margin-bottom:22px">
    <div class="stat">
        <div class="stat-value">${summary.projectCount()}</div>
        <div class="stat-label">Projects</div>
    </div>
    <div class="stat">
        <div class="stat-value">${summary.issueCount()}</div>
        <div class="stat-label">Issues</div>
    </div>
    <div class="stat">
        <div class="stat-value">
            <c:out value="${empty summary.byStatus()['DONE'] ? 0 : summary.byStatus()['DONE']}"/>
        </div>
        <div class="stat-label">Done</div>
    </div>
    <div class="stat">
        <div class="stat-value">
            <c:out value="${empty summary.byPriority()['CRITICAL']
                            ? 0 : summary.byPriority()['CRITICAL']}"/>
        </div>
        <div class="stat-label">Critical</div>
    </div>
</div>

<div class="grid grid-2">
    <div class="card card-tight">
        <h2>Issues by status</h2>
        <c:choose>
            <c:when test="${summary.issueCount() eq 0}">
                <div class="empty">No issues yet.</div>
            </c:when>
            <c:otherwise>
                <table>
                    <tbody>
                    <c:forEach var="entry" items="${summary.byStatus()}">
                        <tr>
                            <td>
                                <span class="badge badge-${fn:toLowerCase(
                                        fn:replace(entry.key, '_', '-'))}">
                                    <c:out value="${entry.key}"/>
                                </span>
                            </td>
                            <td class="table-actions"><strong>${entry.value}</strong></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card card-tight">
        <h2>Issues by priority</h2>
        <c:choose>
            <c:when test="${summary.issueCount() eq 0}">
                <div class="empty">No issues yet.</div>
            </c:when>
            <c:otherwise>
                <table>
                    <tbody>
                    <c:forEach var="entry" items="${summary.byPriority()}">
                        <tr>
                            <td>
                                <span class="badge badge-${fn:toLowerCase(entry.key)}">
                                    <c:out value="${entry.key}"/>
                                </span>
                            </td>
                            <td class="table-actions"><strong>${entry.value}</strong></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<div class="card card-tight">
    <h2>Your projects</h2>
    <c:choose>
        <c:when test="${empty projects}">
            <div class="empty">
                You have no projects yet.
                <a href="${pageContext.request.contextPath}/owner/projects/new">Create one</a>.
            </div>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th>Project</th>
                    <th>Starts</th>
                    <th>Ends</th>
                    <th>Issues</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="project" items="${projects}">
                    <tr>
                        <td>
                            <a href="${pageContext.request.contextPath}/owner/projects/${project.projectId()}">
                                <c:out value="${project.projectName()}"/>
                            </a>
                        </td>
                        <td class="muted">${project.startDate()}</td>
                        <td class="muted">
                            <c:out value="${empty project.endDate() ? '—' : project.endDate()}"/>
                        </td>
                        <td>${issueCounts[project.projectId()]}</td>
                        <td class="table-actions">
                            <a class="btn btn-secondary btn-small"
                               href="${pageContext.request.contextPath}/owner/issues/new?projectId=${project.projectId()}">
                                Add issue
                            </a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<div class="card card-tight">
    <h2>Recently updated issues</h2>
    <c:choose>
        <c:when test="${empty recentIssues}">
            <div class="empty">Nothing has been updated yet.</div>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th>#</th>
                    <th>Summary</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Updated</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="issue" items="${recentIssues}">
                    <tr>
                        <td class="mono muted">${issue.issueId()}</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/issues/${issue.issueId()}">
                                <c:out value="${issue.summary()}"/>
                            </a>
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
                            <c:if test="${not empty issue.lastUpdatedOn()}">
                                ${issue.lastUpdatedOn().toLocalDate()}
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="../layout/foot.jsp"/>
