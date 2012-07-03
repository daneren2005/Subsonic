<h2 class="div"><a name="getSong"></a>getSong</h2>

<p>
    <code>http://your-server/rest/getSong.view</code>
    <br>Since <a href="#versions">1.8.0</a>
</p>

<p>
    Returns details for a song.
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
        <td>The song ID.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;song&gt;</code>
    element on success.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/song_example_1.xml?view=markup">Example</a>.
</p>
