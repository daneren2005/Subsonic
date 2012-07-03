<h2 class="div"><a name="deletePlaylist"></a>deletePlaylist</h2>

<p>
    <code>http://your-server/rest/deletePlaylist.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Deletes a saved playlist.
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
        <td>ID of the playlist to delete, as obtained by <code>getPlaylists</code>.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>
