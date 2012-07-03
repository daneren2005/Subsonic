<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "getting-started"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
    <%@ include file="menu.jsp" %>

    <div id="content">
        <div id="main-col">
            <h1 class="bottomspace">Getting Started</h1>

            <p>
                This guide assumes that you have successfully installed the Subsonic server on your computer. If not,
                please refer to the <a href="installation.jsp">installation instructions</a>.
            </p>
            <p>
                After installing and starting Subsonic, open the Subsonic web page. The web address may differ depending
                on your installation options, but is typically <a href="http://localhost" target="_blank">http://localhost</a>
                or <a href="http://localhost:4040" target="_blank">http://localhost:4040</a>.
            </p>

            <a name="1"><h2>1 Setting up music folders</h2></a>

            <p>You must tell Subsonic where you keep your music. Select <b>Settings &gt; Music folders</b> to add one or
                more folders.</p>

            <p>Note that if you add more than one music folder, a list will appear on the left side of the screen where you can
                select the active folder.</p>

            <a href="inc/img/gettingstarted/musicfolders.png"><img src="inc/img/gettingstarted/musicfolders_thumb.png" alt=""/></a>

            <p>Also note that Subsonic will organize your music according to how they are organized on your disk.
                Unlike many other music applications, Subsonic does not organize the music according to the tag information
                embedded in the files. (It does, however, also read the tags for presentation and search purposes.)</p>

            <p>Consequently, it's recommended that the music folders you add to Subsonic are organized in an
                <span style="white-space:nowrap;">"artist/album/song"</span> manner. There are music managers, like
                <a href="http://www.mediamonkey.com/" target="_blank">MediaMonkey</a>, that can help you achieve this.</p>

            <a name="2"><h2>2 Setting up remote access</h2></a>
            <p>With Subsonic you can access your music anywhere on the internet or from your iPhone or Android phone.
                However, in order to do that certain conditions must be met:</p>

            <ul class="list">
                <li>Your router must have a public IP address and must be accessible from the internet. This is true for most home internet connections.</li>
                <li>Your router must support the UPnP or NAT-PMP protocol so that Subsonic can setup the appropriate port forwarding. If your router doesn't support
                    this you will have to configure the port forwarding manually.</li>
            </ul>

            <a name="2.1"><h3>2.1 Automatic port forwarding</h3></a>
            <p>To enable automatic port forwarding in Subsonic, go to <b>Settings &gt; Network</b> and enable the
                "Automatically configure your router..." option. Click the Save button and pay close attention to the status message
                that is displayed. If you get an error message, you should first try to configure your router to enable UPnP
                (or NAT-PMP if you have an AirPort from Apple).
                If it still fails, or your router doesn't support UPnP, follow the instructions below.
                If it works, the Subsonic server will at regular intervals (every hour) contact the router and tell it to
                forward incoming connections (from the internet) to the Subsonic server.
            </p>

            <a name="2.2"><h3>2.2 Manual port forwarding</h3></a>
            <p>If the automatic option didn't work you can still set it up manually. Consult <a href="http://portforward.com/" target="_blank">portforward.com</a>
                to get instructions specific to your router. Note that the instructions contain important steps on how to
                configure a <em>static IP address</em> for your computer. You should follow these. Later in the process, you
                will find that Subsonic is not on the list of applications, so select the "Default Guide" instead.
            </p>
            <p>
                When you get to the point where the router asks for <em>private and public ports</em>, enter the port number the
                Subsonic server is using (normally 80 on Windows and 4040 on other operating systems) for both values.
                If asked for a <em>protocol</em>, select TCP.
            </p>

            <p>If you are unsure what any of this means you will probably need to consult someone with network configuration
                experience.</p>

            <a name="2.3"><h3>2.3 Setting up your personal subsonic.org address</h3></a>
            <p>Once you get the port forwarding set up correctly, the next step is to get yourself an easy-to-remember web address.
                You will use this address to access your Subsonic server from the internet.</p>

            <p>From <b>Settings &gt; Network</b> you can register your address. (Remember to log on with admin rights).
                Enable the "Access your server over the internet..." option,
                and enter your preferred address, for instance http://joe.subsonic.org.</p>

            <p>Click the Save button, and pay attention to the status message that is displayed. Note that Subsonic will also perform
                a test to ensure that the web address works properly. If the test fails it might be an indication that the port
                forwarding is not set up correctly.</p>

            <p>If you don't want to use a subsonic.org address you can get similar services from dynamic DNS providers such as dyndns.com.</p>


            <a name="2.4"><h3>2.4 Troubleshooting</h3></a>
            <p>This section describes some common network problems and how to fix them.</p>
            <ul class="list">
                <li><b>Multiple routers.</b> There might be more than one router on your network, for instance both an "inner"
                    wireless router and an "outer" (closest to the internet) cable/fiber modem with routing enabled. You must
                    make sure that incoming traffic received by the outermost router is forwarded to the inner router (which
                    in turn forwards the traffic on to your Subsonic server).  You can do this by configuring port forwarding on the
                    outer router (forward traffic to the inner router), or setting it in <em>bridge mode</em>. Your ISP should
                    be able to help you if you have problems with this. </li>
                <li><b>Port blocked by ISP.</b> Many ISPs (Internet Service Providers) block incoming traffic on port 80, which is
                    used by most web servers. Subsonic uses port 80 by default on Windows. You can avoid this by configuring
                    Subsonic to use a different port. Open the Subsonic Control Panel by clicking on the Subsonic tray icon.
                    Change the port number to any random number between 1025 and 65,000.  If you set up manual port forwarding
                    (see above), remember to update that as well.</li>
                <li><b>Firewall.</b> You might be running firewall software (on the Subsonic server) which blocks incoming traffic.
                    If this is the case, try first to turn the firewall off. If this helps, turn the firewall back on and configure it
                    to allow incoming traffic to Subsonic. Note: The Windows installer version of Subsonic will automatically
                    configure the built-in Windows Firewall.</li>
            </ul>

            <a name="3"><h2>3 Registering your license key</h2></a>
            <p>Most features in Subsonic are free, but some (like mobile phone streaming) require a license after the 30-day
                free trial. You get the license key by donating &euro;10 or more to the Subsonic project. After you receive the license key
                by email, you must register it on your server as follows:</p>

            <ul class="list">
                <li>Click the "Donate" link in the top right corner of the Subsonic web interface.</li>
                <li>Enter the email address and license key you received by email.</li>
            </ul>

            <a href="inc/img/gettingstarted/license.png"><img src="inc/img/gettingstarted/license_thumb.png" alt=""/></a>

            <a name="4"><h2>4 Using https</h2></a>
            <p>For improved security you can turn on https encryption.  On Windows, open the Subsonic Control Panel by clicking
                on the Subsonic tray icon, then enable https on the specified port (default 443).
            </p>
            <p> On Linux, add <code>--https-port=4443</code> (or any other port) to SUBSONIC_ARGS in
                <code>/etc/default/subsonic</code> (Ubuntu/Debian) or <code>/etc/sysconfig/subsonic</code> (Fedora/RPM).
            </p>
            <p>
                Note that Subsonic uses a self-signed https certificate by default. This provides encryption but not
                proper authentication.  To use your own certificate you must put it in a Java keystore, then specify the
                following Java system properties: <em>subsonic.ssl.keystore</em> &ndash; (Path to an alternate SSL keystore), and
                <em>subsonic.ssl.password</em> &ndash; (Password of the alternate SSL keystore). On Windows, you can set these system
                properties in <code>C:\Program Files\Subsonic\subsonic-service.exe.vmoptions</code>.
            </p>

        </div>

        <div id="side-col">
            <%@ include file="google-translate.jsp" %>
            <div class="sidebox">
                <h2>Table of contents</h2>
                <ul class="list">
                    <li><a href="#1">1 Setting up music folders</a></li>
                    <li><a href="#2">2 Setting up remote access</a></li>

                    <ul class="list">
                        <li><a href="#2.1">2.1 Automatic port forwarding</a></li>
                        <li><a href="#2.2">2.2 Manual port forwarding</a></li>
                        <li><a href="#2.3">2.3 Setting up your personal subsonic.org address</a></li>
                        <li><a href="#2.3">2.4 Troubleshooting</a></li>
                    </ul>
                    <li><a href="#3">3 Registering your license key</a></li>
                    <li><a href="#4">4 Using https</a></li>
                </ul>
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
