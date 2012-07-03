<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/dwr/util.js'/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playlistService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/starService.js"/>"></script>
    <script type="text/javascript" language="javascript">

        var playlist;
        var songs;

        function init() {
            dwr.engine.setErrorHandler(null);
            $("#dialog-edit").dialog({resizable: true, width:400, position: 'top', modal: true, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.save"/>": function() {
                        $(this).dialog("close");
                        var name = $("#newName").val();
                        var comment = $("#newComment").val();
                        var isPublic = $("#newPublic").is(":checked");
                        $("#name").html(name);
                        $("#comment").html(comment);
                        playlistService.updatePlaylist(playlist.id, name, comment, isPublic, function (playlistInfo){playlistCallback(playlistInfo); top.left.updatePlaylists()});
                    },
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    }
                }});

            $("#dialog-delete").dialog({resizable: false, height: 170, position: 'top', modal: true, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.delete"/>": function() {
                        $(this).dialog("close");
                        playlistService.deletePlaylist(playlist.id, function (){top.left.updatePlaylists(); location = "home.view";});
                    },
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    }
                }});
            getPlaylist();
        }

        function getPlaylist() {
            playlistService.getPlaylist(${model.playlist.id}, playlistCallback);
        }

        function playlistCallback(playlistInfo) {
            this.playlist = playlistInfo.playlist;
            this.songs = playlistInfo.entries;

            if (songs.length == 0) {
                $("#empty").show();
            } else {
                $("#empty").hide();
            }


            $("#songCount").html(playlist.fileCount);
            $("#duration").html(playlist.durationAsString);

            if (playlist.public) {
                $("#shared").html("<fmt:message key="playlist2.shared"/>");
            } else {
                $("#shared").html("<fmt:message key="playlist2.notshared"/>");
            }

            // Delete all the rows except for the "pattern" row
            dwr.util.removeAllRows("playlistBody", { filter:function(tr) {
                return (tr.id != "pattern");
            }});

            // Create a new set cloned from the pattern row
            for (var i = 0; i < songs.length; i++) {
                var song  = songs[i];
                var id = i + 1;
                dwr.util.cloneNode("pattern", { idSuffix:id });
                if (song.starred) {
                    $("#starSong" + id).attr("src", "<spring:theme code='ratingOnImage'/>");
                } else {
                    $("#starSong" + id).attr("src", "<spring:theme code='ratingOffImage'/>");
                }
                if ($("#title" + id)) {
                    $("#title" + id).html(truncate(song.title));
                    $("#title" + id).attr("title", song.title);
                }
                if ($("#album" + id)) {
                    $("#album" + id).html(truncate(song.album));
                    $("#album" + id).attr("title", song.album);
                    $("#albumUrl" + id).attr("href", "main.view?id=" + song.id);
                }
                if ($("#artist" + id)) {
                    $("#artist" + id).html(truncate(song.artist));
                    $("#artist" + id).attr("title", song.artist);
                }
                if ($("#duration" + id)) {
                    $("#duration" + id).html(song.durationAsString);
                }

                $("#pattern" + id).addClass((i % 2 == 0) ? "bgcolor2" : "bgcolor1");
                $("#pattern" + id).show();
            }
        }

        function truncate(s) {
            if (s == null) {
                return s;
            }
            var cutoff = 30;

            if (s.length > cutoff) {
                return s.substring(0, cutoff) + "...";
            }
            return s;
        }

        function onPlay(index) {
            top.playQueue.onPlay(songs[index].id);
        }
        function onPlayAll() {
            top.playQueue.onPlayPlaylist(playlist.id);
        }
        function onAdd(index) {
            top.playQueue.onAdd(songs[index].id);
        }
        function onStar(index) {
            playlistService.toggleStar(playlist.id, index, playlistCallback);
        }
        function onRemove(index) {
            playlistService.remove(playlist.id, index, function (playlistInfo){playlistCallback(playlistInfo); top.left.updatePlaylists()});
        }
        function onUp(index) {
            playlistService.up(playlist.id, index, playlistCallback);
        }
        function onDown(index) {
            playlistService.down(playlist.id, index, playlistCallback);
        }
        function onEditPlaylist() {
            $("#dialog-edit").dialog("open");
        }
        function onDeletePlaylist() {
            $("#dialog-delete").dialog("open");
        }

    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">

<h1 id="name">${model.playlist.name}</h1>
<h2>
    <a href="#" onclick="onPlayAll();"><fmt:message key="common.play"/></a>

    <c:if test="${model.user.downloadRole}">
        <c:url value="download.view" var="downloadUrl"><c:param name="playlist" value="${model.playlist.id}"/></c:url>
        | <a href="${downloadUrl}"><fmt:message key="common.download"/></a>
    </c:if>
    <c:if test="${model.editAllowed}">
        | <a href="#" onclick="onEditPlaylist();"><fmt:message key="common.edit"/></a>
        | <a href="#" onclick="onDeletePlaylist();"><fmt:message key="common.delete"/></a>
    </c:if>
    <c:url value="exportPlaylist.view" var="exportUrl"><c:param name="id" value="${model.playlist.id}"/></c:url>
    | <a href="${exportUrl}"><fmt:message key="playlist2.export"/></a>

</h2>

<div id="comment" class="detail" style="padding-top:0.2em">${model.playlist.comment}</div>

<div class="detail" style="padding-top:0.2em">
    <fmt:message key="playlist2.created">
        <fmt:param>${model.playlist.username}</fmt:param>
        <fmt:param><fmt:formatDate type="date" dateStyle="long" value="${model.playlist.created}"/></fmt:param>
    </fmt:message>.
    <span id="shared"></span>.
    <span id="songCount"></span> <fmt:message key="playlist2.songs"/> (<span id="duration"></span>)
</div>

<div style="height:0.7em"></div>

<p id="empty" style="display: none;"><em><fmt:message key="playlist2.empty"/></em></p>

<table style="border-collapse:collapse;white-space:nowrap">
    <tbody id="playlistBody">
    <tr id="pattern" style="display:none;margin:0;padding:0;border:0">
        <td class="bgcolor1"><a href="#">
            <img id="starSong" onclick="onStar(this.id.substring(8) - 1)" src="<spring:theme code="ratingOffImage"/>" alt="" title=""></a></td>
        <td class="bgcolor1"><a href="#">
            <img id="play" src="<spring:theme code="playImage"/>" alt="<fmt:message key="common.play"/>" title="<fmt:message key="common.play"/>"
                 onclick="onPlay(this.id.substring(4) - 1)"></a></td>
        <td class="bgcolor1"><a href="#">
            <img id="add" src="<spring:theme code="addImage"/>" alt="<fmt:message key="common.add"/>" title="<fmt:message key="common.add"/>"
                 onclick="onAdd(this.id.substring(3) - 1)"></a></td>

        <td style="padding-right:0.25em"></td>
        <td style="padding-right:1.25em"><span id="title">Title</span></td>
        <td style="padding-right:1.25em"><a id="albumUrl" target="main"><span id="album" class="detail">Album</span></a></td>
        <td style="padding-right:1.25em"><span id="artist" class="detail">Artist</span></td>
        <td style="padding-right:1.25em;text-align:right;"><span id="duration" class="detail">Duration</span></td>

        <c:if test="${model.editAllowed}">
            <td class="bgcolor1"><a href="#">
                <img id="removeSong" onclick="onRemove(this.id.substring(10) - 1)" src="<spring:theme code="removeImage"/>"
                     alt="<fmt:message key="playlist.remove"/>" title="<fmt:message key="playlist.remove"/>"></a></td>
            <td class="bgcolor1"><a href="#">
                <img id="up" onclick="onUp(this.id.substring(2) - 1)" src="<spring:theme code="upImage"/>"
                     alt="<fmt:message key="playlist.up"/>" title="<fmt:message key="playlist.up"/>"></a></td>
            <td class="bgcolor1"><a href="#">
                <img id="down" onclick="onDown(this.id.substring(4) - 1)" src="<spring:theme code="downImage"/>"
                     alt="<fmt:message key="playlist.down"/>" title="<fmt:message key="playlist.down"/>"></a></td>
        </c:if>

    </tr>
    </tbody>
</table>

<div id="dialog-delete" title="<fmt:message key="common.confirm"/>" style="display: none;">
    <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>
        <fmt:message key="playlist2.confirmdelete"/></p>
</div>

<div id="dialog-edit" title="<fmt:message key="common.edit"/>" style="display: none;">
    <form>
        <label for="newName" style="display:block;"><fmt:message key="playlist2.name"/></label>
        <input type="text" name="newName" id="newName" value="${model.playlist.name}" class="ui-widget-content"
               style="display:block;width:95%;"/>
        <label for="newComment" style="display:block;margin-top:1em"><fmt:message key="playlist2.comment"/></label>
        <input type="text" name="newComment" id="newComment" value="${model.playlist.comment}" class="ui-widget-content"
               style="display:block;width:95%;"/>
        <input type="checkbox" name="newPublic" id="newPublic" ${model.playlist.public ? "checked='checked'" : ""} style="margin-top:1.5em" class="ui-widget-content"/>
        <label for="newPublic"><fmt:message key="playlist2.public"/></label>
    </form>
</div>

</body></html>