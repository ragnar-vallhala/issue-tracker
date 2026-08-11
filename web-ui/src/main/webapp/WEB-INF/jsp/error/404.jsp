<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Not found · Issue Tracker</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="error-shell">
    <div class="error-code">404</div>
    <h1 class="error-title">That does not exist</h1>
    <p class="error-detail">
        <c:out value="${empty apiMessage
            ? 'The project, issue or page you asked for could not be found. It may have been deleted.'
            : apiMessage}"/>
    </p>
    <a class="btn" href="${pageContext.request.contextPath}/">Back to your dashboard</a>
</div>
</body>
</html>
