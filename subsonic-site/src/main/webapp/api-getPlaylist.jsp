<h2 class="div"><a name="getPlaylist"></a>getPlaylist</h2>

<p>
    <code>http://your-server/rest/getPlaylist.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns a listing of files in a saved playlist.
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
        <td>yes</td>
        <td></td>
        <td>ID of the playlist to return, as obtained by <code>getPlaylists</code>.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;playlist&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/playlist_example_1.xml?view=markup">Example</a>.
</p>
