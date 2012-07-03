<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">

<html><head>
    <%@ include file="head.jsp" %>
    <link rel="alternate" type="application/rss+xml" title="Subsonic Podcast" href="podcast.view?suffix=.rss">
</head>

<frameset rows="70,*,0" border="0" framespacing="0" frameborder="0">
    <frame name="upper" src="top.view?">
    <frameset cols="15%,85%" border="0" framespacing="0" frameborder="0">
        <frame name="left" src="left.view?" marginwidth="0" marginheight="0">

        <frameset rows="70%,30%" border="0" framespacing="0" frameborder="0">
            <frameset cols="*,${model.showRight ? 230 : 0}" border="0" framespacing="0" frameborder="0">
                <frame name="main" src="nowPlaying.view?" marginwidth="0" marginheight="0">
                <frame name="right" src="right.view?">
            </frameset>
            <frame name="playQueue" src="playQueue.view?">
        </frameset>
    </frameset>
    <frame name="hidden" frameborder="0" noresize="noresize">

</frameset>

</html>