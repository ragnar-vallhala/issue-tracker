<%--
  Login (FR-UI-07). Email and password only - the case study's login section also lists
  name, profile and role, but that is its sign-up list duplicated (SRS A-03).
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in · Issue Tracker</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<div class="auth-shell">
    <div class="auth-card">
        <div class="auth-mark">Issue Tracking System</div>
        <h1>Sign in</h1>
        <p class="subtitle">Welcome back.</p>

        <c:if test="${not empty notice}">
            <div class="flash flash-notice"><c:out value="${notice}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="flash flash-error"><c:out value="${error}"/></div>
        </c:if>

        <form:form modelAttribute="loginForm" method="post"
                   action="${pageContext.request.contextPath}/login">

            <div class="form-row">
                <label for="email">Email</label>
                <form:input path="email" id="email" type="email" autocomplete="username"/>
                <form:errors path="email" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="password">Password</label>
                <form:password path="password" id="password" autocomplete="current-password"/>
                <form:errors path="password" cssClass="field-error" element="span"/>
            </div>

            <button type="submit" class="btn btn-block">Sign in</button>
        </form:form>

        <div class="auth-footer">
            No account yet?
            <a href="${pageContext.request.contextPath}/signup">Create one</a>
        </div>
    </div>
</div>

</body>
</html>
