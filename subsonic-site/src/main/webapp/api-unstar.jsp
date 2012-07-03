<h2 class="div"><a name="unstar"></a>unstar</h2>

<p>
    <code>http://your-server/rest/unstar.view</code>
    <br>Since <a href="#versions">1.8.0</a>
</p>

<p>
    Removes the star from a song, album or artist.
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
        <td>No</td>
        <td></td>
        <td>The ID of the file (song) or folder (album/artist) to unstar. Multiple parameters allowed.</td>
    </tr>
    <tr>
        <td><code>albumId</code></td>
        <td>No</td>
        <td></td>
        <td>The ID of an album to unstar. Use this rather than <code>id</code> if the client accesses the media collection according to ID3
            tags rather than file structure. Multiple parameters allowed.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>artistId</code></td>
        <td>No</td>
        <td></td>
        <td>The ID of an artist to unstar. Use this rather than <code>id</code> if the client accesses the media collection according to ID3
            tags rather than file structure. Multiple parameters allowed.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>
