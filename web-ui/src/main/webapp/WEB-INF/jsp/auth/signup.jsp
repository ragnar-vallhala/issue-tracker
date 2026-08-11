<%--
  Sign up (FR-UI-06).

  Note the "profile" field: a short text description of the person, not a file picker or
  URL box. The case study prose says "profile image" throughout, but the reference
  workbook's actual values are biographies, and the workbook wins (SRS A-16).
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Create an account · Issue Tracker</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<div class="auth-shell">
    <div class="auth-card">
        <h1>Create an account</h1>
        <p class="subtitle">Choose the role you will be working in.</p>

        <form:form modelAttribute="signUpForm" method="post"
                   action="${pageContext.request.contextPath}/signup">

            <div class="form-row">
                <label for="name">Name</label>
                <form:input path="name" id="name" autocomplete="name"/>
                <form:errors path="name" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="email">Email</label>
                <form:input path="email" id="email" type="email" autocomplete="email"/>
                <form:errors path="email" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="password">Password</label>
                <form:password path="password" id="password" autocomplete="new-password"/>
                <div class="hint">At least 8 characters.</div>
                <form:errors path="password" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="profile">Profile</label>
                <form:input path="profile" id="profile"
                            placeholder="Front-end developer focused on accessibility"/>
                <div class="hint">A short description of what you do.</div>
                <form:errors path="profile" cssClass="field-error" element="span"/>
            </div>

            <div class="form-row">
                <label for="role">Role</label>
                <form:select path="role" id="role">
                    <form:option value="" label="Choose a role"/>
                    <form:option value="PROJECT_OWNER" label="Project Owner"/>
                    <form:option value="ASSIGNEE" label="Assignee"/>
                </form:select>
                <div class="hint">
                    Project Owners create projects and issues; Assignees work on them.
                </div>
                <form:errors path="role" cssClass="field-error" element="span"/>
            </div>

            <button type="submit" class="btn" style="width:100%">Create account</button>
        </form:form>

        <div class="auth-footer">
            Already registered?
            <a href="${pageContext.request.contextPath}/login">Sign in</a>
        </div>
    </div>
</div>

</body>
</html>
