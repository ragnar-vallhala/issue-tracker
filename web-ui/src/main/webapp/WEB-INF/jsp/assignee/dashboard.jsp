<%--
  Assignee dashboard (FR-UI-15).

  A board grouped by status. Every column is rendered even when empty - a board that
  hides its empty columns makes "nothing in review" look like a rendering failure.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Dashboard"/>
<c:set var="activeNav" value="dashboard"/>
<jsp:include page="../layout/head.jsp"/>

<div class="page-head">
    <div>
        <h1>Your work</h1>
        <p class="subtitle">Issues assigned to you.</p>
    </div>
    <a class="btn btn-secondary"
       href="${pageContext.request.contextPath}/assignee/issues">View as list</a>
</div>

<div class="grid grid-4" style="margin-bottom:22px">
    <div class="stat">
        <div class="stat-value">${summary.issueCount()}</div>
        <div class="stat-label">Assigned to you</div>
    </div>
    <div class="stat">
        <div class="stat-value">${summary.projectCount()}</div>
        <div class="stat-label">Across projects</div>
    </div>
    <div class="stat">
        <div class="stat-value">${currentSprintCount}</div>
        <div class="stat-label">In a sprint</div>
    </div>
    <div class="stat">
        <div class="stat-value">
            <c:out value="${empty summary.byStatus()['DONE'] ? 0 : summary.byStatus()['DONE']}"/>
        </div>
        <div class="stat-label">Done</div>
    </div>
</div>

<c:choose>
    <c:when test="${summary.issueCount() eq 0}">
        <div class="card">
            <div class="empty">
                Nothing is assigned to you yet.<br>
                A Project Owner will assign work to you here.
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="board">
            <c:forEach var="column" items="${board}">
                <div class="board-column">
                    <h3>
                        <span><c:out value="${statusLabels[column.key]}"/></span>
                        <em>${fn:length(column.value)}</em>
                    </h3>

                    <c:choose>
                        <c:when test="${empty column.value}">
                            <div class="muted" style="font-size:13px; padding:6px 2px">
                                Nothing here.
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="issue" items="${column.value}">
                                <div class="board-card">
                                    <a href="${pageContext.request.contextPath}/issues/${issue.issueId()}">
                                        <c:out value="${issue.summary()}"/>
                                    </a>
                                    <div class="meta">
                                        <span class="badge badge-${issue.prioritySlug()}">
                                            <c:out value="${issue.priorityLabel()}"/>
                                        </span>
                                        <span><c:out value="${projectNames[issue.projectId()]}"/></span>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/foot.jsp"/>
