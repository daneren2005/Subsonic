<h2 class="div"><a name="updatePlaylist"></a>updatePlaylist</h2>

<p>
    <code>http://your-server/rest/updatePlaylist.view</code>
    <br>Since <a href="#versions">1.8.0</a>
</p>

<p>
    Updates a playlist. Only the owner of a playlist is allowed to update it.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>playlistId</code></td>
        <td>Yes</td>
        <td></td>
        <td>The playlist ID.</td>
    </tr>
    <tr>
        <td><code>name</code></td>
        <td>No</td>
        <td></td>
        <td>The human-readable name of the playlist.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>comment</code></td>
        <td>No</td>
        <td></td>
        <td>The playlist comment.</td>
    </tr>
    <%--TODO: Add later--%>
    <%--<tr>--%>
        <%--<td><code>usernameToAdd</code></td>--%>
        <%--<td>No</td>--%>
        <%--<td></td>--%>
        <%--<td>Allow this user to listen to this playlist. Multiple parameters allowed.</td>--%>
    <%--</tr>--%>
    <%--<tr class="table-altrow">--%>
        <%--<td><code>usernameToRemove</code></td>--%>
        <%--<td>No</td>--%>
        <%--<td></td>--%>
        <%--<td>Disallow this user to listen to this playlist. Multiple parameters allowed.</td>--%>
    <%--</tr>--%>
    <tr>
        <td><code>songIdToAdd</code></td>
        <td>No</td>
        <td></td>
        <td>Add this song with this ID to the playlist. Multiple parameters allowed.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>songIndexToRemove</code></td>
        <td>No</td>
        <td></td>
        <td>Remove the song at this position in the playlist. Multiple parameters allowed.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>
