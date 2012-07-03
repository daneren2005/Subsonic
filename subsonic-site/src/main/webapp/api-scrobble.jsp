<h2 class="div"><a name="scrobble"></a>scrobble</h2>

<p>
    <code>http://your-server/rest/scrobble.view</code>
    <br>Since <a href="#versions">1.5.0</a>
</p>

<p>
    "Scrobbles" a given music file on last.fm. Requires that the user has configured his/her last.fm
    credentials on the Subsonic server (Settings &gt; Personal).
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>A string which uniquely identifies the file to scrobble.</td>
    </tr>
    <tr>
        <td><code>submission</code></td>
        <td>No</td>
        <td>True</td>
        <td>Whether this is a "submission" or a "now playing" notification.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>
