<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ include file="include.jsp" %>

<%--
PARAMETERS
  id: ID of file.
  video: Whether the file is a video (default false).
  playEnabled: Whether the current user is allowed to play songs (default true).
  addEnabled: Whether the current user is allowed to add songs to the playlist (default true).
  downloadEnabled: Whether the current user is allowed to download songs (default false).
  starEnabled: Whether to show star/unstar controls (default false).
  starred: Whether the file is currently starred.
  asTable: Whether to put the images in td tags.
--%>

<sub:url value="/download.view" var="downloadUrl">
    <sub:param name="id" value="${param.id}"/>
</sub:url>
<c:if test="${param.starEnabled}">
    <c:if test="${param.asTable}"><td></c:if>
    <a href="#" onclick="toggleStar(${param.id}, '#starImage${param.id}'); return false;">
        <c:choose>
            <c:when test="${param.starred}">
                <img id="starImage${param.id}" src="<spring:theme code="ratingOnImage"/>" alt="">
            </c:when>
            <c:otherwise>
                <img id="starImage${param.id}" src="<spring:theme code="ratingOffImage"/>" alt="">
            </c:otherwise>
        </c:choose>
    </a>
    <c:if test="${param.asTable}"></td></c:if>
</c:if>

<c:if test="${param.asTable}"><td></c:if>
<c:if test="${empty param.playEnabled or param.playEnabled}">
    <c:choose>
        <c:when test="${param.video}">
            <sub:url value="/videoPlayer.view" var="videoUrl">
                <sub:param name="id" value="${param.id}"/>
            </sub:url>
            <a href="${videoUrl}" target="main">
                <img src="<spring:theme code="playImage"/>" alt="<fmt:message key="common.play"/>" title="<fmt:message key="common.play"/>"></a>
        </c:when>
        <c:otherwise>
            <a href="javascript:noop()" onclick="top.playQueue.onPlay(${param.id});">
                <img src="<spring:theme code="playImage"/>" alt="<fmt:message key="common.play"/>" title="<fmt:message key="common.play"/>"></a>
        </c:otherwise>
    </c:choose>
</c:if>
<c:if test="${param.asTable}"></td></c:if>

<c:if test="${param.asTable}"><td></c:if>
<c:if test="${(empty param.addEnabled or param.addEnabled) and not param.video}">
    <a href="javascript:noop()" onclick="top.playQueue.onAdd(${param.id});">
        <img src="<spring:theme code="addImage"/>" alt="<fmt:message key="common.add"/>" title="<fmt:message key="common.add"/>"></a>
</c:if>
<c:if test="${param.asTable}"></td></c:if>

<c:if test="${param.asTable}"><td></c:if>
<c:if test="${param.downloadEnabled}">
    <a href="${downloadUrl}">
        <img src="<spring:theme code="downloadImage"/>" alt="<fmt:message key="common.download"/>" title="<fmt:message key="common.download"/>"></a>
</c:if>
<c:if test="${param.asTable}"></td></c:if>
