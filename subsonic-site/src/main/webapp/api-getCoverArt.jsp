<h2 class="div"><a name="getCoverArt"></a>getCoverArt</h2>

<p>
    <code>http://your-server/rest/getCoverArt.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns a cover art image.
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
        <td>A string which uniquely identifies the cover art file to download. Obtained by calls to getMusicDirectory.
        </td>
    </tr>
    <tr>
        <td><code>size</code></td>
        <td>No</td>
        <td></td>
        <td>If specified, scale image to this size.</td>
    </tr>
</table>
<p>
    Returns the cover art image in binary form.
</p>
