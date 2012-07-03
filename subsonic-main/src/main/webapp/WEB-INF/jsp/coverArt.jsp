<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<%--
PARAMETERS
  albumId: ID of album.
  coverArtSize: Height and width of cover art.
  albumName: Album name to display as caption and img alt.
  showLink: Whether to make the cover art image link to the album page.
  showZoom: Whether to display a link for zooming the cover art.
  showChange: Whether to display a link for changing the cover art.
  showCaption: Whether to display the album name as a caption below the image.
  appearAfter: Fade in after this many milliseconds, or nil if no fading in should happen.
--%>
<c:choose>
    <c:when test="${empty param.coverArtSize}">
        <c:set var="size" value="auto"/>
    </c:when>
    <c:otherwise>
        <c:set var="size" value="${param.coverArtSize + 8}px"/>
    </c:otherwise>
</c:choose>

<c:set var="opacity" value="${empty param.appearAfter ? 1 : 0}"/>

<div style="width:${size}; max-width:${size}; height:${size}; max-height:${size}" title="${param.albumName}">
    <sub:url value="main.view" var="mainUrl">
        <sub:param name="id" value="${param.albumId}"/>
    </sub:url>

    <sub:url value="/coverArt.view" var="coverArtUrl">
        <c:if test="${not empty param.coverArtSize}">
            <sub:param name="size" value="${param.coverArtSize}"/>
        </c:if>
        <sub:param name="id" value="${param.albumId}"/>
    </sub:url>
    <sub:url value="/coverArt.view" var="zoomCoverArtUrl">
        <sub:param name="id" value="${param.albumId}"/>
    </sub:url>

    <str:randomString count="5" type="alphabet" var="divId"/>
    <div class="outerpair1" id="${divId}" style="display:none">
        <div class="outerpair2">
            <div class="shadowbox">
                <div class="innerbox">
                    <c:choose>
                        <c:when test="${param.showLink}"><a href="${mainUrl}" title="${param.albumName}"></c:when>
                        <c:when test="${param.showZoom}"><a href="${zoomCoverArtUrl}" rel="zoom" title="${param.albumName}"></c:when>
                    </c:choose>
                        <img src="${coverArtUrl}" alt="${param.albumName}">
                        <c:if test="${param.showLink or param.showZoom}"></a></c:if>
                </div>
            </div>
        </div>
    </div>
    <c:if test="${not empty param.appearAfter}">
        <script type="text/javascript">
            if (window.addEventListener) {
                window.addEventListener('load', function() {
                    setTimeout("$('#${divId}').fadeIn(500)", ${param.appearAfter});
                }, false);
            }
        </script>
    </c:if>
</div>

<div style="text-align:right; padding-right: 8px;">
    <c:if test="${param.showChange}">
        <sub:url value="/changeCoverArt.view" var="changeCoverArtUrl">
            <sub:param name="id" value="${param.albumId}"/>
        </sub:url>
        <a class="detail" href="${changeCoverArtUrl}"><fmt:message key="coverart.change"/></a>
    </c:if>

    <c:if test="${param.showZoom and param.showChange}">
        |
    </c:if>

    <c:if test="${param.showZoom}">
        <a class="detail" rel="zoom" title="${param.albumName}" href="${zoomCoverArtUrl}"><fmt:message key="coverart.zoom"/></a>
    </c:if>

    <c:if test="${not param.showZoom and not param.showChange and param.showCaption}">
        <span class="detail"><str:truncateNicely upper="17">${param.albumName}</str:truncateNicely></span>
    </c:if>
</div>