<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/dwr/interface/nowPlayingService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playQueueService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playlistService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/util.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/swfobject.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/webfx/range.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/webfx/timer.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/webfx/slider.js"/>"></script>
    <link type="text/css" rel="stylesheet" href="<c:url value="/script/webfx/luna.css"/>">
</head>

<body class="bgcolor2 playlistframe" onload="init()">

<script type="text/javascript" language="javascript">
    var player = null;
    var songs = null;
    var currentAlbumUrl = null;
    var currentStreamUrl = null;
    var startPlayer = false;
    var repeatEnabled = false;
    var slider = null;

    function init() {
        dwr.engine.setErrorHandler(null);
        startTimer();

        $("#dialog-select-playlist").dialog({resizable: true, height: 220, position: 'top', modal: true, autoOpen: false,
            buttons: {
                "<fmt:message key="common.cancel"/>": function() {
                    $(this).dialog("close");
                }
            }});

    <c:choose>
    <c:when test="${model.player.web}">
        createPlayer();
    </c:when>
    <c:otherwise>
        getPlayQueue();
    </c:otherwise>
    </c:choose>
    }

    function startTimer() {
        <!-- Periodically check if the current song has changed. -->
        nowPlayingService.getNowPlayingForCurrentPlayer(nowPlayingCallback);
        setTimeout("startTimer()", 10000);
    }

    function nowPlayingCallback(nowPlayingInfo) {
        if (nowPlayingInfo != null && nowPlayingInfo.streamUrl != currentStreamUrl) {
            getPlayQueue();
            if (currentAlbumUrl != nowPlayingInfo.albumUrl && top.main.updateNowPlaying) {
                top.main.location.replace("nowPlaying.view?");
                currentAlbumUrl = nowPlayingInfo.albumUrl;
            }
        <c:if test="${not model.player.web}">
            currentStreamUrl = nowPlayingInfo.streamUrl;
            updateCurrentImage();
        </c:if>
        }
    }

    function createPlayer() {
        var flashvars = {
            backcolor:"<spring:theme code="backgroundColor"/>",
            frontcolor:"<spring:theme code="textColor"/>",
            id:"player1"
        };
        var params = {
            allowfullscreen:"true",
            allowscriptaccess:"always"
        };
        var attributes = {
            id:"player1",
            name:"player1"
        };
        swfobject.embedSWF("<c:url value="/flash/jw-player-5.6.swf"/>", "placeholder", "340", "24", "9.0.0", false, flashvars, params, attributes);
    }

    function playerReady(thePlayer) {
        player = document.getElementById("player1");
        player.addModelListener("STATE", "stateListener");
        getPlayQueue();
    }

    function stateListener(obj) { // IDLE, BUFFERING, PLAYING, PAUSED, COMPLETED
        if (obj.newstate == "COMPLETED") {
            onNext(repeatEnabled);
        }
    }

    function getPlayQueue() {
        playQueueService.getPlayQueue(playQueueCallback);
    }

    function onClear() {
        var ok = true;
    <c:if test="${model.partyMode}">
        ok = confirm("<fmt:message key="playlist.confirmclear"/>");
    </c:if>
        if (ok) {
            playQueueService.clear(playQueueCallback);
        }
    }
    function onStart() {
        playQueueService.start(playQueueCallback);
    }
    function onStop() {
        playQueueService.stop(playQueueCallback);
    }
    function onGain(gain) {
        playQueueService.setGain(gain);
    }
    function onSkip(index) {
    <c:choose>
    <c:when test="${model.player.web}">
        skip(index);
    </c:when>
    <c:otherwise>
        currentStreamUrl = songs[index].streamUrl;
        playQueueService.skip(index, playQueueCallback);
    </c:otherwise>
    </c:choose>
    }
    function onNext(wrap) {
        var index = parseInt(getCurrentSongIndex()) + 1;
        if (wrap) {
            index = index % songs.length;
        }
        skip(index);
    }
    function onPrevious() {
        skip(parseInt(getCurrentSongIndex()) - 1);
    }
    function onPlay(id) {
        startPlayer = true;
        playQueueService.play(id, playQueueCallback);
    }
    function onPlayPlaylist(id) {
        startPlayer = true;
        playQueueService.playPlaylist(id, playQueueCallback);
    }
    function onPlayRandom(id, count) {
        startPlayer = true;
        playQueueService.playRandom(id, count, playQueueCallback);
    }
    function onAdd(id) {
        startPlayer = false;
        playQueueService.add(id, playQueueCallback);
    }
    function onShuffle() {
        playQueueService.shuffle(playQueueCallback);
    }
    function onStar(index) {
        playQueueService.toggleStar(index, playQueueCallback);
    }
    function onRemove(index) {
        playQueueService.remove(index, playQueueCallback);
    }
    function onRemoveSelected() {
        var indexes = new Array();
        var counter = 0;
        for (var i = 0; i < songs.length; i++) {
            var index = i + 1;
            if ($("#songIndex" + index).is(":checked")) {
                indexes[counter++] = i;
            }
        }
        playQueueService.removeMany(indexes, playQueueCallback);
    }

    function onUp(index) {
        playQueueService.up(index, playQueueCallback);
    }
    function onDown(index) {
        playQueueService.down(index, playQueueCallback);
    }
    function onToggleRepeat() {
        playQueueService.toggleRepeat(playQueueCallback);
    }
    function onUndo() {
        playQueueService.undo(playQueueCallback);
    }
    function onSortByTrack() {
        playQueueService.sortByTrack(playQueueCallback);
    }
    function onSortByArtist() {
        playQueueService.sortByArtist(playQueueCallback);
    }
    function onSortByAlbum() {
        playQueueService.sortByAlbum(playQueueCallback);
    }
    function onSavePlaylist() {
        playQueueService.savePlaylist(function () {top.left.updatePlaylists();});
    }
    function onAppendPlaylist() {
        playlistService.getWritablePlaylists(playlistCallback);
    }
    function playlistCallback(playlists) {
        $("#dialog-select-playlist-list").empty();
        for (var i = 0; i < playlists.length; i++) {
            var playlist = playlists[i];
            $("<p class='dense'><b><a href='#' onclick='appendPlaylist(" + playlist.id + ")'>" + playlist.name + "</a></b></p>").appendTo("#dialog-select-playlist-list");
        }
        $("#dialog-select-playlist").dialog("open");
    }
    function appendPlaylist(playlistId) {
        $("#dialog-select-playlist").dialog("close");

        var mediaFileIds = new Array();
        for (var i = 0; i < songs.length; i++) {
            if ($("#songIndex" + (i + 1)).is(":checked")) {
                mediaFileIds.push(songs[i].id);
            }
        }
        playlistService.appendToPlaylist(playlistId, mediaFileIds, function (){top.left.updatePlaylists();});
    }

    function playQueueCallback(playQueue) {
        songs = playQueue.entries;
        repeatEnabled = playQueue.repeatEnabled;
        if ($("#start")) {
            if (playQueue.stopEnabled) {
                $("#start").hide();
                $("#stop").show();
            } else {
                $("#start").show();
                $("#stop").hide();
            }
        }

        if ($("#toggleRepeat")) {
            var text = repeatEnabled ? "<fmt:message key="playlist.repeat_on"/>" : "<fmt:message key="playlist.repeat_off"/>";
            $("#toggleRepeat").html(text);
        }

        if (songs.length == 0) {
            $("#empty").show();
        } else {
            $("#empty").hide();
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
            if ($("#trackNumber" + id)) {
                $("#trackNumber" + id).html(song.trackNumber);
            }
            if (song.starred) {
                $("#starSong" + id).attr("src", "<spring:theme code='ratingOnImage'/>");
            } else {
                $("#starSong" + id).attr("src", "<spring:theme code='ratingOffImage'/>");
            } 
            if ($("#currentImage" + id) && song.streamUrl == currentStreamUrl) {
                $("#currentImage" + id).show();
            }
            if ($("#title" + id)) {
                $("#title" + id).html(truncate(song.title));
                $("#title" + id).attr("title", song.title);
            }
            if ($("#titleUrl" + id)) {
                $("#titleUrl" + id).html(truncate(song.title));
                $("#titleUrl" + id).attr("title", song.title);
                $("#titleUrl" + id).click(function () {onSkip(this.id.substring(8) - 1)});
            }
            if ($("#album" + id)) {
                $("#album" + id).html(truncate(song.album));
                $("#album" + id).attr("title", song.album);
                $("#albumUrl" + id).attr("href", song.albumUrl);
            }
            if ($("#artist" + id)) {
                $("#artist" + id).html(truncate(song.artist));
                $("#artist" + id).attr("title", song.artist);
            }
            if ($("#genre" + id)) {
                $("#genre" + id).html(song.genre);
            }
            if ($("#year" + id)) {
                $("#year" + id).html(song.year);
            }
            if ($("#bitRate" + id)) {
                $("#bitRate" + id).html(song.bitRate);
            }
            if ($("#duration" + id)) {
                $("#duration" + id).html(song.durationAsString);
            }
            if ($("#format" + id)) {
                $("#format" + id).html(song.format);
            }
            if ($("#fileSize" + id)) {
                $("#fileSize" + id).html(song.fileSize);
            }

            $("#pattern" + id).show();
            $("#pattern" + id).addClass((i % 2 == 0) ? "bgcolor1" : "bgcolor2");
        }

        if (playQueue.sendM3U) {
            parent.frames.main.location.href="play.m3u?";
        }

        if (slider) {
            slider.setValue(playQueue.gain * 100);
        }

    <c:if test="${model.player.web}">
        triggerPlayer();
    </c:if>
    }

    function triggerPlayer() {
        if (startPlayer) {
            startPlayer = false;
            if (songs.length > 0) {
                skip(0);
            }
        }
        updateCurrentImage();
        if (songs.length == 0) {
            player.sendEvent("LOAD", new Array());
            player.sendEvent("STOP");
        }
    }

    function skip(index) {
        if (index < 0 || index >= songs.length) {
            return;
        }

        var song = songs[index];
        currentStreamUrl = song.streamUrl;
        updateCurrentImage();
        var list = new Array();
        list[0] = {
            file:song.streamUrl,
            title:song.title,
            provider:"sound"
        };

        if (song.duration != null) {
            list[0].duration = song.duration;
        }
        if (song.format == "aac" || song.format == "m4a") {
            list[0].provider = "video";
        }

        player.sendEvent("LOAD", list);
        player.sendEvent("PLAY");
    }

    function updateCurrentImage() {
        for (var i = 0; i < songs.length; i++) {
            var song  = songs[i];
            var id = i + 1;
            var image = $("#currentImage" + id);

            if (image) {
                if (song.streamUrl == currentStreamUrl) {
                    image.show();
                } else {
                    image.hide();
                }
            }
        }
    }

    function getCurrentSongIndex() {
        for (var i = 0; i < songs.length; i++) {
            if (songs[i].streamUrl == currentStreamUrl) {
                return i;
            }
        }
        return -1;
    }

    function truncate(s) {
        if (s == null) {
            return s;
        }
        var cutoff = ${model.visibility.captionCutoff};

        if (s.length > cutoff) {
            return s.substring(0, cutoff) + "...";
        }
        return s;
    }

    <!-- actionSelected() is invoked when the users selects from the "More actions..." combo box. -->
    function actionSelected(id) {
        if (id == "top") {
            return;
        } else if (id == "savePlaylist") {
            onSavePlaylist();
        } else if (id == "downloadPlaylist") {
            location.href = "download.view?player=${model.player.id}";
        } else if (id == "sharePlaylist") {
            parent.frames.main.location.href = "createShare.view?player=${model.player.id}&" + getSelectedIndexes();
        } else if (id == "sortByTrack") {
            onSortByTrack();
        } else if (id == "sortByArtist") {
            onSortByArtist();
        } else if (id == "sortByAlbum") {
            onSortByAlbum();
        } else if (id == "selectAll") {
            selectAll(true);
        } else if (id == "selectNone") {
            selectAll(false);
        } else if (id == "removeSelected") {
            onRemoveSelected();
        } else if (id == "download") {
            location.href = "download.view?player=${model.player.id}&" + getSelectedIndexes();
        } else if (id == "appendPlaylist") {
            onAppendPlaylist();
        }
        $("#moreActions").prop("selectedIndex", 0);
    }

    function getSelectedIndexes() {
        var result = "";
        for (var i = 0; i < songs.length; i++) {
            if ($("#songIndex" + (i + 1)).is(":checked")) {
                result += "i=" + i + "&";
            }
        }
        return result;
    }

    function selectAll(b) {
        for (var i = 0; i < songs.length; i++) {
            if (b) {
                $("#songIndex" + (i + 1)).attr("checked", "checked");
            } else {
                $("#songIndex" + (i + 1)).removeAttr("checked");
            }
        }
    }

</script>

<div class="bgcolor2" style="position:fixed; top:0; width:100%;padding-top:0.5em">
    <table style="white-space:nowrap;">
        <tr style="white-space:nowrap;">
            <c:if test="${model.user.settingsRole}">
                <td><select name="player" onchange="location='playQueue.view?player=' + options[selectedIndex].value;">
                    <c:forEach items="${model.players}" var="player">
                        <option ${player.id eq model.player.id ? "selected" : ""} value="${player.id}">${player.shortDescription}</option>
                    </c:forEach>
                </select></td>
            </c:if>
            <c:if test="${model.player.web}">
                <td style="width:340px; height:24px;padding-left:10px;padding-right:10px"><div id="placeholder">
                    <a href="http://www.adobe.com/go/getflashplayer" target="_blank"><fmt:message key="playlist.getflash"/></a>
                </div></td>
            </c:if>

            <c:if test="${model.user.streamRole and not model.player.web}">
                <td style="white-space:nowrap;" id="stop"><b><a href="#" onclick="onStop()"><fmt:message key="playlist.stop"/></a></b> | </td>
                <td style="white-space:nowrap;" id="start"><b><a href="#" onclick="onStart()"><fmt:message key="playlist.start"/></a></b> | </td>
            </c:if>

            <c:if test="${model.player.jukebox}">
                <td style="white-space:nowrap;">
                    <img src="<spring:theme code="volumeImage"/>" alt="">
                </td>
                <td style="white-space:nowrap;">
                    <div class="slider bgcolor2" id="slider-1" style="width:90px">
                        <input class="slider-input" id="slider-input-1" name="slider-input-1">
                    </div>
                    <script type="text/javascript">

                        var updateGainTimeoutId = 0;
                        slider = new Slider(document.getElementById("slider-1"), document.getElementById("slider-input-1"));
                        slider.onchange = function () {
                            clearTimeout(updateGainTimeoutId);
                            updateGainTimeoutId = setTimeout("updateGain()", 250);
                        };

                        function updateGain() {
                            var gain = slider.getValue() / 100.0;
                            onGain(gain);
                        }
                    </script>
                </td>
            </c:if>

            <c:if test="${model.player.web}">
                <td style="white-space:nowrap;"><a href="#" onclick="onPrevious()"><b>&laquo;</b></a></td>
                <td style="white-space:nowrap;"><a href="#" onclick="onNext(false)"><b>&raquo;</b></a> |</td>
            </c:if>

            <td style="white-space:nowrap;"><a href="#" onclick="onClear()"><fmt:message key="playlist.clear"/></a> |</td>
            <td style="white-space:nowrap;"><a href="#" onclick="onShuffle()"><fmt:message key="playlist.shuffle"/></a> |</td>

            <c:if test="${model.player.web or model.player.jukebox or model.player.external}">
                <td style="white-space:nowrap;"><a href="#" onclick="onToggleRepeat()"><span id="toggleRepeat"><fmt:message key="playlist.repeat_on"/></span></a> |</td>
            </c:if>

            <td style="white-space:nowrap;"><a href="#" onclick="onUndo()"><fmt:message key="playlist.undo"/></a> |</td>

            <c:if test="${model.user.settingsRole}">
                <td style="white-space:nowrap;"><a href="playerSettings.view?id=${model.player.id}" target="main"><fmt:message key="playlist.settings"/></a> |</td>
            </c:if>

            <td style="white-space:nowrap;"><select id="moreActions" onchange="actionSelected(this.options[selectedIndex].id)">
                <option id="top" selected="selected"><fmt:message key="playlist.more"/></option>
                <option style="color:blue;"><fmt:message key="playlist.more.playlist"/></option>
                <option id="savePlaylist">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="playlist.save"/></option>
                <c:if test="${model.user.downloadRole}">
                    <option id="downloadPlaylist">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="common.download"/></option>
                </c:if>
                <c:if test="${model.user.shareRole}">
                    <option id="sharePlaylist">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="main.more.share"/></option>
                </c:if>
                <option id="sortByTrack">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="playlist.more.sortbytrack"/></option>
                <option id="sortByAlbum">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="playlist.more.sortbyalbum"/></option>
                <option id="sortByArtist">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="playlist.more.sortbyartist"/></option>
                <option style="color:blue;"><fmt:message key="playlist.more.selection"/></option>
                <option id="selectAll">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="playlist.more.selectall"/></option>
                <option id="selectNone">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="playlist.more.selectnone"/></option>
                <option id="removeSelected">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="playlist.remove"/></option>
                <c:if test="${model.user.downloadRole}">
                    <option id="download">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="common.download"/></option>
                </c:if>
                <option id="appendPlaylist">&nbsp;&nbsp;&nbsp;&nbsp;<fmt:message key="playlist.append"/></option>
            </select>
            </td>

        </tr></table>
</div>

<div style="height:3.2em"></div>

<p id="empty"><em><fmt:message key="playlist.empty"/></em></p>

<table style="border-collapse:collapse;white-space:nowrap;">
    <tbody id="playlistBody">
        <tr id="pattern" style="display:none;margin:0;padding:0;border:0">
            <td class="bgcolor2"><a href="#">
                <img id="starSong" onclick="onStar(this.id.substring(8) - 1)" src="<spring:theme code="ratingOffImage"/>"
                     alt="" title=""></a></td>
            <td class="bgcolor2"><a href="#">
                <img id="removeSong" onclick="onRemove(this.id.substring(10) - 1)" src="<spring:theme code="removeImage"/>"
                     alt="<fmt:message key="playlist.remove"/>" title="<fmt:message key="playlist.remove"/>"></a></td>
            <td class="bgcolor2"><a href="#">
                <img id="up" onclick="onUp(this.id.substring(2) - 1)" src="<spring:theme code="upImage"/>"
                     alt="<fmt:message key="playlist.up"/>" title="<fmt:message key="playlist.up"/>"></a></td>
            <td class="bgcolor2"><a href="#">
                <img id="down" onclick="onDown(this.id.substring(4) - 1)" src="<spring:theme code="downImage"/>"
                     alt="<fmt:message key="playlist.down"/>" title="<fmt:message key="playlist.down"/>"></a></td>

            <td class="bgcolor2" style="padding-left: 0.1em"><input type="checkbox" class="checkbox" id="songIndex"></td>
            <td style="padding-right:0.25em"></td>

            <c:if test="${model.visibility.trackNumberVisible}">
                <td style="padding-right:0.5em;text-align:right"><span class="detail" id="trackNumber">1</span></td>
            </c:if>

            <td style="padding-right:1.25em">
                <img id="currentImage" src="<spring:theme code="currentImage"/>" alt="" style="display:none">
                <c:choose>
                    <c:when test="${model.player.externalWithPlaylist}">
                        <span id="title">Title</span>
                    </c:when>
                    <c:otherwise>
                        <a id="titleUrl" href="#">Title</a>
                    </c:otherwise>
                </c:choose>
            </td>

            <c:if test="${model.visibility.albumVisible}">
                <td style="padding-right:1.25em"><a id="albumUrl" target="main"><span id="album" class="detail">Album</span></a></td>
            </c:if>
            <c:if test="${model.visibility.artistVisible}">
                <td style="padding-right:1.25em"><span id="artist" class="detail">Artist</span></td>
            </c:if>
            <c:if test="${model.visibility.genreVisible}">
                <td style="padding-right:1.25em"><span id="genre" class="detail">Genre</span></td>
            </c:if>
            <c:if test="${model.visibility.yearVisible}">
                <td style="padding-right:1.25em"><span id="year" class="detail">Year</span></td>
            </c:if>
            <c:if test="${model.visibility.formatVisible}">
                <td style="padding-right:1.25em"><span id="format" class="detail">Format</span></td>
            </c:if>
            <c:if test="${model.visibility.fileSizeVisible}">
                <td style="padding-right:1.25em;text-align:right;"><span id="fileSize" class="detail">Format</span></td>
            </c:if>
            <c:if test="${model.visibility.durationVisible}">
                <td style="padding-right:1.25em;text-align:right;"><span id="duration" class="detail">Duration</span></td>
            </c:if>
            <c:if test="${model.visibility.bitRateVisible}">
                <td style="padding-right:0.25em"><span id="bitRate" class="detail">Bit Rate</span></td>
            </c:if>
        </tr>
    </tbody>
</table>

<div id="dialog-select-playlist" title="<fmt:message key="main.addtoplaylist.title"/>" style="display: none;">
    <p><fmt:message key="main.addtoplaylist.text"/></p>
    <div id="dialog-select-playlist-list"></div>
</div>

</body></html>