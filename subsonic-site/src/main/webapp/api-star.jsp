<h2 class="div"><a name="star"></a>star</h2>

<p>
    <code>http://your-server/rest/star.view</code>
    <br>Since <a href="#versions">1.8.0</a>
</p>

<p>
    Attaches a star to a song, album or artist.
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
        <td>The ID of the file (song) or folder (album/artist) to star. Multiple parameters allowed.</td>
    </tr>
    <tr>
        <td><code>albumId</code></td>
        <td>No</td>
        <td></td>
        <td>The ID of an album to star. Use this rather than <code>id</code> if the client accesses the media collection according to ID3
            tags rather than file structure. Multiple parameters allowed.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>artistId</code></td>
        <td>No</td>
        <td></td>
        <td>The ID of an artist to star. Use this rather than <code>id</code> if the client accesses the media collection according to ID3
            tags rather than file structure. Multiple parameters allowed.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>
