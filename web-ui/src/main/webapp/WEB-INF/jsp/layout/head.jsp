<%--
  Shared page shell: masthead, role-appropriate navigation and the flash region
  (FR-UI-21). Every authenticated page includes this and layout/foot.jsp.

  Expects: pageTitle, activeNav, and the SessionUser as "user".
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${pageTitle}"/> · Issue Tracker</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<header class="masthead">
    <div class="masthead-inner">
        <a class="brand" href="${pageContext.request.contextPath}/">Issue Tracker</a>
        <c:if test="${not empty user}">
            <div class="masthead-user">
                <span><strong><c:out value="${user.name()}"/></strong></span>
                <span class="role-chip">
                    <c:choose>
                        <c:when test="${user.isProjectOwner()}">Project Owner</c:when>
                        <c:otherwise>Assignee</c:otherwise>
                    </c:choose>
                </span>
                <a href="${pageContext.request.contextPath}/logout">Sign out</a>
            </div>
        </c:if>
    </div>
</header>

<c:if test="${not empty user}">
    <nav class="nav">
        <div class="nav-inner">
            <c:choose>
                <c:when test="${user.isProjectOwner()}">
                    <a href="${pageContext.request.contextPath}/owner/dashboard"
                       class="${activeNav eq 'dashboard' ? 'active' : ''}">Dashboard</a>
                    <a href="${pageContext.request.contextPath}/owner/projects"
                       class="${activeNav eq 'projects' ? 'active' : ''}">Projects</a>
                    <a href="${pageContext.request.contextPath}/owner/issues/new"
                       class="${activeNav eq 'new-issue' ? 'active' : ''}">New issue</a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/assignee/dashboard"
                       class="${activeNav eq 'dashboard' ? 'active' : ''}">Dashboard</a>
                    <a href="${pageContext.request.contextPath}/assignee/issues"
                       class="${activeNav eq 'issues' ? 'active' : ''}">My issues</a>
                </c:otherwise>
            </c:choose>
        </div>
    </nav>
</c:if>

<main>

<%-- One flash region for every page, so a success or failure always lands in the
     same place rather than wherever the individual page happens to put it. --%>
<c:if test="${not empty flash}">
    <div class="flash flash-success"><c:out value="${flash}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="flash flash-error"><c:out value="${error}"/></div>
</c:if>
<c:if test="${not empty notice}">
    <div class="flash flash-notice"><c:out value="${notice}"/></div>
</c:if>
