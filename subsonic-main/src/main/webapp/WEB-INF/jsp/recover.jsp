<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe bgcolor1" onload="document.getElementById('usernameOrEmail').focus()">

<form action="recover.view" method="POST">
    <div class="bgcolor2" style="border:1px solid black; padding:20px 50px 20px 50px; margin-top:100px">

        <div style="margin-left: auto; margin-right: auto; width: 45em">

            <h1><fmt:message key="recover.title"/></h1>
            <p style="padding-top: 1em; padding-bottom: 0.5em"><fmt:message key="recover.text"/></p>
            <input type="text" id="usernameOrEmail" name="usernameOrEmail" style="width:18em;margin-right: 1em">
            <input name="submit" type="submit" value="<fmt:message key="recover.send"/>">

            <c:if test="${not empty model.sentTo}">
                <p style="padding-top: 1em"><fmt:message key="recover.success"><fmt:param value="${model.sentTo}"/></fmt:message></p>
            </c:if>

            <c:if test="${not empty model.error}">
                <p style="padding-top: 1em" class="warning"><fmt:message key="${model.error}"/></p>
            </c:if>

            <div class="back" style="margin-top: 1.5em"><a href="login.view"><fmt:message key="common.back"/></a></div>

        </div>
    </div>
</form>
</body>
</html>
