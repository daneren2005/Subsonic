<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "features"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
<%@ include file="menu.jsp" %>

<div id="content">
<div id="main-col">
<h1>Subsonic Features</h1>

<div class="featureitem">
    <a name="userfriendly"></a>

    <div class="heading">Easy to use <a href="#top" class="backtotop" title="Back To Top"><img
            src="inc/img/top.gif" alt="Back To Top" height="16" width="16"/></a></div>
    <div class="content">
        <div class="screenshot">
            <a href="inc/img/features/amy.png"><img src="inc/img/features/amy-small.png" alt=""/></a>
        </div>
        <div class="description">
            <ul class="list">
                <li>Listen to your music from anywhere &ndash; all you need is a browser.</li>
                <li>The clean web interface is optimized for constrained bandwidth environments and efficient browsing through large music
                    collections (hundreds of gigabytes).</li>
                <li>Free-text search helps you find your favorite tracks quickly.</li>
                <li>Displays cover art, including images embedded in ID3 tags.</li>
                <li>Assign ratings and comments to albums.</li>
                <li>Common playlist features (add, remove, rearrange, repeat, shuffle, undo, save, load).</li>
            </ul>
        </div>
    </div>
</div>

<div class="featureitem">
    <a name="versatile"></a>

    <div class="heading">Versatile <a href="#top" class="backtotop" title="Back To Top"><img
            src="inc/img/top.gif" alt="Back To Top" height="16" width="16"/></a></div>
    <div class="content">
        <div class="screenshot">
            <a href="inc/img/screenshots/screen13.png"><img src="inc/img/screenshots/thumb13.png" alt="" style="padding-left:10px"/></a>
        </div>
        <div class="description">
            <ul class="list">
                <li>Stream or download music directly to your phone. Apps available for <a href="apps.jsp">Android</a>,
                    <a href="apps.jsp">iPhone</a> and <a href="apps.jsp">Windows Phone</a></li>
                <li>Use the AIR desktop application, <a href="apps.jsp">SubAir</a>.</li>
                <li>Control Subsonic from any mobile phone or PDA, using the WAP interface.</li>
                <li>Supports multiple simultaneous players. Manage any player from any location.</li>
                <li>Upload and download files to/from Subsonic, with automatic zipping and unzipping.</li>
            </ul>
        </div>
    </div>
</div>

    <div class="featureitem">
    <a name="mediasupport"></a>

    <div class="heading">Supports most media formats <a href="#top" class="backtotop" title="Back To Top"><img
            src="inc/img/top.gif" alt="Back To Top" height="16" width="16"/></a></div>
    <div class="content">
        <div class="screenshot">
            <img src="inc/img/features/media-support.png" alt=""/>
        </div>

        <div class="description">
            <ul class="list">
                <li>Supports MP3, OGG, AAC and any other audio or video format that streams over HTTP.</li>
                <li><a href="transcoding.jsp"><b>Transcoding engine</b></a> allows for streaming of a variety of lossy and lossless formats by converting to MP3 on-the-fly.</li>
                <li>Works with any network-enabled media player, such as Winamp, iTunes, XMMS, VLC, MusicMatch and Windows Media Player. Also includes an embedded Flash player.</li>
                <li>Tag parsing and editing of MP3, AAC, OGG, FLAC, WMA and APE files, using the <a href="http://www.jthink.net/jaudiotagger/">Jaudiotagger</a> library.</li>
                <li>Playlists can be saved and restored. M3U, PLS and XSPF formats are supported. Saved playlists are available as Podcasts.</li>
                <li>On-the-fly resampling to lower bitrates using the high-quality LAME encoder. Handy if your bandwidth is limited.</li>
                <li>Implements the SHOUTcast protocol. Players which support this (including Winamp, iTunes and XMMS) display the current artist and song, along
                    with other metadata.</li>
            </ul>
        </div>
    </div>
</div>

<div class="featureitem">
    <a name="customize"></a>
    <div class="heading">Customizable user experience <a href="#top" class="backtotop" title="Back To Top"><img
            src="inc/img/top.gif" alt="Back To Top" height="16" width="16"/></a></div>
    <div class="content">
        <div class="screenshot">
            <a href="inc/img/features/personal-settings.png"><img src="inc/img/features/personal-settings-small.png" alt=""/></a>
            <p/>
            <a href="inc/img/features/avatar.png"><img src="inc/img/features/avatar-small.png" alt=""/></a>
        </div>
        <div class="description">
            <ul class="list">
                <li>Available in these languages:<br/><br/>

                    <table style="padding-left:1.5em">
                        <tr><td>&#149; English </td><td>(by Sindre Mehus)</td></tr>
                        <tr><td>&#149; French </td><td>(by JohnDillinger)</td></tr>
                        <tr><td>&#149; Spanish </td><td>(by Jorge Bueno Magdalena)</td></tr>
                        <tr><td>&#149; Portuguese </td><td>(by Miguel Fonseca)</td></tr>
                        <tr><td>&#149; German </td><td>(by Harald Weiss and J&ouml;rg Frommann)</td></tr>
                        <tr><td>&#149; Italian </td><td>(by Michele Petrecca)</td></tr>
                        <tr><td>&#149; Greek </td><td>(by Constantine Samaklis)</td></tr>
                        <tr><td>&#149; Russian </td><td>(by Iaroslav Andrusiak)</td></tr>
                        <tr><td>&#149; Slovenian </td><td>(by Andrej &#381;i&#382;mond, Jan Jamsek and Marko Kastelic)</td></tr>
                        <tr><td>&#149; Macedonian </td><td>(by Stefan Ivanovski)</td></tr>
                        <tr><td>&#149; Polish </td><td>(by Micha&#322; Kotas)</td></tr>
                        <tr><td>&#149; Bulgarian </td><td>(by Ivan Achev)</td></tr>
                        <tr><td>&#149; Chinese </td><td>(by Neil Gao)</td></tr>
                        <tr><td>&#149; Japanese </td><td>(by Takahiro Suzuki)</td></tr>
                        <tr><td>&#149; Korean </td><td>(by Choi Jong-seok)</td></tr>
                        <tr><td>&#149; Dutch </td><td>(by Ronald Knot)</td></tr>
                        <tr><td>&#149; Norwegian </td><td>(by Sindre Mehus and jigsaw)</td></tr>
                        <tr><td>&#149; Swedish </td><td>(by J&ouml;rgen Sj&ouml;berg)</td></tr>
                        <tr><td>&#149; Danish </td><td>(by Morten Hartvich)</td></tr>
                        <tr><td>&#149; Finnish </td><td>(by Reijo J&auml;&auml;rni)</td></tr>
                        <tr><td>&#149; Icelandic </td><td>(by DJ Danny)</td></tr>
                    </table>
                </li>
                <li>Select from 24 different themes, including some that are optimized for HD screens.</li>
                <li>Highly configurable user interface.</li>
            </ul>
        </div>
    </div>
</div>

<div class="featureitem">
    <a name="integrate"></a>

    <div class="heading">Integrates with the best web services <a href="#top" class="backtotop" title="Back To Top"><img
            src="inc/img/top.gif" alt="Back To Top" height="16" width="16"/></a></div>
    <div class="content">
        <div class="screenshot">
            <img src="inc/img/features/last-fm.png" alt=""  style="padding-bottom:20px;padding-right:20px"/>
            <p/>
            <img src="inc/img/features/wikipedia.png" alt="" style="padding-right:20px"/>
        </div>
        <div class="description">
            <ul class="list">
                <li>Automatically register what you're playing on Last.fm, using the built-in Audioscrobbling support.</li>
                <li>Find cover art and lyrics using web services from Google and Chartlyrics.</li>
                <li>Read album reviews and more at Wikipedia, Google Music and allmusic.</li>
            </ul>
        </div>
    </div>
</div>

<div class="featureitem">
    <a name="secure"></a>

    <div class="heading">Secure and reliable<a href="#top" class="backtotop" title="Back To Top"><img
            src="inc/img/top.gif" alt="Back To Top" height="16" width="16"/></a></div>
    <div class="content">
        <div class="screenshot">
            <a href="inc/img/features/logon.png"><img src="inc/img/features/logon-small.png" alt=""/></a>
        </div>
        <div class="description">
            <ul class="list">
                <li>Users must log in with a username and password. Users are assigned different privileges.</li>
                <li>Specify upload and download bandwidth limits.</li>
                <li>Use HTTPS/SSL encryption for ultimate protection.</li>
                <li>Supports authentication in LDAP and Active Directory.</li>
                <li>Runs for months without crashing, hanging or leaking resources.</li>
            </ul>
        </div>
    </div>
</div>

<div class="featureitem">
    <a name="extras"></a>

    <div class="heading">Cool extra features <a href="#top" class="backtotop" title="Back To Top"><img
            src="inc/img/top.gif" alt="Back To Top" height="16" width="16"/></a></div>
    <div class="content">
        <div class="screenshot">
            <img src="inc/img/features/extras.png" alt=""/>
        </div>
        <div class="description">
            <ul class="list">
                <li>Download Podcasts with the integrated Podcast receiver.</li>
                <li>Manage your internet TV and radio stations.</li>
                <li>Play music directly on the server's audio hardware using the <b>jukebox</b> mode.</li>
            </ul>
        </div>
    </div>
</div>

</div>


<div id="side-col">
    <%@ include file="google-translate.jsp" %>
    <div class="sidebox">
        <h2>Features</h2>
        <ul class="list">
            <li><a href="#userfriendly">Easy to use</a></li>
            <li><a href="#versatile">Versatile</a></li>
            <li><a href="#mediasupport">Supports most media formats</a></li>
            <li><a href="#customize">Customizable user experience</a></li>
            <li><a href="#integrate">Integrates with the best web services</a></li>
            <li><a href="#secure">Secure and reliable</a></li>
            <li><a href="#extras">Cool extra features</a></li>
        </ul>
    </div>

    <%@ include file="download-subsonic.jsp" %>
    <%@ include file="translate-subsonic.jsp" %>
</div>

<div class="clear">
</div>
</div>
<hr/>
<%@ include file="footer.jsp" %>
</div>


</body>
</html>
