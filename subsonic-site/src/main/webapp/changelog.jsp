<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "changelog"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
<%@ include file="menu.jsp" %>

<div id="content">
<div id="main-col">
<h1 class="bottomspace">Subsonic Change Log</h1>

<a name="4.7.beta2"><h2 class="div">Subsonic 4.7.beta2 - Jun 08, 2012</h2></a>
<ul>
    <li><span class="bugid">New: </span>Playlist import/export.</li>
    <li><span class="bugid">New: </span>Sort albums by year.</li>
    <li><span class="bugid">New: </span>Show album year.</li>
    <li><span class="bugid">New: </span>Added Czech translation, courtesy of Robert Ilyk.</li>
    <li><span class="bugid">New: </span>Better error message if file or playlist not found.</li>
    <li><span class="bugid">Bugfix: </span>Handle media files without any tags.</li>
    <li><span class="bugid">Bugfix: </span>Display directory name rather than album name.</li>
    <li><span class="bugid">Bugfix: </span>Fixed genre search problem.</li>
</ul>

<a name="4.7.beta1"><h2 class="div">Subsonic 4.7.beta1 - May 12, 2012</h2></a>
<ul>
    <li><span class="bugid">New: </span>Major rewrite of the Subsonic core. It now uses less memory and is significantly faster.</li>
    <li><span class="bugid">New: </span>"Star" support for managing your favorite artists, albums and songs.</li>
    <li><span class="bugid">New: </span>Much improved playlist support: private/shared playlists, improved user interface.</li>
    <li><span class="bugid">New: </span>Added option <em>Settings &gt; Media folders &gt; Fast access mode</em> which additionally improves performance for network disks.</li>
    <li><span class="bugid">New: </span>Show media folder scanning status.</li>
    <li><span class="bugid">New: </span>New albums lists: Starred and All.</li>
    <li><span class="bugid">New: </span>Use ffmpeg rather than lame for downsampling.</li>
    <li><span class="bugid">New: </span>Added Polish translation, courtesy of Micha&#322; Kotas</li>
    <li><span class="bugid">New: </span>Added Catalan translation, courtesy of Josep Santal&oacute;.</li>
    <li><span class="bugid">New: </span>Added Estonian translation, courtesy of Olav M&auml;gi.</li>
    <li><span class="bugid">New: </span>Updated Dutch translation, courtesy of W. van der Heijden.</li>
    <li><span class="bugid">New: </span>Updated Swedish translation, courtesy of Fritte Jensen.</li>
    <li><span class="bugid">New: </span>Updated Catalan translation, courtesy of Josep Santalo.</li>
    <li><span class="bugid">New: </span>Added option to reset and email forgotten password or username.</li>
    <li><span class="bugid">New: </span>Make it configurable to run as a different user than root in Linux RPM.</li>
    <li><span class="bugid">New: </span>Display warning if running as root user.</li>
    <li><span class="bugid">New: </span>Avoid false alarms from Windows virus scanners.</li>
    <li><span class="bugid">Bugfix: </span>Fixed problem with players being associated to wrong users.</li>
    <li><span class="bugid">Bugfix: </span>Serve generic cover art if failing to scale original.</li>
    <li><span class="bugid">REST: </span>Full support for accessing the media collection organized according to ID3 tags, rather than file structure.</li>
    <li><span class="bugid">REST: </span>Added starring and playlist management.</li>
    <li><span class="bugid">REST: </span>Added disc number, creation date and media type.</li>
    <li><span class="bugid">REST: </span>Suppress content length estimation by default. Added "estimateContentLength" parameter to stream method.</li>
    <li><span class="bugid">REST: </span>Added getAvatar method.</li>
    <li><span class="bugid">REST: </span>Added "shareRole" to createUser</li>
    <li><span class="bugid">REST: </span>Fixed some JSON conversion problems ("JSON does not allow non-finite numbers")</li>
    <li><span class="bugid">REST: </span>Fixed a problem with the "u" and "p" parameters sometimes not being used if the JSESSIONID cookie is present.</li>
    <li><span class="bugid">REST: </span>Stream method now supports HEAD requests.</li>
    <li><span class="bugid">REST: </span>Use shorter IDs.</li>
    <li><span class="bugid">Tech: </span>Use port 4040 by default for new Windows installs.</li>
</ul>

<a name="4.6"><h2 class="div">Subsonic 4.6 - Dec 06, 2011</h2></a>
<ul>
    <li><span class="bugid">New: </span>Added option to resend license key.</li>
    <li><span class="bugid">New: </span>Added AIFF support.</li>
</ul>

<a name="4.6.beta2"><h2 class="div">Subsonic 4.6.beta2 - Nov 17, 2011</h2></a>
<ul>
    <li><span class="bugid">New: </span>Added option in Settings &gt; Transcoding: "Enable this transcoding for all existing and new players."</li>
    <li><span class="bugid">New: </span>Updated Slovenian translation, courtesy of Jan Jam&#353;ek.</li>
    <li><span class="bugid">New: </span>Added Google+ button.</li>
    <li><span class="bugid">New: </span>Automatically delete obsolete players on startup.</li>
    <li><span class="bugid">Bugfix: </span>Jukebox now support skipping when paused.</li>
    <li><span class="bugid">Bugfix: </span>Fixed a case where guessing the title and track number failed.</li>
    <li><span class="bugid">Bugfix: </span>Don't estimate content-length for web players.</li>
    <li><span class="bugid">Bugfix: </span>Album search links missing if subdirectory exists.</li>
    <li><span class="bugid">Bugfix: </span>Fixed broken Google search link.</li>
    <li><span class="bugid">Bugfix: </span>Remove link to lyrics. chartlyrics.com no longer exists.</li>
    <li><span class="bugid">REST: </span>Ensure that jukebox "set" method maintains correct current index.</li>
    <li><span class="bugid">REST: </span>Added "scrobblingEnabled" to getUser.</li>
    <li><span class="bugid">Tech: </span>Ensure Windows uninstaller only removes program files (in case user installs to c:\subsonic).</li>
    <li><span class="bugid">Tech: </span>Subsonic Control Panel now prompts for elevated permissions when necessary.</li>
</ul>

<a name="4.6.beta1"><h2 class="div">Subsonic 4.6.beta1 - Nov 1, 2011</h2></a>
<ul>
    <li><span class="bugid">New: </span>Simplified and improved transcoding framework.</li>
    <li><span class="bugid">New: </span>Improved video quality with H.264 encoding.</li>
    <li><span class="bugid">New: </span>Reimplemented jukebox. More robust and new features.</li>
    <li><span class="bugid">New: </span>Share on Google+.</li>
    <li><span class="bugid">New: </span>Estimate content-length for transcoded audio.</li>
    <li><span class="bugid">New: </span>Updated Dutch translation, courtesy of Muiz.</li>
    <li><span class="bugid">New: </span>Updated German translation, courtesy of deejay.</li>
    <li><span class="bugid">New: </span>Added BUUF theme, courtesy of Fractal Systems.</li>
    <li><span class="bugid">New: </span>Added UK English translation, courtesy of Brian Aust.</li>
    <li><span class="bugid">New: </span>Hide dock icon on Mac.</li>
    <li><span class="bugid">Bugfix: </span>Menu broken in "Settings &gt; Shared media" when logged in as admin.</li>
    <li><span class="bugid">Tech: </span>Improved security in Linux packages.</li>
    <li><span class="bugid">Tech: </span>Stronger license check.</li>
    <li><span class="bugid">Tech: </span>Use UTF-8 when reading playlist files.</li>
    <li><span class="bugid">API: </span>Include files in root folders.</li>
    <li><span class="bugid">API: </span>New jukebox features.</li>
</ul>

<a name="4.5"><h2 class="div">Subsonic 4.5 - Aug 6, 2011</h2></a>
<ul>
    <li><span class="bugid">New: </span>Updated Korean translation, courtesy of Rhetor Choi.</li>
    <li><span class="bugid">New: </span>Updated Danish translation, courtesy of Morten Hartvich.</li>
    <li><span class="bugid">Bugfix: </span>Clear rating now works again.</li>
    <li><span class="bugid">Bugfix: </span>Misc fixes to share settings page.</li>
    <li><span class="bugid">Bugfix: </span>Revert back to running as root in Debian/Ubuntu.</li>
    <li><span class="bugid">API: </span>JSONP support</li>
</ul>

<a name="4.5.beta2"><h2 class="div">Subsonic 4.5.beta2 - Jul 28, 2011</h2></a>
<ul>
    <li><span class="bugid">New: </span>Implemented management of shared media (expiration, removal, statistics, description).</li>
    <li><span class="bugid">New: </span>Added more actions for selected songs in album view (Download, Add to playlist).</li>
    <li><span class="bugid">New: </span>Added more actions for selected songs in playlist view (Share).</li>
    <li><span class="bugid">New: </span>Subsonic service no longer running as root on Debian/Ubuntu</li>
    <li><span class="bugid">API: </span>Added share management methods.</li>
    <li><span class="bugid">API: </span>Added rating support.</li>
    <li><span class="bugid">Bugfix: </span>Use guest user when streaming shared media.</li>
    <li><span class="bugid">Bugfix: </span>Use UTF-8 in search index file.</li>
</ul>

<a name="4.5.beta1"><h2 class="div">Subsonic 4.5.beta1 - Jun 12, 2011</h2></a>
<ul>
    <li><span class="bugid">New: </span>Twitter/Facebook integration.</li>
    <li><span class="bugid">New: </span>Share songs and videos by sending someone a link.</li>
    <li><span class="bugid">New: </span>Support video in external player.</li>
    <li><span class="bugid">New: </span>Support higher video bitrates, 3000 and 5000 kbps.</li>
    <li><span class="bugid">New: </span>Added Bulgarian translation, courtesy of Ivan Achev.</li>
    <li><span class="bugid">New: </span>Updated Norwegian translation, courtesy of Tommy Karlsen.</li>
    <li><span class="bugid">New: </span>Updated German translation, courtesy of deejay2302.</li>
    <li><span class="bugid">New: </span>Support http header "Content-Range" when streaming.</li>
    <li><span class="bugid">New: </span>Ignore "@eaDir" folders on Synology devices.</li>
    <li><span class="bugid">API: </span>Added REST method getPodcasts.</li>
    <li><span class="bugid">API: </span>Added REST method getShareUrl.</li>
    <li><span class="bugid">API: </span>Added user email to REST API.</li>
    <li><span class="bugid">API: </span>Added example XMLs to API documentation.</li>
    <li><span class="bugid">API: </span>Encoded passwords were not decoded correctly in REST methods createUser and changePassword.</li>
    <li><span class="bugid">API: </span>Added "format" and "size" parameters to REST method stream.</li>
    <li><span class="bugid">Bugfix: </span>Don't include podcasts when generating random songs.</li>
    <li><span class="bugid">Bugfix: </span>Handle whitespace in podcast urls.</li>
    <li><span class="bugid">Bugfix: </span>Updated expired ssl cert.</li>
    <li><span class="bugid">Bugfix: </span>Mac installer: make transcode dir executable.</li>
</ul>

<a name="4.4"><h2 class="div">Subsonic 4.4 - Feb 06, 2011</h2></a>
<ul>
    <li><span class="bugid">New: </span>Updated Greek translation, courtesy of Constantine Samaklis.</li>
    <li><span class="bugid">New: </span>Updated Slovenian translation, courtesy of Jan Jam&#353;ek and Marko Kastelic.</li>
</ul>

<a name="4.4.beta1"><h2 class="div">Subsonic 4.4.beta1 - Jan 20, 2011</h2></a>
<ul>
    <li><span class="bugid">New: </span>Added https support.</li>
    <li><span class="bugid">New: </span>Added option to open video in resizable window.</li>
    <li><span class="bugid">New: </span>Improved search with accented characters (e.g., searching for "bartok" will match "bart&oacute;k").</li>
    <li><span class="bugid">New: </span>Added REST API method for scrobbling to last.fm.</li>
    <li><span class="bugid">New: </span>Added Greek translation, courtesy of Constantine Samaklis.</li>
    <li><span class="bugid">Bugfix: </span>When converting videos, always use a width that is a multiple of two.</li>
    <li><span class="bugid">Bugfix: </span>Handle invalid pixel aspect rates (PAR) reported by ffmpeg.</li>
    <li><span class="bugid">Bugfix: </span>Don't scrobble videos to last.fm.</li>
    <li><span class="bugid">Bugfix: </span>Remember port number and memory settings when reinstalling on Windows.</li>
    <li><span class="bugid">Bugfix: </span>Accessing "Newest" album list from Android sometimes caused an error.</li>
</ul>

<a name="4.3"><h2 class="div">Subsonic 4.3 - Jan 7, 2011</h2></a>
<ul>
    <li><span class="bugid">Bugfix: </span>When switching bit rate, also jump to correct time offset.</li>
    <li><span class="bugid">Bugfix: </span>Fixed problem with audio getting out-of-sync with video.</li>
    <li><span class="bugid">Bugfix: </span>When converting videos, always use a width that is a multiple of two.</li>
    <li><span class="bugid">Bugfix: </span>Fixed bug in REST method createPlaylist.</li>
    <li><span class="bugid">Bugfix: </span>Use language settings in /etc/default/locale on Ubuntu/Debian.</li>
</ul>

<a name="4.3.beta1"><h2 class="div">Subsonic 4.3.beta1 - Dec 30, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>Video support! Stream video to browsers and Android 2.2+ phones.</li>
    <li><span class="bugid">New: </span>Added Korean translation, courtesy of Choi Jong-seok.</li>
    <li><span class="bugid">New: </span>Updated French translation, courtesy of Yoann Spicher.</li>
</ul>

<a name="4.2"><h2 class="div">Subsonic 4.2 - Nov 21, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>Added Portuguese translation, courtesy of Miguel Fonseca.</li>
    <li><span class="bugid">New: </span>Updated Italian translation, courtesy of Luca Perri.</li>
    <li><span class="bugid">Bugfix: </span>Improved searching.</li>
    <li><span class="bugid">Bugfix: </span>Set ID3 tags when transcoding.</li>
    <li><span class="bugid">Bugfix: </span>Fixed problem with transcoding/downsampling failing for files with non-Latin characters.</li>
    <li><span class="bugid">Bugfix: </span>Handle transcoding/downsampling files with double quotes in filename.</li>
    <li><span class="bugid">Bugfix: </span>Remove html markup from podcast descriptions.</li>
    <li><span class="bugid">Bugfix: </span>Fix broken link to allmusic.</li>
</ul>

<a name="4.2.beta1"><h2 class="div">Subsonic 4.2.beta1 - Nov 4, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>New &amp; improved search engine based on <a href="http://lucene.apache.org/">Lucene</a>.</li>
    <li><span class="bugid">New: </span>New &amp; improved cover art search based on Google Image Search.</li>
    <li><span class="bugid">New: </span>Mac installer now includes transcoders, courtesy of einstein2x.</li>
    <li><span class="bugid">New: </span>Updated German translation, courtesy of deejay2302.</li>
    <li><span class="bugid">New: </span>Improved LAME integration (preserve ID3 tags, avoid skipping in Android client).</li>
    <li><span class="bugid">New: </span>Added new REST API method for searching.</li>
    <li><span class="bugid">Bugfix: </span>Fixed bug in startup script in standalone Subsonic (readlink).</li>
</ul>

<a name="4.1"><h2 class="div">Subsonic 4.1 - Sep 10, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>Updated Danish translation, courtesy of Morten Hartvich.</li>
    <li><span class="bugid">Bugfix: </span>Exclude dot files in cover art search.</li>
</ul>

<a name="4.1.beta1"><h2 class="div">Subsonic 4.1.beta1 - Aug 21, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>Support automatic port forwarding on Airport Extreme/Express.</li>
    <li><span class="bugid">New: </span>Improved tray/application/fav icons.</li>
    <li><span class="bugid">New: </span>New and updated themes.</li>
    <li><span class="bugid">New: </span>Improved Mac control panel.</li>
    <li><span class="bugid">New: </span>Added REST API methods getUser and deleteUser.</li>
    <li><span class="bugid">New: </span>Added Traditional Chinese translation, courtesy of Cheng Jen Li.</li>
    <li><span class="bugid">New: </span>Updated French translation, courtesy of Christophe.</li>
    <li><span class="bugid">Bugfix: </span>Fixed chat feature when deploying Subsonic on Tomcat.</li>
    <li><span class="bugid">Bugfix: </span>Proper lookup of local IP on Linux.</li>
    <li><span class="bugid">Bugfix: </span>Support html5 (return correct content type and length).</li>
    <li><span class="bugid">Performance: </span>Faster coverart lookup.</li>
    <li><span class="bugid">Tech: </span>Upgraded to LAME 3.98.4.</li>
</ul>

<a name="4.0.1"><h2 class="div">Subsonic 4.0.1 - May 13, 2010</h2></a>
<ul>
    <li><span class="bugid">Bugfix: </span>Fixed broken cover art download.</li>
    <li><span class="bugid">Bugfix: </span>Fixed broken m4a transcoding.</li>
    <li><span class="bugid">Bugfix: </span>Fixed broken quick links in artist frame for some new themes.</li>
</ul>

<a name="4.0"><h2 class="div">Subsonic 4.0 - May 12, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>Added REST API method for deleting playlists.</li>
    <li><span class="bugid">Bugfix: </span>Use UTF-8 encoding when creating M3U playlists.</li>
    <li><span class="bugid">Security: </span>Network and cover art settings require admin role.</li>
</ul>

<a name="4.0.beta2"><h2 class="div">Subsonic 4.0.beta2 - May 05, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>Added themes Slick, High Contrast, High Contrast (Inverted) and High-Tech, courtesy of Fisher Evans.</li>
    <li><span class="bugid">New: </span>Added themes Simplify, PinkPanther and Denim, courtesy of Thomas Bruce Dyrud.</li>
    <li><span class="bugid">New: </span>Updated Danish translation, courtesy of Morten Hartvich.</li>
    <li><span class="bugid">Bugfix: </span>User statistics for streamed data was too high.</li>
    <li><span class="bugid">Bugfix: </span>Proper ordering songs on multi-disc albums.</li>
    <li><span class="bugid">Bugfix: </span>Read tags from AAC files.</li>
    <li><span class="bugid">Bugfix: </span>Fixed errors with chat feature.</li>
    <li><span class="bugid">Bugfix: </span>More robust tag parsing and editing.</li>
    <li><span class="bugid">Bugfix: </span>Removed "Settings &gt; General &gt; Video mask" that was added by mistake.</li>
</ul>

<a name="4.0.beta1"><h2 class="div">Subsonic 4.0.beta1 - Apr 23, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>New version of the <a href="api.jsp">Subsonic API</a> with many new methods to be used by Subsonic <a href="apps.jsp">apps</a>.</li>
    <li><span class="bugid">New: </span>New, simpler transcoder pack with ffmpeg and lame.</li>
    <li><span class="bugid">New: </span>Improved usability of Settings &gt; Network.</li>
    <li><span class="bugid">New: </span>Added option to clear chat messages.</li>
    <li><span class="bugid">New: </span>Added Monochrome themes, courtesy of David D.</li>
    <li><span class="bugid">New: </span>Added Groove themes, courtesy of Thomas Bruce Dyrud.</li>
    <li><span class="bugid">New: </span>Updated German translation, courtesy of deejay2302.</li>
    <li><span class="bugid">New: </span>Updated Finnish translation, courtesy of Reijo J&auml;&auml;rni.</li>
    <li><span class="bugid">New: </span>Updated Slovenian translation, courtesy of Andrej &#381;i&#382;mond.</li>
    <li><span class="bugid">New: </span>Made user statistics graphs logarithmic.</li>
    <li><span class="bugid">New: </span>Truncate long genre names in "More" page.</li>
    <li><span class="bugid">New: </span>Improved year selection in "More" page.</li>
    <li><span class="bugid">New: </span>Automatically exclude all hidden files and directories (those starting with ".")</li>
    <li><span class="bugid">Bugfix: </span>Fixed native playback of AAC in Flash player.</li>
    <li><span class="bugid">Bugfix: </span>Make Flash player work on Linux.</li>
    <li><span class="bugid">Bugfix: </span>Fixed bug in coverart batch.</li>
    <li><span class="bugid">Bugfix: </span>Fixed album link to last.fm.</li>
    <li><span class="bugid">Bugfix: </span>Improved stability of lyrics lookup.</li>
    <li><span class="bugid">Tech: </span>Replaced tag library with <a href="http://www.jthink.net/jaudiotagger/">Jaudiotagger</a>.</li>
    <li><span class="bugid">Tech: </span>Compress html pages with gzip.</li>
    <li><span class="bugid">Tech: </span>Changed default Java memory limit to 100 MB.</li>
</ul>

<a name="3.9"><h2 class="div">Subsonic 3.9 - Feb 12, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>Added link to <a href="http://www.nonpixel.com/subair/">SubAir</a> in "More" page.</li>
    <li><span class="bugid">New: </span>Make <em>yourname.subsonic.org</em> addresses also work within LANs.</li>
    <li><span class="bugid">Bugfix: </span>Don't look for UPnP routers if port forwarding is disabled.</li>
</ul>

<a name="3.9.beta1"><h2 class="div">Subsonic 3.9.beta1 - Jan 28, 2010</h2></a>
<ul>
    <li><span class="bugid">New: </span>Automatically configure port forwarding for compatible routers.</li>
    <li><span class="bugid">New: </span>Access your server using an easy-to-remember address: <em>yourname.subsonic.org</em></li>
    <li><span class="bugid">New: </span>Added "Getting started" page for basic setup.</li>
    <li><span class="bugid">New: </span>Cover art batch download, courtesy of Christian Nedreg&aring;rd.</li>
    <li><span class="bugid">New: </span>Improved relevance for top hits in cover art search, courtesy of Christian Nedreg&aring;rd.</li>
    <li><span class="bugid">New: </span>Added Polish translation, courtesy of Micha&#322; Kotas</li>
    <li><span class="bugid">New: </span>Added Icelandic translation, courtesy of DJ Danni.</li>
    <li><span class="bugid">New: </span>Updated Finnish translation, courtesy of Reijo J&auml;&auml;rni.</li>
    <li><span class="bugid">New: </span>Updated Russian translation, courtesy of Anton Khoruzhy.</li>
    <li><span class="bugid">New: </span>Reversed order of chat log, showing newest messages first.</li>
    <li><span class="bugid">New: </span>Added API method to change password.</li>
    <li><span class="bugid">New: </span>Discontinued Subsonic JME client.</li>
    <li><span class="bugid">Bugfix: </span>Avoid setting incorrect content length when transcoding or downsampling.</li>
    <li><span class="bugid">Bugfix: </span>Fixed lyrics lookup. Now uses chartlyrics.com which provides a stable API.</li>
    <li><span class="bugid">Bugfix: </span>Fixed "Not in GZIP format" error in Discogs cover image search, courtesy of Christian Nedreg&aring;rd.</li>
    <li><span class="bugid">Bugfix: </span>Sort list of saved playlists.</li>
    <li><span class="bugid">Bugfix: </span>Home &gt; Newest now sorts by creation date, not modification date.</li>
    <li><span class="bugid">Bugfix: </span>Hide donate button when license is valid.</li>
</ul>

<a name="3.8"><h2 class="div">Subsonic 3.8 - Oct 23, 2009</h2></a>
<ul>
    <li><span class="bugid">New: </span>(Android) Downloaded songs can now be played directly from the Subsonic app.</li>
    <li><span class="bugid">New: </span>(Android) New feature: Download + Play.</li>
    <li><span class="bugid">New: </span>(Android) New feature: Search.</li>
    <li><span class="bugid">New: </span>(Android) New feature: Load playlist.</li>
    <li><span class="bugid">New: </span>(Android) New feature: Delete from phone.</li>
    <li><span class="bugid">New: </span>(Android) New feature: Check for software update.</li>
    <li><span class="bugid">New: </span>(Android) Nicer song listing.</li>
    <li><span class="bugid">New: </span>(Android) More informative error message when not connected.</li>
    <li><span class="bugid">New: </span>(Android) Show artist shortcuts.</li>
    <li><span class="bugid">New: </span>Added option to disable chat messages.</li>
    <li><span class="bugid">New: </span>Delete chat entries more than seven days old.</li>
    <li><span class="bugid">New: </span>Documented error codes in REST API.</li>
    <li><span class="bugid">New: </span>Updated Swedish translation, courtesy of Fritte.</li>
    <li><span class="bugid">New: </span>Updated German translation, courtesy of deejay2302 and Radon.</li>
    <li><span class="bugid">Bugfix: </span>Fixed wrong background color in More page.</li>
</ul>

<a name="3.8.beta1"><h2 class="div">Subsonic 3.8.beta1 - Oct 02, 2009</h2></a>
<ul>
    <li><span class="bugid">New: </span>Implemented Subsonic client for Android phones.</li>
    <li><span class="bugid">New: </span>Added chat.</li>
    <li><span class="bugid">New: </span>Added REST API for third party applications.</li>
    <li><span class="bugid">New: </span>Support playlist repeat in web player.</li>
    <li><span class="bugid">New: </span>Jukebox now support WAV format (including FLAC > WAV transcoding).</li>
    <li><span class="bugid">New: </span>Updated to Last.fm submission protocol version 1.2.1, with support for "Now playing".</li>
    <li><span class="bugid">New: </span>Updated Dutch translation, courtesy of Sander van der Grind and Jeremy Terpstra.</li>
    <li><span class="bugid">New: </span>Updated Slovenian translation, courtesy of Andrej &#381;i&#382;mond</li>
    <li><span class="bugid">New: </span>Improved French translation, courtesy of Rapha&euml;l Boulcourt.</li>
    <li><span class="bugid">New: </span>Improved German translation, courtesy of 3R3.</li>
    <li><span class="bugid">New: </span>Added Finnish translation, courtesy of Reijo J&auml;&auml;rni</li>
    <li><span class="bugid">Bugfix: </span>Subsonic server doesn't require an internet connection during startup.</li>
    <li><span class="bugid">Bugfix: </span>Avoid problems when upgrading stand-alone version.</li>
    <li><span class="bugid">Bugfix: </span>Repeat now works properly in jukebox mode.</li>
    <li><span class="bugid">Bugfix: </span>Looks nicer in Chrome.</li>
    <li><span class="bugid">Bugfix: </span>Usernames can now contain white spaces and international characters.</li>
    <li><span class="bugid">Bugfix: </span>Allow alternate date format in Podcast episodes.</li>
    <li><span class="bugid">Bugfix: </span>Fixed broken Discogs image search.</li>
    <li><span class="bugid">Security: </span>IP addresses are no longer displayed in the log or the status view.</li>
</ul>

<a name="3.7"><h2 class="div">Subsonic 3.7 - Jun 22, 2009</h2></a>
<ul>
    <li><span class="bugid">New:</span> Default search option is now to search both title, album and artist.</li>
    <li><span class="bugid">New:</span> Customizable default music, playlist and podcast folders in support of Amahi Home Server.</li>
    <li><span class="bugid">New:</span> Added configurable message in login page.</li>
    <li><span class="bugid">Bugfix:</span> Fixed broken WAR version.</li>
    <li><span class="bugid">Bugfix:</span> Fixed broken tag editor.</li>
    <li><span class="bugid">Bugfix:</span> Fixed broken upload progress bar.</li>
    <li><span class="bugid">Bugfix:</span> Automatically start web player when clicking play on artist, album or song.</li>
    <li><span class="bugid">Bugfix:</span> Fixed problem with "Random album" page being initially empty.</li>
    <li><span class="bugid">Bugfix:</span> Added some padding above player toolbar.</li>
    <li><span class="bugid">Bugfix:</span> Playlist combobox option disabling broken in IE8.</li>
    <li><span class="bugid">Security:</span> Don't show full file paths in "Status" and "Help" pages.</li>
</ul>

<a name="3.7.beta1"><h2 class="div">Subsonic 3.7.beta1 - May 08, 2009</h2></a>
<ul>
    <li><span class="bugid">New:</span> Jukebox support. Plays music directly on the server's audio hardware.</li>
    <li><span class="bugid">New:</span> Improved search feature. (Paging, album/artist search fields.)</li>
    <li><span class="bugid">New:</span> Added buttons for next/previous track in web player.</li>
    <li><span class="bugid">New:</span> Support for artist-level comments.</li>
    <li><span class="bugid">New:</span> Added new authorization role "User is allowed to change settings and password" in support for guest users.</li>
    <li><span class="bugid">New:</span> Added theme "Cool and Clean", courtesy of Dan Eriksen.</li>
    <li><span class="bugid">New:</span> Added theme "Midnight Fun", courtesy of Don Pearson.</li>
    <li><span class="bugid">New:</span> Added Slovenian translation, courtesy of Andrej &#381;i&#382;mond.</li>
    <li><span class="bugid">New:</span> Added Danish translation, courtesy of Morten Hartvich</li>
    <li><span class="bugid">New:</span> Added Japanese translation, courtesy of Takahiro Suzuki.</li>
    <li><span class="bugid">New:</span> Updated Norwegian translation, courtesy of jigsaw.</li>
    <li><span class="bugid">New:</span> Updated Swedish translation, courtesy of Fredrik Leufkens.</li>
    <li><span class="bugid">New:</span> Show online help as balloon tooltip.</li>
    <li><span class="bugid">New:</span> Player is always visible in playlist, even when scrolling.</li>
    <li><span class="bugid">New:</span> Removed frame borders.</li>
    <li><span class="bugid">New:</span> Don't show player username or IP address in playlist.</li>
    <li><span class="bugid">New:</span> Automatically trim text fields when entering email address and license key.</li>
    <li><span class="bugid">New:</span> Added advanced option to bind Subsonic to a particular IP address. (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1740 ">More</a>)</li>
    <li><span class="bugid">New:</span> Improved subsonic.sh startup script.</li>
    <li><span class="bugid">New:</span> Automatically add Windows Firewall exceptions during install. (<a href="http://forum.subsonic.org/forum/viewtopic.php?p=5188">More</a>)</li>
    <li><span class="bugid">Performance:</span> Improved browser and server caching of artist list. (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1558">More</a>)</li>
    <li><span class="bugid">Performance:</span> Only poll for service status if Subsonic Control Panel is opened. (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1612">More</a>)</li>
    <li><span class="bugid">Bugfix:</span> Web player now displays correct duration for transcoded tracks.</li>
    <li><span class="bugid">Bugfix:</span> Sticky artist index now works in IE.</li>
    <li><span class="bugid">Bugfix:</span> Discogs image retrieval now works again.</li>
    <li><span class="bugid">Bugfix:</span> Now works behind SSL proxy. (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1301">More</a>)</li>
    <li><span class="bugid">Bugfix:</span> Missing album art for artists in multiple folders. (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1626">More</a>)</li>
    <li><span class="bugid">Bugfix:</span> Wrong player selected if different users access Subsonic from the same browser instance.</li>
    <li><span class="bugid">Bugfix:</span> Wrong repeat state displayed in playlist.</li>
    <li><span class="bugid">Bugfix:</span> Stop web player when playlist is cleared.</li>
    <li><span class="bugid">Bugfix:</span> Lyrics lookup working again.</li>
    <li><span class="bugid">Bugfix:</span> Support quotes in Podcast names. (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1734">More</a>)</li>
    <li><span class="bugid">Bugfix:</span> Fixed IE layout problem in top frame.</li>
</ul>

<a name="3.6"><h2 class="div">Subsonic 3.6 - Feb 01, 2009</h2></a>
<ul>
    <li><span class="bugid">Security:</span> For improved security, only transcoders installed in SUBSONIC_HOME/transcode are allowed to be executed. This includes LAME downsampling.</li>
    <li><span class="bugid">New:</span> Added Swedish translation by J&ouml;rgen Sj&ouml;berg.</li>
    <li><span class="bugid">New:</span> Added two new locales, "English (United States)" and "English (United Kingdom)". The only difference currently is the date format.</li>
    <li><span class="bugid">New:</span> Change tray icon if Subsonic server isn't running.</li>
    <li><span class="bugid">Bugfix:</span> Make new Flash player work if Subsonic is behind proxy.</li>
    <li><span class="bugid">Bugfix:</span> Fixed problem where server in some cases came to a state where streaming is refused.</li>
    <li><span class="bugid">Bugfix:</span> Support download and streaming of files larger than 2 GB.</li>
    <li><span class="bugid">Bugfix:</span> Use UTF-8 when generating m3u playlist.</li>
    <li><span class="bugid">Bugfix:</span> Main frame now correctly switches to the currently playing album.</li>
    <li><span class="bugid">Bugfix:</span> Show currently playing icon for all player types.</li>
</ul>

<a name="3.6.beta2"><h2 class="div">Subsonic 3.6.beta2 - Jan 13, 2009</h2></a>
<ul>
    <li><span class="bugid">New:</span> Updated French translation. (Thanks to sheridan).</li>
    <li><span class="bugid">New:</span> Improved subsonic.sh startup script.</li>
    <li><span class="bugid">Bugfix:</span> New Flash player now works in Linux browsers. (Thanks to zeekay).</li>
    <li><span class="bugid">Bugfix:</span> Fixed bug introduced in 3.6.beta1 causing streams to be killed repeatedly.</li>
    <li><span class="bugid">Bugfix:</span> Show proper error message if user is not authorized to perform an operation.</li>
    <li><span class="bugid">Bugfix:</span> Removing welcome title/subtitle/message doesn't work.</li>
    <li><span class="bugid">Bugfix:</span> Welcome message too wide in IE.</li>
    <li><span class="bugid">Bugfix:</span> Flash player doesn't work with Italian locale.</li>
    <li><span class="bugid">Bugfix:</span> Disable "Play more random songs when end of playlist is reached" if Flash player.</li>
    <li><span class="bugid">Tech:</span> Embedded player now requires Flash plugin 9.0.0 or later, not 9.0.115 or later.</li>
</ul>

<a name="3.6.beta1"><h2 class="div">Subsonic 3.6.beta1 - Jan 05, 2009</h2></a>
<ul>
    <li><span class="bugid">New:</span> Much improved embedded Flash player.</li>
    <li><span class="bugid">New:</span> Playlist is now Ajax-enabled, for a smoother user experience.</li>
    <li><span class="bugid">New:</span> Configurable Welcome title, subtitle and message in home page. Uses wiki notation.</li>
    <li><span class="bugid">New:</span> Fade-in effect for cover art images.</li>
    <li><span class="bugid">New:</span> Option to append selected tracks to previously saved playlist.</li>
    <li><span class="bugid">New:</span> Random play on artist/album level.</li>
    <li><span class="bugid">New:</span> New agent/service architecture. Tray icon should now (finally) work on Vista.</li>
    <li><span class="bugid">New:</span> From the Subsonic Control Panel you can now see the Windows service status, and start/stop the Subsonic service.</li>
    <li><span class="bugid">New:</span> Display "Now playing" for up to an hour, including idle time.</li>
    <li><span class="bugid">New:</span> Enable transcoders on first-time Windows install (since they are now bundled).</li>
    <li><span class="bugid">New:</span> Added Cancel buttons to all settings pages.</li>
    <li><span class="bugid">New:</span> Created subsonic.bat</li>
    <li><span class="bugid">New:</span> Changed license to GPLv3.</li>
    <li><span class="bugid">New:</span> Hide music library statistics if unavailable.</li>
    <li><span class="bugid">Bugfix:</span> Fixed broken lyrics lookup (again).</li>
    <li><span class="bugid">Bugfix:</span> Avoid ugly line breaks in IE.</li>
    <li><span class="bugid">Tech:</span> Java 6 or later is now required for Windows installer version.</li>
</ul>

<a name="3.5"><h2 class="div">Subsonic 3.5 - Nov 09, 2008</h2></a>
<ul>
    <li><span class="bugid">New:</span> New Subsonic logo (thanks to <a href="http://www.conceptualintegration.com/">Concept211</a>).</li>
    <li><span class="bugid">New:</span> Added Italian translation (thanks to Michele Petrecca).
    <li><span class="bugid">New:</span> Smooth scrolling in jump list.</li>
    <li><span class="bugid">New:</span> Updated AAC decoder (faad.exe) in transcoder pack.</li>
    <li><span class="bugid">New:</span> Added OS to about page.</li>
    <li><span class="bugid">New:</span> Changed license to Creative Commons Noncommercial.</li>
    <li><span class="bugid">Bugfix:</span> Added start menu item "Settings" as work-around for missing tray icon in Vista.</li>
    <li><span class="bugid">Bugfix:</span> Fixed caching problem in Opera. (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1334">More</a>)</li>
    <li><span class="bugid">Bugfix:</span> Improved Windows installer. (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1291">More</a>)</li>
    <li><span class="bugid">Bugfix:</span> Fixed typos in several translations.</li>
</ul>

<a name="3.5.beta2"><h2 class="div">Subsonic 3.5.beta2 - Sep 23, 2008</h2></a>
<ul>
    <li><span class="bugid">New:</span> Fancy cover art zoom.</li>
    <li><span class="bugid">New:</span> Remove artist name from album name.</li>
    <li><span class="bugid">New:</span> Transcoding pack is now included in Windows installer.</li>
    <li><span class="bugid">New:</span> Updated German translation (thanks to J&ouml;rg Frommann) and Norwegian translation
        (thanks to jigsaw).
    </li>
    <li><span class="bugid">New:</span> Added French translation (thanks to JohnDillinger).
    <li><span class="bugid">New:</span> Added Ripserver theme.</li>
    <li><span class="bugid">Bugfix:</span> Streaming to mobile phones now works better.</li>
    <li><span class="bugid">Bugfix:</span> Made tray icon work on Vista (requires Java 6 or later).</li>
    <li><span class="bugid">Bugfix:</span> Disable random playlist functionality if user is not authorized to play
        music.
    </li>
    <li><span class="bugid">Tech:</span> Build number is now identical to Subversion revision.</li>
</ul>

<a name="3.5.beta1"><h2 class="div">Subsonic 3.5.beta1 - Jul 10, 2008</h2></a>
<ul>
    <li><span class="bugid">New:</span> Implemented music player for Java-enabled mobile phones.
        (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1203">More</a>)
    </li>
    <li><span class="bugid">New:</span> Support for personal images (avatars).</li>
    <li><span class="bugid">New:</span> Support "read-only" view.
        (<a href="http://forum.subsonic.org/forum/viewtopic.php?p=2435">More</a>)
    </li>
    <li><span class="bugid">New:</span> Simplified user interface in settings pages. Now only one "Save" button.</li>
    <li><span class="bugid">New:</span> Improved usability of coverart search page. Now also searches discogs.com.</li>
    <li><span class="bugid">New:</span> Merge artists with same name, but located in different music folders.</li>
    <li><span class="bugid">New:</span> Added option "Let others see what I am playing"</li>
    <li><span class="bugid">New:</span> Added option "Always use web player".
        (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1077">More</a>)
    </li>
    <li><span class="bugid">New:</span> Nicer controls in modern browsers (Opera 9.5, Firefox 3, IE 7).</li>
    <li><span class="bugid">Bugfix:</span> Lots of extra players are no longer created.</li>
    <li><span class="bugid">Bugfix:</span> Fix minor bug with letter appearing if only file (not directory) exists.
        (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1023">More</a>)
    </li>
    <li><span class="bugid">Bugfix:</span> Make standalone version work on Linux without X11 server.</li>
    <li><span class="bugid">Bugfix:</span> Make lyrics work again.
        (<a href="http://forum.subsonic.org/forum/viewtopic.php?p=2588">More</a>)
    </li>
    <li><span class="bugid">Bugfix:</span> Proper sorting of "The" artists.
        (<a href="http://forum.subsonic.org/forum/viewtopic.php?t=1144">More</a>)
    </li>
    <li><span class="bugid">Bugfix:</span> Make source release build.</li>
</ul>

<a name="3.4"><h2 class="div">Subsonic 3.4 - Apr 27, 2008</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added optional setting for LDAP bind DN and password.</li>
    <li><span class="bugid">New:</span> Added quick links to Google, Wikipedia, allmusic and Last.fm.</li>
    <li><span class="bugid">New:</span> Added standalone installation option.</li>
    <li><span class="bugid">Bugfix:</span> Fixed layout problems in Podcast page.</li>
    <li><span class="bugid">Bugfix:</span> Clean up partially downloaded Podcast episodes at start-up.</li>
    <li><span class="bugid">Bugfix:</span> Old Podcast episodes were in some cases not deleted.</li>
</ul>

<a name="3.4.beta1"><h2 class="div">Subsonic 3.4.beta1 - Mar 28, 2008</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added support for user authentication in LDAP, including Microsoft Active
        Directory.
    </li>
    <li><span class="bugid">New:</span> Implemented "Party mode" with a simpler user interface.</li>
    <li><span class="bugid">New:</span> Added option to specify music folder in "Random playlist".</li>
    <li><span class="bugid">New:</span> Added option "Play more random songs when end of playlist is reached" in "Random
        playlist".
    </li>
    <li><span class="bugid">New:</span> Implemented download option in WAP interface.</li>
    <li><span class="bugid">New:</span> Implemented random playlist option in WAP interface.</li>
    <li><span class="bugid">New:</span> Make index always visible (doesn't work in IE).</li>
    <li><span class="bugid">New:</span> Added play/add/download buttons for songs in left frame.</li>
    <li><span class="bugid">New:</span> Suggest track number in tag editor.</li>
    <li><span class="bugid">Bugfix:</span> Fixed faulty layout in main frame.</li>
    <li><span class="bugid">Bugfix:</span> Fixed caching bug of left frame (when changing theme etc).</li>
    <li><span class="bugid">Bugfix:</span> "Highest rated" now also shows albums that have never been played.</li>
    <li><span class="bugid">Bugfix:</span> Player selection in WAP interface now works.</li>
    <li><span class="bugid">Bugfix:</span> Support quotes in search field and shortcut field.</li>
    <li><span class="bugid">Bugfix:</span> Don't display track number zero.</li>
    <li><span class="bugid">Tech:</span> Faster start-up on Windows. Extract war file to SUBSONIC_HOME/jetty instead of
        temp directory.
    </li>
    <li><span class="bugid">Tech:</span> Made it possible to run the Jetty version on Linux and other platforms.</li>
    <li><span class="bugid">Tech:</span> Allow up to one week of idle time in Jetty.</li>
</ul>

<a name="3.3"><h2 class="div">Subsonic 3.3 - Dec 23, 2007</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added "HD-720" and "Barents Sea" themes.</li>
    <li><span class="bugid">New:</span> Improved layout of left and main frame.</li>
    <li><span class="bugid">New:</span> Sort genres in "Edit tags".</li>
    <li><span class="bugid">Bugfix:</span> Make transcoding work when combined with client-side playlist.</li>
    <li><span class="bugid">Bugfix:</span> Case-insensitive sorting of artists.</li>
    <li><span class="bugid">Bugfix:</span> Made genre parsing more robust.</li>
    <li><span class="bugid">Tech:</span> Upgraded to Acegi 1.0.5</li>
    <li><span class="bugid">Tech:</span> Upgraded to Spring 2.5</li>
    <li><span class="bugid">Tech:</span> Make it compile with Java 5.</li>
</ul>

<a name="3.3.beta1"><h2 class="div">Subsonic 3.3.beta1 - Nov 23, 2007</h2></a>
<ul>
    <li><span class="bugid">New:</span> Implemented client-side playlists, including random skipping within songs
        (see <em>Settings &gt; Players &gt; Playlist is managed by player</em>).
    </li>
    <li><span class="bugid">New:</span> Support resumable downloads.</li>
    <li><span class="bugid">New:</span> Rewrite stream URL if Subsonic is behind a proxy.</li>
    <li><span class="bugid">New:</span> Added two new themes designed for large HD screens.</li>
    <li><span class="bugid">New:</span> Added Russian translation by Iaroslav Andrusiak.</li>
    <li><span class="bugid">New:</span> Made it possible to collapse/expand Podcast episodes. Improved layout and
        usability.
    </li>
    <li><span class="bugid">New:</span> Added "Play on phone" option to wap interface.</li>
    <li><span class="bugid">New:</span> Auto-focus on username field in login page.</li>
    <li><span class="bugid">New:</span> Created new settings categories "Advanced" and "Personal".</li>
    <li><span class="bugid">New:</span> Moved index from top to left frame.</li>
    <li><span class="bugid">Bugfix:</span> Support Podcast folder that is located outside music folder.</li>
    <li><span class="bugid">Bugfix:</span> Handle track number on the form "3/12"</li>
    <li><span class="bugid">Bugfix:</span> Fixed concurrency bug when rendering wiki markup.</li>
</ul>

<a name="3.2"><h2 class="div">Subsonic 3.2 - Oct 09, 2007</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added support for editing genre in ID3 tags.</li>
    <li><span class="bugid">New:</span> Show miniature album images in "Now playing" sidebar.</li>
    <li><span class="bugid">Bugfix:</span> Handle podcasts with colons in the name.</li>
    <li><span class="bugid">Bugfix:</span> Handle podcasts without enclosures.</li>
    <li><span class="bugid">Bugfix:</span> Handle song, album and artist names with special characters (&lt;, &gt; etc)
    </li>
</ul>

<a name="3.2.beta1"><h2 class="div">Subsonic 3.2.beta1 - Sep 19, 2007</h2></a>
<ul>
    <li><span class="bugid">New:</span> Download Podcasts with the new integrated Podcast receiver.</li>
    <li><span class="bugid">New:</span> One-click installation with the new Windows installer.</li>
    <li><span class="bugid">New:</span> Fetch lyrics automatically from www.metrolyrics.com.</li>
    <li><span class="bugid">New:</span> See what others are playing in the new "Now playing" sidebar.</li>
    <li><span class="bugid">New:</span> Play music directly in the browser with the new integrated Flash-based player.
    </li>
    <li><span class="bugid">New:</span> New tag engine (entagged.sourceforge.net) supports tags in a lot of formats
        (mp3, ogg, flac, wav, wma,
        etc).
    </li>
    <li><span class="bugid">New:</span> Added confirmation dialog when deleting playlists.</li>
    <li><span class="bugid">New:</span> Remember selected music folder (in left frame) across sessions.</li>
    <li><span class="bugid">Bugfix:</span> Fixed bug with rendering multiple status charts concurrently.</li>
    <li><span class="bugid">Tech:</span> Added db admin tool.</li>
</ul>

<a name="3.1"><h2 class="div">Subsonic 3.1 - Jul 30, 2007</h2></a>
<p>(No changes since 3.1.beta2)</p>

<a name="3.1.beta2"><h2 class="div">Subsonic 3.1.beta2 - Jul 23, 2007</h2></a>
<ul>
    <li><span class="bugid">New:</span> Optimized scanning of music folders. Dramatic improvement for network disks.
    </li>
    <li><span class="bugid">Bugfix:</span> Fixed minor concurrency bug in search index creation.</li>
    <li><span class="bugid">Bugfix:</span> Make cache work if clocks on remote disks are out of sync.</li>
    <li><span class="bugid">Bugfix:</span> Ensure that cover art images are ordered alphabetically.</li>
    <li><span class="bugid">Bugfix:</span> Remove nag message for licensed users.</li>
</ul>

<a name="3.1.beta1"><h2 class="div">Subsonic 3.1.beta1 - Jun 30, 2007</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added checkboxes to songs in playlist, with option to perform operations on all
        selected songs.
    </li>
    <li><span class="bugid">New:</span> Now possible to specify genre and decade when generating random playlist.</li>
    <li><span class="bugid">New:</span> Added menu option to sort playlist by track, album or artist.</li>
    <li><span class="bugid">New:</span> Audioscrobbling is now more fault-tolerant. Will retry if Last.fm is down.</li>
    <li><span class="bugid">New:</span> Replaced Google ads with a donation request message. Donors will not see the
        message.
    </li>
    <li><span class="bugid">New:</span> Added new attribute "default" to transcodings. When "default" is true, the
        transcoding is automatically activated for new players.
    </li>
    <li><span class="bugid">New:</span> Implemented support for browser caching of left frame (with artist list).</li>
    <li><span class="bugid">New:</span> Made downsampling command configurable.</li>
    <li><span class="bugid">New:</span> Switch to "Now playing" after 3 minutes of inactivity.</li>
    <li><span class="bugid">New:</span> Album page now has link to artist.</li>
    <li><span class="bugid">Bugfix:</span> Logically delete/undelete metadata for albums that disappear/reappear on
        disk.
    </li>
    <li><span class="bugid">Bugfix:</span> Fixed ugly checkboxes in IE.</li>
</ul>

<a name="3.0"><h2 class="div">Subsonic 3.0 - Mar 22, 2007</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added Google ads.</li>
    <li><span class="bugid">Tech:</span> Upgraded <a href="http://code.google.com/p/jvorbiscomment/">jvorbiscomment</a>
        library.
    </li>
    <li><span class="bugid">Bugfix:</span> Artists from different music folders are now properly sorted.</li>
</ul>

<a name="3.0.beta2"><h2 class="div">Subsonic 3.0.beta2 - Feb 24, 2007</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added per-user bitrate limit.</li>
    <li><span class="bugid">New:</span> Show error message in browser if Subsonic home can't be created.</li>
    <li><span class="bugid">Bugfix:</span> Fixed remaining bugs (hopefully) related to folders with non-latin
        characters.
    </li>
    <li><span class="bugid">Bugfix:</span> Zooming of ID3 cover art now works.</li>
    <li><span class="bugid">Bugfix:</span> last.fm password must no longer be entered every time.</li>
</ul>

<a name="3.0.beta1"><h2 class="div">Subsonic 3.0.beta1 - Feb 04, 2007</h2></a>
<ul>
    <li><span class="bugid">New:</span> Audioscrobbling support. Automatically register what you're playing on last.fm.
    </li>
    <li><span class="bugid">New:</span> Support display and editing of OGG Vorbis tags.</li>
    <li><span class="bugid">New:</span> Display cover art embedded in ID3 tags.</li>
    <li><span class="bugid">New:</span> Dutch translation by Ronald Knot.</li>
    <li><span class="bugid">New:</span> Links to previous/next album by same artist.</li>
    <li><span class="bugid">New:</span> Change tab order in login screen.</li>
    <li><span class="bugid">Bugfix:</span> Support directory names with non-latin characters.</li>
    <li><span class="bugid">Bugfix:</span> Fixed ampersand bug in wap interface.</li>
    <li><span class="bugid">Bugfix:</span> Display hours for very long tracks.</li>
    <li><span class="bugid">Tech:</span> Upgraded Spring and Acegi to latest versions.</li>
</ul>

<a name="2.9"><h2 class="div">Subsonic 2.9 - Nov 13, 2006</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added German translation by Harald Weiss.</li>
    <li><span class="bugid">New:</span> Show server version and memory usage in help page.</li>
    <li><span class="bugid">Bugfix:</span> Don't hang if unable to resolve latest version number.</li>
    <li><span class="bugid">Bugfix:</span> Avoid duplicates in random album page.</li>
    <li><span class="bugid">Bugfix:</span> More robust ID3 parsing.</li>
    <li><span class="bugid">Bugfix:</span> More robust thumbnail scaling.</li>
    <li><span class="bugid">Bugfix:</span> Fixed bug which in some cases made it impossible to restart streams.</li>
    <li><span class="bugid">Bugfix:</span> Improve track number removal from title.</li>
</ul>

<a name="2.9.beta1"><h2 class="div">Subsonic 2.9.beta1 - Oct 16, 2006</h2></a>
<ul>
    <li><span class="bugid">Bugfix:</span> Avoid excessive disk and memory usage when (re)scanning the music library
        (bug introduced in 2.8).
    </li>
    <li><span class="bugid">Bugfix:</span> Avoid flickering PNG images in IE.</li>
    <li><span class="bugid">New:</span> Improved thumbnail quality using step-wise bilinear resampling.</li>
    <li><span class="bugid">New:</span> Thumbnails are now cached in SUBSONIC_HOME/thumbs. Expect significant speed-ups
        (after a while), in particular for huge images.
    </li>
    <li><span class="bugid">New:</span> Improved search speed by a factor of two.</li>
    <li><span class="bugid">New:</span> Miscellaneous other caching mechanisms for faster response times.</li>
    <li><span class="bugid">New:</span> Support Wiki markup in album comments.</li>
    <li><span class="bugid">New:</span> Added "Download" menu option to main album view.</li>
    <li><span class="bugid">New:</span> Ratings are now per-user. Average rating is also displayed.</li>
    <li><span class="bugid">New:</span> Now possible to delete rating (for current user).</li>
    <li><span class="bugid">New:</span> Added Spanish translation by Jorge Bueno Magdalena. Gracias, Jorge!</li>
    <li><span class="bugid">New:</span> New Subsonic logo.</li>
    <li><span class="bugid">New:</span> Keep bitrate statistics even if player reconnects.</li>
    <li><span class="bugid">New:</span> Support editing of track numbers in ID3 tags.</li>
    <li><span class="bugid">New:</span> Show full file name as tool tip in ID3 tag editor.</li>
    <li><span class="bugid">New:</span> Search results are now better sorted.</li>
</ul>

<a name="2.8"><h2 class="div">Subsonic 2.8 - Sep 07, 2006</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added option for transcoders to obey the player max bitrate setting.</li>
    <li><span class="bugid">New:</span> Sort songs by track number.</li>
    <li><span class="bugid">New:</span> Added transcoding support for Shorten and WavPack.</li>
    <li><span class="bugid">New:</span> Improved caching of cover art images in browser.</li>
    <li><span class="bugid">New:</span> Scroller color in Sandstorm theme.</li>
    <li><span class="bugid">New:</span> Updated Norwegian translation.</li>
    <li><span class="bugid">New:</span> Added Simplified Chinese translation by Neil Gao (currently incomplete).</li>
    <li><span class="bugid">Bugfix:</span> Fixed performance problem with "Home" page.</li>
    <li><span class="bugid">Bugfix:</span> Fixed broken wap interface.</li>
    <li><span class="bugid">Bugfix:</span> Fixed playlist autoscroll.</li>
    <li><span class="bugid">Bugfix:</span> Proper rendering of directories with both files and sub-directories.</li>
</ul>

<a name="2.8.beta1"><h2 class="div">Subsonic 2.8.beta1 - Aug 13, 2006</h2></a>
<ul>
    <li><span class="bugid">New:</span> Transcoding plugin framework.</li>
    <li><span class="bugid">New:</span> VBR (variable bitrate) detection and resampling.</li>
    <li><span class="bugid">New:</span> Added form-based login with remember-me.</li>
    <li><span class="bugid">New:</span> Added logout option.</li>
    <li><span class="bugid">New:</span> Support per-user language and theme.</li>
    <li><span class="bugid">New:</span> Show track number, genre, format, duration and file size.</li>
    <li><span class="bugid">New:</span> Configurable level of song details.</li>
    <li><span class="bugid">New:</span> Various artists detection.</li>
    <li><span class="bugid">New:</span> Added option to stream over plain HTTP when using SSL.</li>
    <li><span class="bugid">New:</span> Mouseover tooltip for truncated text and cover art images.</li>
    <li><span class="bugid">New:</span> Optional notification of new final and beta releases.</li>
    <li><span class="bugid">New:</span> Nicer error page.</li>
    <li><span class="bugid">New:</span> Added "Back in black" and "Sandstorm" themes.</li>
    <li><span class="bugid">New:</span> Exclude files and folders listed in "subsonic_exclude.txt"</li>
    <li><span class="bugid">Tech:</span> New security implementation based on Acegi (http://acegisecurity.org/)</li>
    <li><span class="bugid">Tech:</span> Springified WAP pages and servlets.</li>
    <li><span class="bugid">Tech:</span> Springified DWR.</li>
    <li><span class="bugid">Tech:</span> Springified DAO's and data source.</li>
</ul>

<a name="2.7"><h2 class="div">Subsonic 2.7 - Jun 12, 2006</h2></a>
<ul>
    <li><span class="bugid">Tech:</span> Subsonic was completely rewritten to use the Spring MVC framework.</li>
    <li><span class="bugid">New:</span> Theme support. Now ships with two themes, "Subsonic Default" and "2 minutes to
        midnight". Theme authors are encouraged to contribute.
    </li>
    <li><span class="bugid">New:</span> Ajax-based ID3 tag editor.</li>
    <li><span class="bugid">New:</span> Cover art 3D effect and popup. (3D effect not available in Internet Explorer).
    </li>
    <li><span class="bugid">New:</span> Added options to limit bandwidth for downloads and uploads.</li>
    <li><span class="bugid">New:</span> Added progress bar to upload page.</li>
    <li><span class="bugid">New:</span> Show upload (as well as download and streaming) charts in status page.</li>
    <li><span class="bugid">New:</span> Added option to configure shortcuts to certain folders.</li>
    <li><span class="bugid">New:</span> Rating where you can see how much each user has uploaded/downloaded/streamed.
    </li>
    <li><span class="bugid">New:</span> Nicer layout and graphics.</li>
    <li><span class="bugid">New:</span> Added option to download playlists, both current and previously saved ones.</li>
    <li><span class="bugid">New:</span> Now logs to c:/subsonic/subsonic.log (Windows) or /var/subsonic/subsonic.log
        (other platforms).
    </li>
    <li><span class="bugid">New:</span> Support symbolic links on Unix.</li>
    <li><span class="bugid">New:</span> Support XSPF playlist format.</li>
    <li><span class="bugid">New:</span> Create backup of old image file when changing cover art.</li>
    <li><span class="bugid">Bugfix:</span> Avoid wrapped lines if browser window is small.</li>
    <li><span class="bugid">Bugfix:</span> Use proper Y-range in bandwidth charts.</li>
    <li><span class="bugid">Bugfix:</span> Integer overflow in search index creation interval.</li>
    <li><span class="bugid">Bugfix:</span> Some Amazon search didn't show any results.</li>
    <li><span class="bugid">Bugfix:</span> Now possible to change cover art even if original file is write protected
        (Windows only).
    </li>
</ul>

<a name="2.6"><h2 class="div">Subsonic 2.6 - Mar 10, 2006</h2></a>
<ul>
    <li><span class="bugid">New:</span> Nicer layout, colors and icons. Customizable welcome message. Option to limit
        number of cover art images to display.
    </li>
    <li><span class="bugid">New:</span> You can now specify multiple media folders in the configuration.
        This is useful, for example, if you have your music on multiple disks, or if you have one
        directory with music, and another with movies. The index (on the left-hand side) can either show all
        media folders (merged alphabetically), or you can select from a combo box which folder to display.
    </li>
    <li><span class="bugid">New:</span> Assign ratings (one to five stars) and comments to individual albums.</li>
    <li><span class="bugid">New:</span> There is a new welcome page, displaying lists of random albums, newest albums,
        highest rated albums, most often played albums and most recently played albums.
    </li>
    <li><span class="bugid">New:</span> You can configure a set of links to Internet TV and radio stations. These links
        become available in the index on the left-hand side. Click on a link, and your player connects to the station.
    </li>
    <li><span class="bugid">New:</span> Saved playlists are now available as Podcasts (available from the "More" page).
    </li>
    <li><span class="bugid">New:</span> Improved player management. Support multiple players with the same IP address,
        and players with dynamic IP addresses. Display player type (e.g., WinAmp) and last-seen date.
    </li>
    <li><span class="bugid">New:</span> Macedonian translation by Stefan Ivanovski.</li>
    <li><span class="bugid">New:</span> Implemented support for non-Latin character encodings (for instance Japanese or
        Cyrillic).
    </li>
    <li><span class="bugid">New:</span> Added option to load a previously saved playlist in the WAP interface.</li>
    <li><span class="bugid">New:</span> "Album Info" now integrates with Google Music.</li>
    <li><span class="bugid">New:</span> Avoid unnecessary reloading of the playlist window.</li>
    <li><span class="bugid">Bugfix:</span> Fallback to file name if ID3 tags are present but empty.</li>
    <li><span class="bugid">Bugfix:</span> LAME now works on Linux.</li>
    <li><span class="bugid">Bugfix:</span> Solved problem with playlist autoscroll on rearrange.</li>
    <li><span class="bugid">Bugfix:</span> Clicking index in top frame sometimes caused reloading of left frame.</li>
</ul>

<a name="2.5"><h2 class="div">Subsonic 2.5 - Nov 25, 2005</h2></a>
<ul>
    <li><span class="bugid">New:</span> Implemented user management. Users are easily created, deleted and assigned
        different privileges.
    </li>
    <li><span class="bugid">New:</span> Faster song switching. Players react immediately to playlist changes.</li>
    <li><span class="bugid">New:</span> Now possible to refine search for cover art and album info.</li>
    <li><span class="bugid">New:</span> Better support for OGG, AAC and other formats. Added proper suffix to stream URL
        as a hint to the player. </li>
    <li><span class="bugid">New:</span> Display media library statistics.</li>
    <li><span class="bugid">New:</span> New location for preferences, search index and database (c:\subsonic or
        /var/subsonic).
    </li>
    <li><span class="bugid">Bugfix:</span> Improved parsing of MP3 tags. Avoid funny characters in artist, album and
        song title.
    </li>
</ul>

<a name="2.4"><h2 class="div">Subsonic 2.4 - Oct 10, 2005</h2></a>
<ul>
    <li><span class="bugid">New:</span> Download cover art and album info from Amazon web service.</li>
    <li><span class="bugid">New:</span> Show selection of random albums on welcome page.</li>
    <li><span class="bugid">New:</span> Display notice if LAME is not installed.</li>
    <li><span class="bugid">Bugfix:</span> Allow max one stream per player.</li>
    <li><span class="bugid">Bugfix:</span> Use user-wide (not system-wide) preferences on non-Windows platforms.</li>
</ul>

<a name="2.3"><h2 class="div">Subsonic 2.3 - May 28, 2005</h2></a>
<ul>
    <li><span class="bugid">New:</span> Full internationalization support.</li>
    <li><span class="bugid">New:</span> Automatic update of search index at specified intervals.</li>
    <li><span class="bugid">New:</span> Search is now more like Google.</li>
    <li><span class="bugid">New:</span> Updating search index is now a lot faster (30 seconds for 18000 songs).</li>
    <li><span class="bugid">New:</span> Unique playlist "undo" function.</li>
    <li><span class="bugid">New:</span> "Now Playing" automatically refreshes when a new album is played.</li>
    <li><span class="bugid">New:</span> Ongoing downloads are now displayed in the status page, with charts showing
        download speed.
    </li>
    <li><span class="bugid">New:</span> Some improvements in zip functionality.</li>
    <li><span class="bugid">New:</span> Significantly improved accuracy in charts.</li>
    <li><span class="bugid">New:</span> Better support for non-ASCII characters in SHOUTcast.</li>
    <li><span class="bugid">New:</span> No longer necessary to enter username and password in player.</li>
    <li><span class="bugid">New:</span> Support for "ignored articles".</li>
    <li><span class="bugid">New:</span> Show log in help page.</li>
</ul>

<a name="2.2"><h2 class="div">Subsonic 2.2 - March 17, 2005</h2></a>
<ul>
    <li><span class="bugid">New:</span> Implemented SHOUTcast support.</li>
    <li><span class="bugid">New:</span> Added "Album info" which links to reviews etc at allmusic.com.</li>
    <li><span class="bugid">New:</span> Support for uploading files from the browser to the Subsonic server. Zip-files
        are automatically unpacked.
    </li>
    <li><span class="bugid">New:</span> Improved usability in search interface.</li>
    <li><span class="bugid">New:</span> Option to specify http://yourhostname/stream/file.ogg etc.</li>
    <li><span class="bugid">Bugfix:</span> Detect zero-terminated strings in ID3 tags.</li>
    <li><span class="bugid">Bugfix:</span> Use ID3v2 tags instead of ID3v1, if both are present.</li>
</ul>

<a name="2.1"><h2 class="div">Subsonic 2.1 - March 8, 2005</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added more search options.</li>
    <li><span class="bugid">New:</span> Added support for downloading an entire directory structure as a zip file.</li>
    <li><span class="bugid">New:</span> Subsonic now parses song titles, artists and albums from ID3 tags.</li>
    <li><span class="bugid">New:</span> Settings for cover art size and max bitrate can now be set for individual
        players.
    </li>
    <li><span class="bugid">New:</span> Search index is now generated in the background.</li>
    <li><span class="bugid">New:</span> Keep position in playlist frame (e.g., always show the currently playing track).
    </li>
    <li><span class="bugid">Bugfix:</span> Some minor bug fixes.</li>
</ul>

<a name="2.0"><h2 class="div">Subsonic 2.0 - February 27, 2005</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added WAP interface for controlling Subsonic from a mobile phone or PDA.</li>
    <li><span class="bugid">New:</span> Added option to generate a random playlist.</li>
    <li><span class="bugid">New:</span> Added option to download and save individual songs.</li>
    <li><span class="bugid">New:</span> Subsonic now alerts users if a new version is available.</li>
    <li><span class="bugid">New:</span> Improved documentation and usability.</li>
    <li><span class="bugid">New:</span> ANT support.</li>
</ul>

<a name="1.0"><h2 class="div">Subsonic 1.0 - February 10, 2005</h2></a>
<ul>
    <li><span class="bugid">New:</span> Added chart for displaying bitrates last few minutes.</li>
    <li><span class="bugid">New:</span> Implemented transcoding to lower bitrates.</li>
    <li><span class="bugid">New:</span> Added online help.</li>
    <li><span class="bugid">New:</span> Support PLS and M3U playlists.</li>
</ul>

<a name="0.1"><h2 class="div">Subsonic 0.1 - December 14, 2004</h2></a>
<ul>
    <li>Initial release.</li>
</ul>

</div>

<div id="side-col">
    <%@ include file="google-translate.jsp" %>
    <div class="sidebox">
        <h2>Releases</h2>
        <ul class="list">
            <li><a href="#4.7.beta2">Subsonic 4.7.beta2</a></li>
            <li><a href="#4.7.beta1">Subsonic 4.7.beta1</a></li>
            <li><a href="#4.6">Subsonic 4.6</a></li>
            <li><a href="#4.6.beta2">Subsonic 4.6.beta2</a></li>
            <li><a href="#4.6.beta1">Subsonic 4.6.beta1</a></li>
            <li><a href="#4.5">Subsonic 4.5</a></li>
            <li><a href="#4.5.beta2">Subsonic 4.5.beta2</a></li>
            <li><a href="#4.5.beta1">Subsonic 4.5.beta1</a></li>
            <li><a href="#4.4">Subsonic 4.4</a></li>
            <li><a href="#4.4.beta1">Subsonic 4.4.beta1</a></li>
            <li><a href="#4.3">Subsonic 4.3</a></li>
            <li><a href="#4.3.beta1">Subsonic 4.3.beta1</a></li>
            <li><a href="#4.2">Subsonic 4.2</a></li>
            <li><a href="#4.2.beta1">Subsonic 4.2.beta1</a></li>
            <li><a href="#4.1">Subsonic 4.1</a></li>
            <li><a href="#4.1.beta1">Subsonic 4.1.beta1</a></li>
            <li><a href="#4.0.1">Subsonic 4.0.1</a></li>
            <li><a href="#4.0">Subsonic 4.0</a></li>
            <li><a href="#4.0.beta2">Subsonic 4.0.beta2</a></li>
            <li><a href="#4.0.beta1">Subsonic 4.0.beta1</a></li>
            <li><a href="#3.9">Subsonic 3.9</a></li>
            <li><a href="#3.9.beta1">Subsonic 3.9.beta1</a></li>
            <li><a href="#3.8">Subsonic 3.8</a></li>
            <li><a href="#3.8.beta1">Subsonic 3.8.beta1</a></li>
            <li><a href="#3.7">Subsonic 3.7</a></li>
            <li><a href="#3.7.beta1">Subsonic 3.7.beta1</a></li>
            <li><a href="#3.6">Subsonic 3.6</a></li>
            <li><a href="#3.6.beta2">Subsonic 3.6.beta2</a></li>
            <li><a href="#3.6.beta1">Subsonic 3.6.beta1</a></li>
            <li><a href="#3.5">Subsonic 3.5</a></li>
            <li><a href="#3.5.beta2">Subsonic 3.5.beta2</a></li>
            <li><a href="#3.5.beta1">Subsonic 3.5.beta1</a></li>
            <li><a href="#3.4">Subsonic 3.4</a></li>
            <li><a href="#3.4">Subsonic 3.4.beta1</a></li>
            <li><a href="#3.3">Subsonic 3.3</a></li>
            <li><a href="#3.3.beta1">Subsonic 3.3.beta1</a></li>
            <li><a href="#3.2">Subsonic 3.2</a></li>
            <li><a href="#3.2.beta1">Subsonic 3.2.beta1</a></li>
            <li><a href="#3.1">Subsonic 3.1</a></li>
            <li><a href="#3.1.beta2">Subsonic 3.1.beta2</a></li>
            <li><a href="#3.1.beta1">Subsonic 3.1.beta1</a></li>
            <li><a href="#3.0">Subsonic 3.0</a></li>
            <li><a href="#3.0.beta2">Subsonic 3.0.beta2</a></li>
            <li><a href="#3.0.beta1">Subsonic 3.0.beta1</a></li>
            <li><a href="#2.9">Subsonic 2.9</a></li>
            <li><a href="#2.9.beta1">Subsonic 2.9.beta1</a></li>
            <li><a href="#2.8">Subsonic 2.8</a></li>
            <li><a href="#2.8.beta1">Subsonic 2.8.beta1</a></li>
            <li><a href="#2.7">Subsonic 2.7</a></li>
            <li><a href="#2.6">Subsonic 2.6</a></li>
            <li><a href="#2.5">Subsonic 2.5</a></li>
            <li><a href="#2.4">Subsonic 2.4</a></li>
            <li><a href="#2.3">Subsonic 2.3</a></li>
            <li><a href="#2.2">Subsonic 2.2</a></li>
            <li><a href="#2.1">Subsonic 2.1</a></li>
            <li><a href="#2.0">Subsonic 2.0</a></li>
            <li><a href="#1.0">Subsonic 1.0</a></li>
            <li><a href="#0.1">Subsonic 0.1</a></li>
        </ul>
    </div>

    <%@ include file="donate.jsp" %>

</div>

<div class="clear">
</div>
</div>
<hr/>
<%@ include file="footer.jsp" %>
</div>


</body>
</html>
