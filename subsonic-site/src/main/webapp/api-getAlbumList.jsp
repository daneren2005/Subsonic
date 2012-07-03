<h2 class="div"><a name="getAlbumList"></a>getAlbumList</h2>

<p>
    <code>http://your-server/rest/getAlbumList.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Returns a list of random, newest, highest rated etc. albums. Similar to the album lists
    on the home page of the Subsonic web interface.
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
            <code>highest</code>, <code>frequent</code>, <code>recent</code>. Since <a href="#versions">1.8.0</a>
            you can also use <code>alphabeticalByName</code> or <code>alphabeticalByArtist</code> to page through all albums
            alphabetically, and <code>starred</code> to retrieve starred albums.</td>
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
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;albumList&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/albumList_example_1.xml?view=markup">Example</a>.
</p>
