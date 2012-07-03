<h2 class="div"><a name="getLyrics"></a>getLyrics</h2>

<p>
    <code>http://your-server/rest/getLyrics.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Searches for and returns lyrics for a given song.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>artist</code></td>
        <td>No</td>
        <td></td>
        <td>The artist name.</td>
    </tr>
    <tr>
        <td><code>title</code></td>
        <td>No</td>
        <td></td>
        <td>The song title.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;lyrics&gt;</code>
    element on success. The <code>&lt;lyrics&gt;</code> element is empty if no matching lyrics was found.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/lyrics_example_1.xml?view=markup">Example</a>.
</p>
