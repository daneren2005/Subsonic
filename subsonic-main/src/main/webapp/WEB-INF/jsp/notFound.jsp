<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>

<body class="mainframe bgcolor1">

<h1>
    <img src="<spring:theme code="errorImage"/>" alt=""/>
    <fmt:message key="notFound.title"/>
</h1>

<fmt:message key="notFound.text"/>

<div class="forward" style="float:left;padding-right:10pt"><a href="javascript:top.location.reload(true)"><fmt:message key="notFound.reload"/></a></div>
<div class="forward" style="float:left"><a href="musicFolderSettings.view"><fmt:message key="notFound.scan"/></a></div>

</body>
</html>