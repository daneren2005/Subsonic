<h2 class="div"><a name="setRating"></a>setRating</h2>

<p>
    <code>http://your-server/rest/setRating.view</code>
    <br>Since <a href="#versions">1.6.0</a>
</p>

<p>
    Sets the rating for a music file.
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
        <td>A string which uniquely identifies the file (song) or folder (album/artist) to rate.</td>
    </tr>
    <tr>
        <td><code>rating</code></td>
        <td>Yes</td>
        <td></td>
        <td>The rating between 1 and 5 (inclusive), or 0 to remove the rating.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>
