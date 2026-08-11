<%--
  Renders a person as a monogram plus their name.

  Initials rather than a photograph: the profile column holds a text biography, not an
  image (SRS A-16), so there is no picture to show — and a consistent monogram reads
  better in a dense table than a placeholder silhouette would.

  Usage:
    <jsp:include page="../layout/person.jsp">
        <jsp:param name="personName" value="${userNames[issue.assigneeId()]}"/>
    </jsp:include>
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:choose>
    <c:when test="${empty param.personName}">
        <span class="muted">Unassigned</span>
    </c:when>
    <c:otherwise>
        <c:set var="nameParts" value="${fn:split(param.personName, ' ')}"/>
        <span class="person">
            <%-- First and last initial; a single-word name contributes just the one. --%>
            <span class="avatar" aria-hidden="true"><%--
                --%>${fn:toUpperCase(fn:substring(nameParts[0], 0, 1))}<%--
                --%><c:if test="${fn:length(nameParts) > 1}"><%--
                    --%>${fn:toUpperCase(fn:substring(nameParts[fn:length(nameParts) - 1], 0, 1))}<%--
                --%></c:if><%--
            --%></span>
            <span><c:out value="${param.personName}"/></span>
        </span>
    </c:otherwise>
</c:choose>
