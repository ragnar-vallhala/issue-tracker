<%-- Project detail with its issues (FR-UI-12). --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${project.projectName()}"/>
<c:set var="activeNav" value="projects"/>
<jsp:include page="../layout/head.jsp"/>

<div class="page-head">
    <div>
        <h1><c:out value="${project.projectName()}"/></h1>
        <p class="subtitle">
            ${project.startDate()}
            <c:if test="${not empty project.endDate()}"> — ${project.endDate()}</c:if>
        </p>
    </div>
    <div class="actions">
        <a class="btn btn-secondary"
           href="${pageContext.request.contextPath}/owner/projects/${project.projectId()}/edit">
            Edit</a>
        <a class="btn"
           href="${pageContext.request.contextPath}/owner/issues/new?projectId=${project.projectId()}">
            New issue</a>
    </div>
</div>

<div class="card card-tight">
    <h2>Issues in this project</h2>

    <c:choose>
        <c:when test="${empty issues}">
            <div class="empty">
                No issues in this project yet.
                <a href="${pageContext.request.contextPath}/owner/issues/new?projectId=${project.projectId()}">
                    Create the first one</a>.
            </div>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th>#</th>
                    <th>Summary</th>
                    <th>Type</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Assignee</th>
                    <th>Points</th>
                    <th></th>
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
                        <td class="muted"><c:out value="${issue.typeLabel()}"/></td>
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
                        <td>
                            <%-- Names come from a single users call made for the page, not
                                 one lookup per row (DESIGN 8.6). --%>
                            <c:choose>
                                <c:when test="${empty issue.assigneeId()}">
                                    <span class="muted">Unassigned</span>
                                </c:when>
                                <c:otherwise>
                                    <c:out value="${userNames[issue.assigneeId()]}"/>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="muted">
                            <c:out value="${empty issue.storyPoints() ? '—' : issue.storyPoints()}"/>
                        </td>
                        <td class="table-actions">
                            <a class="btn-link"
                               href="${pageContext.request.contextPath}/owner/issues/${issue.issueId()}/edit">
                                Edit</a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="../layout/foot.jsp"/>
