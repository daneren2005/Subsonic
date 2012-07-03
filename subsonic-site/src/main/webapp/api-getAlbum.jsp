<h2 class="div"><a name="getAlbum"></a>getAlbum</h2>

<p>
    <code>http://your-server/rest/getAlbum.view</code>
    <br>Since <a href="#versions">1.8.0</a>
</p>

<p>
    Returns details for an album, including a list of songs. This method organizes music according to ID3 tags.
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
        <td>The album ID.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;album&gt;</code>
    element on success.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/album_example_1.xml?view=markup">Example</a>.
</p>
