<%@ include file="include.jsp" %>

<!--[if lt IE 7.]>
<script defer type="text/javascript" src="<c:url value="/script/pngfix.js"/>"></script>
<![endif]-->
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<c:set var="styleSheet"><spring:theme code="styleSheet"/></c:set>
<c:set var="faviconImage"><spring:theme code="faviconImage"/></c:set>
<link rel="stylesheet" href="<c:url value="/${styleSheet}"/>" type="text/css">
<link rel="shortcut icon" href="<c:url value="/${faviconImage}"/>" type="text/css">
<title>Subsonic</title>
