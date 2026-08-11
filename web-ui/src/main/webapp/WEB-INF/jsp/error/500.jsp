<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Something went wrong · Issue Tracker</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="error-shell">
    <div class="error-code">500</div>
    <h1 class="error-title">Something went wrong</h1>
    <%-- The message, never a stack trace: the detail is in the server log where it is
         useful, not on the page where it is only alarming (NFR-08). --%>
    <p class="error-detail">
        <c:out value="${empty apiMessage
            ? 'An unexpected error occurred. Please try again.' : apiMessage}"/>
    </p>
    <a class="btn" href="${pageContext.request.contextPath}/">Back to your dashboard</a>
</div>
</body>
</html>
