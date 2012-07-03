<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="../head.jsp" %>
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/reset/reset.css">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/fonts/fonts.css">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/grid/grid.css">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/base/base.css">
</head>
<body>

<h1>Resend Subsonic license key</h1>

<c:if test="${empty model.email}">
    <p>Have you purchased a Subsonic license but lost the license key?</p>

    <p>Enter your email address below to have it resent to you.</p>
</c:if>

<c:if test="${not empty model.email and not model.valid}">
    <p>Sorry, no license key is associated to ${model.email}. Did you use a different email address when
        creating the payment on PayPal?</p>
</c:if>

<c:choose>
    <c:when test="${model.valid}">
        <p>Your license key has been sent to ${model.email}. Didn't get it? Please remember to check your spam
            folder.</p>
    </c:when>
    <c:otherwise>
        <form method="post" action="requestLicense.view">
            <label>Email address
                <input type="text" size="30" name="email">
            </label>
            <input type="submit" value="Send license key">
        </form>
    </c:otherwise>
</c:choose>

</body>
</html>