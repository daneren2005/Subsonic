<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="../head.jsp" %>
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/reset/reset.css">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/fonts/fonts.css">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/grid/grid.css">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/base/base.css">
</head><body>

<h1>Database query</h1>

<form method="post" action="db.view">
    <textarea rows="10" cols="80" name="query" style="margin-top:1em">${model.query}</textarea>
    <input type="submit" value="OK">
    <input type="hidden" name="p" value="${model.p}">
</form>


<c:if test="${not empty model.result}">
    <h1 style="margin-top:2em">Result</h1>

    <table>
        <c:forEach items="${model.result}" var="row" varStatus="loopStatus">

            <c:if test="${loopStatus.count == 1}">
                <tr>
                    <c:forEach items="${row}" var="entry">
                        <td>${entry.key}</td>
                    </c:forEach>
                </tr>
            </c:if>
            <tr>
                <c:forEach items="${row}" var="entry">
                    <td>${entry.value}</td>
                </c:forEach>
            </tr>
        </c:forEach>

    </table>
</c:if>

<c:if test="${not empty model.error}">
    <h1 style="margin-top:2em">Error</h1>

    <p>
        ${model.error}
    </p>
</c:if>

</body></html>