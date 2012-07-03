<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/dwr/util.js'/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/starService.js"/>"></script>
    <script type="text/javascript" language="javascript">

        function toggleStar(mediaFileId, imageId) {
            if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOnImage"/>") != -1) {
                $(imageId).attr("src", "<spring:theme code="ratingOffImage"/>");
                starService.unstar(mediaFileId);
            }
            else if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOffImage"/>") != -1) {
                $(imageId).attr("src", "<spring:theme code="ratingOnImage"/>");
                starService.star(mediaFileId);
            }
        }
    </script>
</head>
<body class="mainframe bgcolor1">

<h1>
    <fmt:message key="starred.title"/>
</h1>

<c:if test="${empty model.artists and empty model.albums and empty model.songs}">
    <p style="padding-top: 1em"><em><fmt:message key="starred.empty"/></em></p>
</c:if>

<c:if test="${not empty model.artists}">
    <h2><fmt:message key="search.hits.artists"/></h2>
    <table style="border-collapse:collapse">
        <c:forEach items="${model.artists}" var="artist" varStatus="loopStatus">

            <sub:url value="/main.view" var="mainUrl">
                <sub:param name="path" value="${artist.path}"/>
            </sub:url>

            <tr>
                <c:import url="playAddDownload.jsp">
                    <c:param name="id" value="${artist.id}"/>
                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                    <c:param name="addEnabled" value="${model.user.streamRole and (not model.partyModeEnabled or not artist.directory)}"/>
                    <c:param name="downloadEnabled" value="${model.user.downloadRole and not model.partyModeEnabled}"/>
                    <c:param name="starEnabled" value="true"/>
                    <c:param name="starred" value="${not empty artist.starredDate}"/>
                    <c:param name="asTable" value="true"/>
                </c:import>
                <td ${loopStatus.count % 2 == 1 ? "class='bgcolor2'" : ""} style="padding-left:0.25em;padding-right:1.25em">
                    <a href="${mainUrl}">${artist.name}</a>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>

<c:if test="${not empty model.albums}">
    <h2><fmt:message key="search.hits.albums"/></h2>
    <table style="border-collapse:collapse">
        <c:forEach items="${model.albums}" var="album" varStatus="loopStatus">

            <sub:url value="/main.view" var="mainUrl">
                <sub:param name="path" value="${album.path}"/>
            </sub:url>

            <tr>
                <c:import url="playAddDownload.jsp">
                    <c:param name="id" value="${album.id}"/>
                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                    <c:param name="addEnabled" value="${model.user.streamRole and (not model.partyModeEnabled or not album.directory)}"/>
                    <c:param name="downloadEnabled" value="${model.user.downloadRole and not model.partyModeEnabled}"/>
                    <c:param name="starEnabled" value="true"/>
                    <c:param name="starred" value="${not empty album.starredDate}"/>
                    <c:param name="asTable" value="true"/>
                </c:import>

                <td ${loopStatus.count % 2 == 1 ? "class='bgcolor2'" : ""} style="padding-left:0.25em;padding-right:1.25em">
                    <a href="${mainUrl}">${album.albumName}</a>
                </td>

                <td ${loopStatus.count % 2 == 1 ? "class='bgcolor2'" : ""} style="padding-right:0.25em">
                    <span class="detail">${album.artist}</span>
                </td>
            </tr>

        </c:forEach>
    </table>
</c:if>

<c:if test="${not empty model.songs}">
    <h2><fmt:message key="search.hits.songs"/></h2>
    <table style="border-collapse:collapse">
        <c:forEach items="${model.songs}" var="song" varStatus="loopStatus">

            <sub:url value="/main.view" var="mainUrl">
                <sub:param name="path" value="${song.parentPath}"/>
            </sub:url>

            <tr>
                <c:import url="playAddDownload.jsp">
                    <c:param name="id" value="${song.id}"/>
                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                    <c:param name="addEnabled" value="${model.user.streamRole and (not model.partyModeEnabled or not song.directory)}"/>
                    <c:param name="downloadEnabled" value="${model.user.downloadRole and not model.partyModeEnabled}"/>
                    <c:param name="starEnabled" value="true"/>
                    <c:param name="starred" value="${not empty song.starredDate}"/>
                    <c:param name="video" value="${song.video and model.player.web}"/>
                    <c:param name="asTable" value="true"/>
                </c:import>

                <td ${loopStatus.count % 2 == 1 ? "class='bgcolor2'" : ""} style="padding-left:0.25em;padding-right:1.25em">
                        ${song.title}
                </td>

                <td ${loopStatus.count % 2 == 1 ? "class='bgcolor2'" : ""} style="padding-right:1.25em">
                    <a href="${mainUrl}"><span class="detail">${song.albumName}</span></a>
                </td>

                <td ${loopStatus.count % 2 == 1 ? "class='bgcolor2'" : ""} style="padding-right:0.25em">
                    <span class="detail">${song.artist}</span>
                </td>
            </tr>

        </c:forEach>
    </table>
</c:if>

</body></html>