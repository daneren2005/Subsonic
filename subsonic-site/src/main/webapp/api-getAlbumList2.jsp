<h2 class="div"><a name="getAlbumList2"></a>getAlbumList2</h2>

<p>
    <code>http://your-server/rest/getAlbumList2.view</code>
    <br>Since <a href="#versions">1.8.0</a>
</p>

<p>
    Similar to <code>getAlbumList</code>, but organizes music according to ID3 tags.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>type</code></td>
        <td>Yes</td>
        <td></td>
        <td>The list type. Must be one of the following: <code>random</code>, <code>newest</code>,
            <code>frequent</code>, <code>recent</code>, <code>starred</code>,
            <code>alphabeticalByName</code> or <code>alphabeticalByArtist</code>.</td>
    </tr>
    <tr>
        <td><code>size</code></td>
        <td>No</td>
        <td>10</td>
        <td>The number of albums to return. Max 500.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>offset</code></td>
        <td>No</td>
        <td>0</td>
        <td>The list offset. Useful if you for example want to page through the list of newest albums.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;albumList2&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/albumList2_example_1.xml?view=markup">Example</a>.
</p>
