<%--
  Delete confirmation (FR-UI-11, FR-PRJ-12).

  This page exists because the delete cascades: the project's issues go with it, and
  their comments go with those. That reaches three databases, cannot be undone, and is
  not something to discover after clicking. The issue count is stated explicitly rather
  than described in the abstract.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Delete project"/>
<c:set var="activeNav" value="projects"/>
<jsp:include page="../layout/head.jsp"/>

<div class="page-head">
    <div>
        <h1>Delete this project?</h1>
        <p class="subtitle">This cannot be undone.</p>
    </div>
</div>

<div class="card form-narrow">
    <div class="detail-grid" style="margin-bottom:20px">
        <div class="detail-item">
            <div class="detail-label">Project</div>
            <div class="detail-value"><strong><c:out value="${project.projectName()}"/></strong></div>
        </div>
        <div class="detail-item">
            <div class="detail-label">Issues that will be deleted</div>
            <div class="detail-value"><strong>${issueCount}</strong></div>
        </div>
    </div>

    <div class="flash flash-error">
        <c:choose>
            <c:when test="${issueCount eq 0}">
                This project has no issues. Deleting it removes the project only.
            </c:when>
            <c:when test="${issueCount eq 1}">
                Deleting <strong><c:out value="${project.projectName()}"/></strong> will also
                permanently delete <strong>1 issue</strong> and every comment on it.
            </c:when>
            <c:otherwise>
                Deleting <strong><c:out value="${project.projectName()}"/></strong> will also
                permanently delete <strong>${issueCount} issues</strong> and every comment
                on them.
            </c:otherwise>
        </c:choose>
    </div>

    <form method="post"
          action="${pageContext.request.contextPath}/owner/projects/${project.projectId()}/delete">
        <div class="actions">
            <button type="submit" class="btn btn-danger">
                <c:choose>
                    <c:when test="${issueCount eq 0}">Delete project</c:when>
                    <c:otherwise>Delete project and ${issueCount} issue<c:if
                            test="${issueCount ne 1}">s</c:if></c:otherwise>
                </c:choose>
            </button>
            <a class="btn btn-secondary"
               href="${pageContext.request.contextPath}/owner/projects">Cancel</a>
        </div>
    </form>
</div>

<jsp:include page="../layout/foot.jsp"/>
