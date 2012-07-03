<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="../head.jsp" %>
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/reset/reset.css">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/fonts/fonts.css">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/grid/grid.css">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/base/base.css">
    <meta http-equiv="refresh" content="300">
</head>
<body>

<div style="margin-left: auto; margin-right: auto;width:10em">
    <h1 style="text-align: center;">&euro;${model.sumToday}</h1>

    <div style="white-space: nowrap; text-align:center;">
        <span title="Sum yesterday">Y <b>&euro;${model.sumYesterday}</b></span> &nbsp;&nbsp;
        <span title="Daily average this month">M <b>&euro;${model.dayAverageThisMonth}</b></span>
    </div>
</div>
</body>
</html>