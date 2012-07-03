<h2 class="div"><a name="getLicense"></a>getLicense</h2>

<p>
    <code>http://your-server/rest/getLicense.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Get details about the software license. Takes no extra parameters. Please note that access to the
    REST API requires that the server has a valid license (after a 30-day trial period). To get a license key you can
    give a donation to the Subsonic project.
</p>

<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;license&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/license_example_1.xml?view=markup">Example</a>.
</p>
