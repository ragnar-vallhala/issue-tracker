<%--
  The confirmation the case study specifies by name: "Your account is created
  successfully", with a hyperlink to login. The message comes from the API rather than
  being written here, so there is one copy of that wording in the system.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Account created · Issue Tracker</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<div class="auth-shell">
    <div class="auth-card">
        <h1>Welcome, <c:out value="${created.user().name()}"/></h1>

        <div class="flash flash-success" style="margin-top:16px">
            <c:out value="${created.message()}"/>
        </div>

        <div class="detail-grid" style="margin: 20px 0">
            <div class="detail-item">
                <div class="detail-label">Email</div>
                <div class="detail-value"><c:out value="${created.user().email()}"/></div>
            </div>
            <div class="detail-item">
                <div class="detail-label">Username</div>
                <div class="detail-value mono"><c:out value="${created.user().username()}"/></div>
            </div>
            <div class="detail-item">
                <div class="detail-label">Role</div>
                <div class="detail-value"><c:out value="${created.user().roleLabel()}"/></div>
            </div>
        </div>

        <a class="btn" style="width:100%; text-align:center"
           href="${pageContext.request.contextPath}/login">Sign in</a>
    </div>
</div>

</body>
</html>
