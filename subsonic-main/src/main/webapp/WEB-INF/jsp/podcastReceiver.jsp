<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/scripts.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/prototype.js"/>"></script>
</head>
<body class="mainframe bgcolor1">

<script type="text/javascript" language="javascript">
    var channelCount = ${fn:length(model.channels)};

    function downloadSelected() {
        location.href = "podcastReceiverAdmin.view?downloadChannel=" + getSelectedChannels() +
                        "&downloadEpisode=" + getSelectedEpisodes() +
                        "&expandedChannels=" + getExpandedChannels();
    }

    function deleteSelected() {
        if (confirm("<fmt:message key="podcastreceiver.confirmdelete"/>")) {
            location.href = "podcastReceiverAdmin.view?deleteChannel=" + getSelectedChannels() +
            "&deleteEpisode=" + getSelectedEpisodes() +
            "&expandedChannels=" + getExpandedChannels();
        }
    }

    function refreshChannels() {
        location.href = "podcastReceiverAdmin.view?refresh=" +
                        "&expandedChannels=" + getExpandedChannels();
    }

    function refreshPage() {
        location.href = "podcastReceiver.view?expandedChannels=" + getExpandedChannels();
    }

    function toggleEpisodes(channelIndex) {
        for (var i = 0; i < episodeCount; i++) {
            var row = $("episodeRow" + i);
            if (row.title == "channel" + channelIndex) {
                row.toggle();
                $("channelExpanded" + channelIndex).checked = row.visible() ? "checked" : "";
            }
        }
    }

    function toggleAllEpisodes(visible) {
        for (var i = 0; i < episodeCount; i++) {
            var row = $("episodeRow" + i);
            if (visible) {
                row.show();
            } else {
                row.hide();
            }
        }
        for (i = 0; i < channelCount; i++) {
            $("channelExpanded" + i).checked = visible ? "checked" : "";
        }
    }

    function getExpandedChannels() {
        var result = "";
        for (var i = 0; i < channelCount; i++) {
            var checkbox = $("channelExpanded" + i);
            if (checkbox.checked) {
                result += (checkbox.value + " ");
            }
        }
        return result;
    }

    function getSelectedChannels() {
        var result = "";
        for (var i = 0; i < channelCount; i++) {
            var checkbox = $("channel" + i);
            if (checkbox.checked) {
                result += (checkbox.value + " ");
            }
        }
        return result;
    }

    function getSelectedEpisodes() {
        var result = "";
        for (var i = 0; i < episodeCount; i++) {
            var checkbox = $("episode" + i);
            if (checkbox.checked) {
                result += (checkbox.value + " ");
            }
        }
        return result;
    }
</script>

<h1>
    <img src="<spring:theme code="podcastLargeImage"/>" alt=""/>
    <fmt:message key="podcastreceiver.title"/>
</h1>

<table><tr>
    <td style="padding-right:2em"><div class="forward"><a href="javascript:toggleAllEpisodes(true)"><fmt:message key="podcastreceiver.expandall"/></a></div></td>
    <td style="padding-right:2em"><div class="forward"><a href="javascript:toggleAllEpisodes(false)"><fmt:message key="podcastreceiver.collapseall"/></a></div></td>
</tr></table>

<table style="border-collapse:collapse;white-space:nowrap">

    <c:set var="episodeCount" value="0"/>

    <c:forEach items="${model.channels}" var="channel" varStatus="i">

        <c:set var="title" value="${channel.key.title}"/>
        <c:if test="${empty title}">
            <c:set var="title" value="${channel.key.url}"/>
        </c:if>

        <c:set var="channelExpanded" value="false"/>
        <c:forEach items="${model.expandedChannels}" var="expandedChannelId">
            <c:if test="${expandedChannelId eq channel.key.id}">
                <c:set var="channelExpanded" value="true"/>
            </c:if>
        </c:forEach>

        <tr style="margin:0;padding:0;border:0">
            <td style="padding-top:1em">
                <input type="checkbox" class="checkbox" id="channel${i.index}" value="${channel.key.id}"/>
                <input type="checkbox" class="checkbox" id="channelExpanded${i.index}" value="${channel.key.id}" style="display:none"
                       <c:if test="${channelExpanded}">checked="checked"</c:if>/>
            </td>
            <td colspan="6" style="padding-left:0.25em;padding-top:1em">
                <a href="javascript:toggleEpisodes(${i.index})">
                    <span title="${title}"><b><str:truncateNicely upper="40">${title}</str:truncateNicely></b></span>
                    (${fn:length(channel.value)})
                </a>
            </td>
            <td style="padding-left:1.5em;padding-top:1em;text-align:center;">
                <span class="detail"><fmt:message key="podcastreceiver.status.${fn:toLowerCase(channel.key.status)}"/></span>
            </td>
            <td style="padding-left:1.5em;padding-top:1em">
                <c:choose>
                    <c:when test="${channel.key.status eq 'ERROR'}">
                        <span class="detail warning" title="${channel.key.errorMessage}"><str:truncateNicely upper="100">${channel.key.errorMessage}</str:truncateNicely></span>
                    </c:when>
                    <c:otherwise>
                        <span class="detail" title="${channel.key.description}"><str:truncateNicely upper="100">${channel.key.description}</str:truncateNicely></span>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>

        <c:set var="class" value=""/>

        <c:forEach items="${channel.value}" var="episode" varStatus="j">

            <c:choose>
                <c:when test="${empty class}">
                    <c:set var="class" value="class='bgcolor2'"/>
                </c:when>
                <c:otherwise>
                    <c:set var="class" value=""/>
                </c:otherwise>
            </c:choose>
            <tr title="channel${i.index}" id="episodeRow${episodeCount}" style="margin:0;padding:0;border:0;display:${channelExpanded ? "table-row" : "none"}">

                <td><input type="checkbox" class="checkbox" id="episode${episodeCount}" value="${episode.id}"/></td>

                <c:choose>
                    <c:when test="${empty episode.path}">
                        <td ${class} colspan="3"/>
                    </c:when>
                    <c:otherwise>
                        <c:import url="playAddDownload.jsp">
                            <c:param name="id" value="${episode.mediaFileId}"/>
                            <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                            <c:param name="addEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                            <c:param name="downloadEnabled" value="false"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>
                    </c:otherwise>
                </c:choose>

                <c:set var="episodeCount" value="${episodeCount + 1}"/>


                <sub:url value="main.view" var="mainUrl">
                    <sub:param name="path" value="${episode.path}"/>
                </sub:url>


                <td ${class} style="padding-left:0.6em">
                    <span title="${episode.title}">
                        <c:choose>
                            <c:when test="${empty episode.path}">
                                <str:truncateNicely upper="40">${episode.title}</str:truncateNicely>
                            </c:when>
                            <c:otherwise>
                                <a target="main" href="${mainUrl}"><str:truncateNicely upper="40">${episode.title}</str:truncateNicely></a>
                            </c:otherwise>
                        </c:choose>
                    </span>
                </td>

                <td ${class} style="padding-left:1.5em">
                    <span class="detail">${episode.duration}</span>
                </td>

                <td ${class} style="padding-left:1.5em">
                    <span class="detail"><fmt:formatDate value="${episode.publishDate}" dateStyle="medium"/></span>
                </td>

                <td ${class} style="padding-left:1.5em;text-align:center">
                    <span class="detail">
                    <c:choose>
                        <c:when test="${episode.status eq 'DOWNLOADING'}">
                            <fmt:formatNumber type="percent" value="${episode.completionRate}"/>
                        </c:when>
                        <c:otherwise>
                            <fmt:message key="podcastreceiver.status.${fn:toLowerCase(episode.status)}"/>
                        </c:otherwise>
                    </c:choose>
                    </span>
                </td>

                <td ${class} style="padding-left:1.5em">
                    <c:choose>
                        <c:when test="${episode.status eq 'ERROR'}">
                            <span class="detail warning" title="${episode.errorMessage}"><str:truncateNicely upper="100">${episode.errorMessage}</str:truncateNicely></span>
                        </c:when>
                        <c:otherwise>
                            <span class="detail" title="${episode.description}"><str:truncateNicely upper="100">${episode.description}</str:truncateNicely></span>
                        </c:otherwise>
                    </c:choose>
                </td>

            </tr>
        </c:forEach>
    </c:forEach>
</table>

<script type="text/javascript" language="javascript">
    var episodeCount = ${episodeCount};
</script>

<table style="padding-top:1em"><tr>
    <c:if test="${model.user.podcastRole}">
        <td style="padding-right:2em"><div class="forward"><a href="javascript:downloadSelected()"><fmt:message key="podcastreceiver.downloadselected"/></a></div></td>
        <td style="padding-right:2em"><div class="forward"><a href="javascript:deleteSelected()"><fmt:message key="podcastreceiver.deleteselected"/></a></div></td>
        <td style="padding-right:2em"><div class="forward"><a href="javascript:refreshChannels()"><fmt:message key="podcastreceiver.check"/></a></div></td>
    </c:if>
    <td style="padding-right:2em"><div class="forward"><a href="javascript:refreshPage()"><fmt:message key="podcastreceiver.refresh"/></a></div></td>
    <c:if test="${model.user.adminRole}">
        <td style="padding-right:2em"><div class="forward"><a href="podcastSettings.view?"><fmt:message key="podcastreceiver.settings"/></a></div></td>
    </c:if>
</tr></table>

<c:if test="${model.user.podcastRole}">
    <form method="post" action="podcastReceiverAdmin.view?">
        <input type="hidden" name="expandedChannels" value=""/>
        <table>
            <tr>
                <td><fmt:message key="podcastreceiver.subscribe"/></td>
                <td><input type="text" name="add" value="http://" style="width:30em" onclick="select()"/></td>
                <td><input type="submit" value="<fmt:message key="common.ok"/>"/></td>
            </tr>
        </table>
    </form>
</c:if>


</body>
</html>