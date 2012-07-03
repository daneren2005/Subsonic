<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/prototype.js"/>"></script>
</head>
<body>

<div id="tFfBc" style="opacity:0;">
    <img src="/coverArt.view?size=200" alt="">
</div>

<ul>
    <li><a href="#" onclick="new Effect.Opacity('tFfBc', { from: 1, to: 0 }); return false;">Hide this box</a></li>
    <li><a href="#" onclick="new Effect.Opacity('tFfBc', { from: 0, to: 1 }); return false;">Show this box</a></li>
</ul>

</body>
</html>