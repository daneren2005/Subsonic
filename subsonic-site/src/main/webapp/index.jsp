<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "home"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
<%@ include file="menu.jsp" %>
<%@ include file="banner.jsp" %>

<div id="content">
<div id="main-col">
    <h1>Welcome to Subsonic!</h1>

    <div class="floatcontainer margin10-t margin10-b">
        <ul class="stars column-left">
            <li>Enjoy your music and movies no matter where you are. Share with family and friends.</li>
            <li>Browse and manage your media collection with the user-friendly web interface.</li>
            <li>Stream music to <a href="apps.jsp">Android</a>, <a href="apps.jsp">iPhone</a> and <a href="apps.jsp">Windows Phone</a>.</li>
            <li>Subsonic <a href="apps.jsp">apps</a> available for Roku, BlackBerry Playbook and many more.</li>
        </ul>
        <ul class="stars column-right">
            <li>Convert and stream lossless music on the fly.</li>
            <li>Manage 100,000+ files in your music collection without hassle.</li>
            <li>Download and listen to Podcasts.</li>
            <li>Get cover art, lyrics, reviews and other album info from Google, <a href="http://www.chartlyrics.com/" target="_blank">Chartlyrics</a>,
                allmusic and Wikipedia.</li>
            <li><a href="features.jsp">Plus much more...</a></li>
        </ul>
    </div>

    <div class="featureitem">
        <div class="heading">What is Subsonic?</div>
        <div class="content">
            <div class="wide-content">

                <p>
                    Subsonic is a free, web-based media streamer, providing ubiquitous access to your music.
                    Use it to share your music with friends, or to listen to your own music while at work. You can
                    stream to multiple players simultaneously, for instance to one player in your kitchen and another in
                    your living room.
                </p>

                <a href="inc/img/screenshots/screen01.png"><img src="inc/img/screenshots/thumb01.png" alt="" class="img-right"/></a>

                <p>
                    Subsonic is designed to handle very large music collections (hundreds of gigabytes).
                    Although optimized for MP3 streaming, it works for any audio or video format that can stream over HTTP,
                    for instance AAC and OGG. By using <a href="transcoding.jsp">transcoder plug-ins</a>, Subsonic supports
                    on-the-fly conversion and streaming of virtually any audio format, including WMA, FLAC, APE, Musepack,
                    WavPack and Shorten.
                </p>

                <p>
                    If you have constrained bandwidth, you may set an upper limit for the bitrate of the music streams.
                    Subsonic will then automatically resample the music to a suitable bitrate.
                </p>

                <p>
                    In addition to being a streaming media server, Subsonic works very well as a local jukebox. The
                    intuitive web interface, as well as search and index facilities, are optimized for efficient browsing through large
                    media libraries.  Subsonic also comes with an integrated Podcast receiver, with many of the same features
                    as you find in iTunes.
                </p>

                <p>
                    Based on Java technology, Subsonic runs on most platforms, including Windows, Mac, Linux and Unix variants.
                </p>

                <a href="http://www.gnu.org/copyleft/gpl.html"><img src="inc/img/gpl.png" alt="GPL" class="img-left"/></a>

                <p>
                    Subsonic is open-source software licensed under <a rel="license" href="http://www.gnu.org/copyleft/gpl.html">GPL</a>.
                </p>

            </div>
        </div>
    </div>

    <div class="featureitem">
        <div class="heading">About</div>
        <div class="content">
            <div class="wide-content">
                <p>
                    <img src="inc/img/sindre.jpeg" alt="Sindre Mehus" hspace="10" vspace="10" style="float:right"/>
                    Subsonic is developed by <a href="mailto:sindre@activeobjects.no">Sindre Mehus</a>.
                    I live in Oslo, Norway and work as a Java software consultant.
                </p>
                <p>
                    If you have any questions, comments or suggestions for improvements, please visit the <a href="forum.jsp">Subsonic Forum</a>.
                </p>
            </div>
        </div>
    </div>
</div>

<div id="side-col">
    <%@ include file="google-translate.jsp" %>
    <%@ include file="download-subsonic.jsp" %>
    <%@ include file="quotes.jsp" %>
    <%@ include file="donate.jsp" %>
    <div class="bottomspace">
        <a href="http://sourceforge.net/projects/subsonic/"><img src="http://sourceforge.net/sflogo.php?group_id=126265&type=4" alt="SourceForge.net" class="img-center"/></a>
    </div>
</div>
<div class="clear">
</div>
</div>
<hr/>
<%@ include file="footer.jsp" %>
</div>


</body>
</html>
