<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "translate"; %>
<%@ include file="header.jsp" %>

<body>

<div id="container">
<%@ include file="menu.jsp" %>

<div id="content">
<div id="main-col">
<h1 class="bottomspace">Translating Subsonic</h1>

    <p>
        Here's how to translate Subsonic to a new language. This description assumes that you're using the Windows installer
        version of Subsonic.
    </p>

    <p>
        In the following, <code>&lt;ROOT&gt;</code> refers to the directory in which you installed Subsonic
        &ndash; normally <code>c:\Program Files\Subsonic</code>.
    </p>
    <ol>
        <li>Stop the Subsonic service if it's running.</li>
        <li>Rename <code>&lt;ROOT&gt;\subsonic.war</code> to <code>&lt;ROOT&gt;\subsonic.zip</code>.</li>
        <li>Unzip the zip file into a new <em>directory</em> called <code>&lt;ROOT&gt;\subsonic.war</code>.
        <li>Find the two-letter ISO-639 code for your language &ndash; a list is available <a href="http://www.loc.gov/standards/iso639-2/php/English_list.php">here</a>.
        For instance, Italian is assigned the code <code>it</code>.</li>
        <li>Add the ISO-639 code to the file <code>&lt;ROOT&gt;\subsonic.war\WEB_INF\classes\net\sourceforge\subsonic\i18n\locales.txt</code>.</li>
        <li>In the same directory is the file <code>ResourceBundle_en.properties</code> which is the English translation. Use this (or any other file)
            as the basis for your translation. Better yet, use the latest version from the
            <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/resources/net/sourceforge/subsonic/i18n/">SVN repository</a>.</li>
        <li>Create the new resource file, for instance <code>ResourceBundle_it.properties</code>.</li>
        <li>Translate the text in the new file.</li>
        <li>If you are using a non-Latin alphabet (e.g., Cyrillic or Japanese), you have to convert the property file to ASCII before using it.
            Use the <code>native2ascii</code> tool which is included in the Java Development Kit. For instance, if you're writing a Macedonian translation
            (using the Cyrillic alphabet) using UTF-16 character encoding, you must convert it as follows: <br/><br/>

            <code>native2ascii -encoding utf-16 c:\develop\ResourceBundle_mk.properties &lt;ROOT&gt;/subsonic.war/WEB_INF/classes/net/sourceforge/subsonic/i18n/ResourceBundle_mk.properties</code>
        </li>
        <li>You have to restart Subsonic for the changes to have effect.</li>
        <li>When you're done, send the new language file to <a href="mailto:sindre@activeobjects.no">sindre@activeobjects.no</a>.</li>
    </ol>

</div>

    <div id="side-col">
        <%@ include file="google-translate.jsp" %>
        <%@ include file="donate.jsp" %>
        <%@ include file="merchandise.jsp" %>
    </div>

    <div class="clear">
    </div>
</div>
    <hr/>
    <%@ include file="footer.jsp" %>
</div>


</body>
</html>
