<%--
  Project create/edit (FR-UI-10).

  There is no owner input. The owner is taken from the session server-side, so it cannot
  be reassigned by editing a hidden field.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<c:set var="creating" value="${mode eq 'create'}"/>
<c:set var="pageTitle" value="${creating ? 'New project' : 'Edit project'}"/>
<c:set var="activeNav" value="projects"/>
<jsp:include page="../layout/head.jsp"/>

<div class="page-head">
    <div>
        <h1><c:out value="${creating ? 'New project' : 'Edit project'}"/></h1>
        <p class="subtitle">You will be recorded as the project owner.</p>
    </div>
</div>

<div class="card form-narrow">
    <form:form modelAttribute="projectForm" method="post"
               action="${creating
                   ? pageContext.request.contextPath.concat('/owner/projects')
                   : pageContext.request.contextPath.concat('/owner/projects/').concat(projectForm.projectId)}">

        <div class="form-row">
            <label for="projectName">Project name</label>
            <form:input path="projectName" id="projectName"/>
            <div class="hint">Must be unique across the system.</div>
            <form:errors path="projectName" cssClass="field-error" element="span"/>
        </div>

        <div class="form-grid">
            <div class="form-row">
                <label for="startDate">Start date</label>
                <form:input path="startDate" id="startDate" type="date"/>
                <form:errors path="startDate" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="endDate">End date</label>
                <form:input path="endDate" id="endDate" type="date"/>
                <div class="hint">Optional.</div>
                <form:errors path="endDate" cssClass="field-error" element="span"/>
            </div>
        </div>

        <div class="actions" style="margin-top:8px">
            <button type="submit" class="btn">
                <c:out value="${creating ? 'Create project' : 'Save changes'}"/>
            </button>
            <a class="btn btn-secondary"
               href="${pageContext.request.contextPath}/owner/projects">Cancel</a>
        </div>
    </form:form>
</div>

<jsp:include page="../layout/foot.jsp"/>
