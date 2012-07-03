<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "installation"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"></a>

<div id="container">
<%@ include file="menu.jsp" %>

<div id="content">
<div id="main-col">
<h1 class="bottomspace">Installing Subsonic</h1>
<p>
    Subsonic features several flexible installation options.  Select the one which suits you best:
</p>

<ul class="list">
    <li><a href="#windows">Windows installation</a></li>
    <li><a href="#mac">Mac installation</a></li>
    <li><a href="#debian">Debian/Ubuntu installation</a></li>
    <li><a href="#rpm">Red Hat/Fedora installation</a></li>
    <li><a href="#standalone">Stand-alone installation</a></li>
    <li><a href="#war">WAR installation</a></li>
</ul>

<a href="inc/img/change-password.png"><img src="inc/img/change-password-small.png" alt="Change password" class="img-right"/></a>
<p class="bottomspace"><b>Important!</b>
    Immediately after installing Subsonic you should change the admin password to secure the server.
    Point your browser to the Subsonic web page (see URL below), and log in with username <code>admin</code> and password
    <code>admin</code>.  Go to <code>Settings &gt; Users</code> to change password and create new users.
</p>

<div class="featureitem">
    <a name="windows"></a>
    <div class="heading"><img src="inc/img/download-windows.png" alt="" height="16" width="16"/> Windows installation <a href="#top" class="backtotop" title="Top"><img src="inc/img/top.gif" alt="" height="16" width="16"/></a></div>
    <div class="content">
        <div class="wide-content">
            <p>
                On Windows, the easiest way to install Subsonic is using the provided Windows Installer: <code>subsonic-x.x-setup.exe</code>.
            </p>
            <a href="inc/img/windows-installer.png"><img class="img-right" src="inc/img/windows-installer-small.png" alt="Windows Installer"/></a>
            <p>
                It installs Subsonic as a service, available in <code>Control Panel &gt; Administrative Tools &gt; Services</code>, that is started automatically when you log on to Windows.
            </p>
            <p>
                It also creates links in the Start Menu: <code>Start &gt; All Programs &gt; Subsonic</code>. From this menu you can start/stop the Subsonic service, open the Subsonic web page, or
                change basic settings such as which port number Subsonic should use. You can also click on the little yellow submarine tray icon.
            </p>
            <p>
                If you're upgrading an existing Subsonic installation, you don't have to uninstall the old version first. In any case, the existing Subsonic settings are preserved.
            </p>
            <p>
                After installing and starting Subsonic, open the Subsonic web page on <a href="http://localhost">http://localhost</a>.
            </p>
        </div>
    </div>
</div>

<div class="featureitem">
    <a name="mac"></a>
    <div class="heading"><img src="inc/img/download-mac.png" alt="" height="16" width="16"/> Mac installation <a href="#top" class="backtotop" title="Top"><img src="inc/img/top.gif" alt="" height="16" width="16"/></a></div>
    <div class="content">
        <div class="wide-content">
            <a href="inc/img/mac-installer.png"><img class="img-right" src="inc/img/mac-installer-small.png" alt="Mac Installer"/></a>
            <p>
                Download the installation package and run it. This installs Subsonic.app in Applications. Start it by opening Applications and
                click the Subsonic icon.  A small headset icon will appear in the menu bar. If you want Subsonic to start automatically when you log in,
                open <b>System Preferences &gt; Accounts</b> and add Subsonic to the list of <b>Login Items</b>.
            </p>
            <p>
                After installing and starting Subsonic, open the Subsonic web page on <a href="http://localhost:4040">http://localhost:4040</a>.
            </p>
            <p><b>Advanced configuration</b></p>
            <p>
                To change the port number, Java memory settings or other startup parameters, edit <code style="white-space:nowrap;">/Applications/Subsonic.app/Contents/Info.plist</code>.
            </p>
            <p><b>Troubleshooting</b></p>
            <p>
                If Subsonic doesn't start properly you can troubleshoot it by starting it from the command line:
                <code style="white-space:nowrap;" >/Applications/Subsonic.app/Contents/MacOS/JavaApplicationStub</code>.
            </p>
            <p>
                Also check the logs in <code style="white-space:nowrap;">/Library/Application Support/Subsonic</code>.
            </p>

        </div>
    </div>
</div>

<div class="featureitem">
    <a name="debian"></a>
    <div class="heading"><img src="inc/img/download-ubuntu.png" alt="" height="16" width="16"/> Debian/Ubuntu installation <a href="#top" class="backtotop" title="Top"><img src="inc/img/top.gif" alt="" height="16" width="16"/></a></div>
    <div class="content">
        <div class="wide-content">
            <p>
                This installation option applies to Debian Linux, including Ubuntu.
            </p>
            <ul>
                <li>First, install Java: <code>sudo apt-get install openjdk-6-jre</code></li>
                <li><a href="download.jsp">Download</a> the Subsonic .deb package and install it: <code>sudo dpkg -i subsonic-x.x.deb</code></li>
            </ul>
            <p>
                Notice that the installer configures your system to start Subsonic automatically when booting.
                After installing, open the Subsonic web page on <a href="http://localhost:4040">http://localhost:4040</a>.
            </p>

            <p><b>Change user</b></p>
            <p>
                By default the Subsonic process is run as the root user. To improve security it is strongly advised that you run it as a
                dedicated user with less powerful privileges. You can specify this user by setting the SUBSONIC_USER variable in
                <code style="white-space:nowrap;">/etc/default/subsonic</code>.
            </p>
            <p>
                Note that non-root users are by default not allowed to use ports below 1024. Also make sure to grant the user write permissions in
                the music directories, otherwise changing album art and tags will fail.
            </p>

            <p><b>Advanced configuration</b></p>
            <p>
                To change the port number, Java memory settings or other startup parameters, edit the SUBSONIC_ARGS variable in
                <code style="white-space:nowrap;">/etc/default/subsonic</code>. To restart Subsonic, do
                <code style="white-space:nowrap;">sudo service subsonic restart</code>
            </p>

            <p><b>Troubleshooting</b></p>
            <p>
                Check the logs in <code style="white-space:nowrap;">/var/subsonic</code>.
            </p>

        </div>
    </div>
</div>

<div class="featureitem">
    <a name="rpm"></a>
    <div class="heading"><img src="inc/img/download-fedora.png" alt="" height="16" width="16"/> Red Hat/Fedora installation <a href="#top" class="backtotop" title="Top"><img src="inc/img/top.gif" alt="" height="16" width="16"/></a></div>
    <div class="content">
        <div class="wide-content">
            <p>
                This installation option applies to Red Hat Linux, including Fedora.
            </p>
            <ul>
                <li>First, install Java: <code>sudo yum install java-1.6.0-openjdk</code></li>
                <li><a href="download.jsp">Download</a> the Subsonic .rpm installation package and install it: <code style="white-space:nowrap;">sudo yum install --nogpgcheck subsonic-x.x.rpm</code></li>
                <li>If nothing happens, try to remove the old version first (all settings will be retained): <code style="white-space:nowrap;">sudo yum remove subsonic</code></li>
            </ul>
            <p>
                Notice that the installer configures your system to start Subsonic automatically when booting.
                After installing, open the Subsonic web page on <a href="http://localhost:4040">http://localhost:4040</a>.
            </p>

            <p><b>Change user</b></p>
            <p>
                By default the Subsonic process is run as the root user. To improve security it is strongly advised that you run it as a
                dedicated user with less powerful privileges. You can specify this user by setting the SUBSONIC_USER variable in
                <code style="white-space:nowrap;">/etc/sysconfig/subsonic</code>.
            </p>
            <p>
                Note that non-root users are by default not allowed to use ports below 1024. Also make sure to grant the user write permissions in
                the music directories, otherwise changing album art and tags will fail.
            </p>

            <p><b>Advanced configuration</b></p>
            <p>
                To change the port number, Java memory settings or other startup parameters, edit the SUBSONIC_ARGS variable in
                <code style="white-space:nowrap;">/etc/sysconfig/subsonic</code>. To restart Subsonic, do
                <code style="white-space:nowrap;">sudo service subsonic restart</code>
            </p>

            <p><b>Troubleshooting</b></p>
            <p>
                Check the logs in <code style="white-space:nowrap;">/var/subsonic</code>.
            </p>

        </div>
    </div>
</div>

<div class="featureitem">
    <a name="standalone"></a>
    <div class="heading">Stand-alone installation <a href="#top" class="backtotop" title="Top"><img src="inc/img/top.gif" alt="" height="16" width="16"/></a></div>
    <div class="content">
        <div class="wide-content">
            <p><b>Requirements: </b>Sun Java 5 or later (<a href="http://www.java.com">Download</a>)</p>
            <p>
                This is Subsonic with an embedded Jetty server, and can be installed on any Windows, Linux, Unix, FreeNAS, unRAID etc operating system.
            </p>
            <ul>
                <li>Unpack <code>subsonic-x.x-standalone.tar.gz</code> to <code>SUBSONIC_HOME/standalone</code>.
                    <code>SUBSONIC_HOME</code> is typically <code>c:\subsonic</code> on Windows, and <code>/var/subsonic</code> on Unix-based operating systems.</li>
                <li>Optionally configure the startup script <code>SUBSONIC_HOME/standalone/subsonic.sh</code></li>
                <li>Execute the startup script. (Typically you will configure your operating system to execute the script automatically at start-up.)</li>
                <li>Open the Subsonic web page. The default address is <a href="http://localhost:4040">http://localhost:4040</a>.</li>
            </ul>
        </div>
    </div>
</div>

<div class="featureitem">
    <a name="war"></a>

    <div class="heading">WAR installation <a href="#top" class="backtotop" title="Top"><img src="inc/img/top.gif" alt="" height="16" width="16"/></a></div>
    <div class="content">
        <div class="wide-content">
            <p><b>Requirements: </b>Java 5 or later (<a href="http://www.java.com">Download</a>). A servlet container supporting Servlet 2.4 and JSP 2.0.</p>

            <p>
                Use this option if you want to deploy Subsonic in an external server, such as Tomcat, Jetty, GlassFish or Geronimo. Subsonic comes
                packaged as a standard Java web application, <code>subsonic.war</code>, which can be easily deployed in any compatible servlet container.
                The most commonly used server is Tomcat, and the rest of this section describes how to install or upgrade Subsonic on a Tomcat server.
                (Please note that Subsonic currently doesn't work with Tomcat&nbsp;7.)
            </p>

            <ul>
                <li>Stop Tomcat if it's running.</li>
                <li>Remove these files and directories if they exist:
                    <ul>
                        <li><code>TOMCAT_HOME/webapps/subsonic.war</code></li>
                        <li><code>TOMCAT_HOME/webapps/subsonic</code></li>
                        <li><code>TOMCAT_HOME/work</code></li>
                    </ul>
                </li>
                <li>Copy the file <code>subsonic.war</code> to <code>TOMCAT_HOME/webapps</code>.</li>
                <li>Start Tomcat.</li>
                <li>Point your web browser to <a href="http://localhost/subsonic">http://localhost/subsonic</a> (or
                    <a href="http://localhost:8080/subsonic">http://localhost:8080/subsonic</a> if you installed Tomcat on port 8080.)</li>
            </ul>

            <p><b>Notes</b></p>
            <ul>
                <li> If you installed Tomcat as a Windows Service, you can start and stop it from the service manager: <code>Control Panel &gt; Administrative Tools &gt; Services</code>.</li>
                <li><code>TOMCAT_HOME</code> refers to the directory in which you installed Tomcat. On Windows this is normally <code>C:\Program Files\Apache Software Foundation\Tomcat 5.5</code></li>
                <li>On Linux, Subsonic keeps its files in <code>/var/subsonic</code>. Depending on your configuration, Tomcat may not be permitted to create this directory, in which case
                    you have to create it manually: <code>mkdir /var/subsonic; chown tomcat:tomcat /var/subsonic</code></li>
            </ul>

            <p><b>Troubleshooting</b></p>
            <p>
                If you experience any problems, please make sure you follow the installation instructions above. Here's a list of suggestions of what to do if it still doesn't work:
            </p>
            <ul>
                <li>Restart Tomcat.</li>
                <li>Reinstall Subsonic.</li>
                <li>Upgrade to the latest Tomcat version (at least if you're using Tomcat 5.0 or earlier).</li>
                <li>Look for errors in the Subsonic log, <code>c:\subsonic\subsonic.log</code> or <code>/var/subsonic/subsonic.log</code>.</li>
                <li>Look for errors in the Tomcat logs, <code>TOMCAT_HOME/logs</code>.</li>
                <li>Post a message to the <a href="forum.jsp">Subsonic forum</a>. Please let us know what Subsonic version, Tomcat version and
                    operating system you're using. Also, please give a detailed description of the problem. This way, we'll be able to help you faster.</li>
            </ul>
        </div>
    </div>
</div>

</div>

<div id="side-col">
    <%@ include file="google-translate.jsp" %>
    <%@ include file="tutorial.jsp" %>
    <%@ include file="donate.jsp" %>
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
