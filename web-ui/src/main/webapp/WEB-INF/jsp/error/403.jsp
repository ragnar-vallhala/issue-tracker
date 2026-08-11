<%--
  403 (FR-UI-20). Forwarded to, never redirected to: redirecting a role mismatch to the
  other dashboard produces an infinite bounce when a user's role and their bookmark
  disagree.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Not permitted · Issue Tracker</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="error-shell">
    <div class="error-code">403</div>
    <h1 class="error-title">That page belongs to a different role</h1>
    <p class="error-detail">
        <c:out value="${empty apiMessage
            ? 'Your account does not have access to this part of the application.'
            : apiMessage}"/>
    </p>
    <a class="btn" href="${pageContext.request.contextPath}/">Back to your dashboard</a>
</div>
</body>
</html>
