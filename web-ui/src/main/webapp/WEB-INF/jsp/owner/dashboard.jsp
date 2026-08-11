<%-- Project Owner dashboard (FR-UI-08). --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Dashboard"/>
<c:set var="activeNav" value="dashboard"/>
<jsp:include page="../layout/head.jsp"/>

<c:set var="total" value="${summary.issueCount()}"/>
<c:set var="done" value="${empty summary.byStatus()['DONE'] ? 0 : summary.byStatus()['DONE']}"/>
<c:set var="critical" value="${empty summary.byPriority()['CRITICAL']
                               ? 0 : summary.byPriority()['CRITICAL']}"/>

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

<div class="grid grid-4">
    <div class="stat">
        <div class="stat-value">${summary.projectCount()}</div>
        <div class="stat-label">Projects</div>
    </div>
    <div class="stat">
        <div class="stat-value">${total}</div>
        <div class="stat-label">Issues</div>
    </div>
    <div class="stat">
        <div class="stat-value">${done}</div>
        <div class="stat-label">Done</div>
    </div>
    <%-- The one figure that earns the accent rule: it is the only number here that
         means someone should do something today. --%>
    <div class="stat ${critical > 0 ? 'stat-accent' : ''}">
        <div class="stat-value">${critical}</div>
        <div class="stat-label">Critical</div>
    </div>
</div>

<c:if test="${total > 0}">
    <div class="card">
        <h2>Backlog composition</h2>
        <%-- One stacked bar rather than four counts: an unbalanced backlog is visible
             here in a way a column of numbers never is. --%>
        <div class="meter">
            <c:forEach var="key" items="${statusOrder}">
                <c:set var="count" value="${empty summary.byStatus()[key]
                                            ? 0 : summary.byStatus()[key]}"/>
                <c:if test="${count > 0}">
                    <span class="seg-${fn:toLowerCase(fn:replace(key, '_', '-'))}"
                          style="width:${count * 100 / total}%"
                          title="${statusLabels[key]}: ${count}"></span>
                </c:if>
            </c:forEach>
        </div>
        <div class="meter-key">
            <c:forEach var="key" items="${statusOrder}">
                <c:set var="count" value="${empty summary.byStatus()[key]
                                            ? 0 : summary.byStatus()[key]}"/>
                <span>
                    <i class="key-${fn:toLowerCase(fn:replace(key, '_', '-'))}"></i>
                    <c:out value="${statusLabels[key]}"/> · ${count}
                </span>
            </c:forEach>
        </div>
    </div>
</c:if>

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
                        <td class="num">${project.startDate()}</td>
                        <td class="num">
                            <c:out value="${empty project.endDate() ? '—' : project.endDate()}"/>
                        </td>
                        <td class="num">${issueCounts[project.projectId()]}</td>
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
    <h2>Recently updated</h2>
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
                    <th>Assignee</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Updated</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="issue" items="${recentIssues}">
                    <tr>
                        <td class="num">${issue.issueId()}</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/issues/${issue.issueId()}">
                                <c:out value="${issue.summary()}"/>
                            </a>
                        </td>
                        <td>
                            <jsp:include page="../layout/person.jsp">
                                <jsp:param name="personName"
                                           value="${userNames[issue.assigneeId()]}"/>
                            </jsp:include>
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
                        <td class="num">
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
