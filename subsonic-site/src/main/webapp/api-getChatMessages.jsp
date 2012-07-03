<h2 class="div"><a name="getChatMessages"></a>getChatMessages</h2>

<p>
    <code>http://your-server/rest/getChatMessages.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Returns the current visible (non-expired) chat messages.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>since</code></td>
        <td>No</td>
        <td></td>
        <td>Only return messages newer than this time (in millis since Jan 1 1970).</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;chatMessages&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/chatMessages_example_1.xml?view=markup">Example</a>.
</p>
