<%-- Project list (FR-UI-09). --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Projects"/>
<c:set var="activeNav" value="projects"/>
<jsp:include page="../layout/head.jsp"/>

<div class="page-head">
    <div>
        <h1>Projects</h1>
        <p class="subtitle">Projects you own.</p>
    </div>
    <a class="btn" href="${pageContext.request.contextPath}/owner/projects/new">New project</a>
</div>

<div class="card card-tight">
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
                            <a class="btn-link"
                               href="${pageContext.request.contextPath}/owner/projects/${project.projectId()}/edit">
                                Edit</a>
                            &nbsp;·&nbsp;
                            <%-- A confirmation page, not a one-click delete: this cascades
                                 into two other databases and cannot be undone. --%>
                            <a class="btn-link" style="color:var(--danger)"
                               href="${pageContext.request.contextPath}/owner/projects/${project.projectId()}/delete">
                                Delete</a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="../layout/foot.jsp"/>
