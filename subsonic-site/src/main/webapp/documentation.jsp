<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "documentation"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
    <%@ include file="menu.jsp" %>

    <div id="content">
        <div id="main-col">
            <h1>Documentation</h1>

            <ul class="list" style="padding-top:1em">
                <li><b><a href="installation.jsp">Installation</a></b><br>
                    How to install Subsonic on Windows, Mac, Linux and other platforms.
                </li>
                <li><b><a href="getting-started.jsp">Getting started</a></b><br>
                    How to set up music folders, remote access etc.
                </li>
                <li><b><a href="faq.jsp">FAQ</a></b><br>
                    Frequently asked questions.
                </li>
                <li><b><a href="http://monroeworld.com/android/subsonic/">A practical guide to installing and configuring Subsonic</a></b><br>
                By Shane R. Monroe, Monroeworld.com.
                </li>
                <li><b><a href="https://sourceforge.net/apps/mediawiki/subsonic/index.php?title=Subsonic">Wiki</a></b><br>
                    Wiki pages contributed by your fellow Subsonic users.
                </li>
                <li><b><a href="forum.jsp">Forum</a></b><br>
                    Discuss and ask questions to fellow users. Roughly 30 new posts per day.
                </li>
                <li><b><a href="transcoding.jsp">Transcoding</a></b><br>
                    Detailed documentation of how Subsonic automatically converts between music formats.
                </li>
                <li><b><a href="translate.jsp">Translation</a></b><br>
                    How to translate Subsonic to a new language.
                </li>
                <li><b><a href="api.jsp">API documentation</a></b><br>
                    How to access Subsonic using the REST API. (For developers)
                </li>

            </ul>

        </div>

        <div id="side-col">
            <%@ include file="google-translate.jsp" %>
            <%@ include file="download-subsonic.jsp" %>
        </div>

        <div class="clear">
        </div>
    </div>
    <hr/>
    <%@ include file="footer.jsp" %>
</div>


</body>
</html>
