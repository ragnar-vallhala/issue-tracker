<%--
  503 (FR-UI-20). This is what an inter-service failure looks like to a person: a service
  is down, the answer is unknown, and the honest response is to say so and offer a retry -
  rather than render an empty list that would read as "you have no work".
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Temporarily unavailable · Issue Tracker</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="error-shell">
    <div class="error-code">503</div>
    <h1 class="error-title">Temporarily unavailable</h1>
    <p class="error-detail">
        <c:out value="${empty apiMessage
            ? 'One of the services this page needs is not responding right now.'
            : apiMessage}"/>
        <br>
        Nothing has been lost — try again in a moment.
    </p>
    <div class="actions" style="justify-content:center">
        <a class="btn" href="javascript:location.reload()">Try again</a>
        <a class="btn btn-secondary" href="${pageContext.request.contextPath}/">Dashboard</a>
    </div>
</div>
</body>
</html>
