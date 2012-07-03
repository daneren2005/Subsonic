<h2 class="div"><a name="download"></a>download</h2>

<p>
    <code>http://your-server/rest/download.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Downloads a given media file. Similar to <code>stream</code>, but this method returns the original media data
    without transcoding or downsampling.
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
        <td>A string which uniquely identifies the file to download. Obtained by calls to getMusicDirectory.</td>
    </tr>
</table>
<p>
    Returns binary data on success, or an XML document on error (in which case the HTTP content type will start with "text/xml").
</p>
