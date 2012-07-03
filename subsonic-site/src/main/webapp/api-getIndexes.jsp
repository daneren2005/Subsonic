<h2 class="div"><a name="getIndexes"></a>getIndexes</h2>

<p>
    <code>http://your-server/rest/getIndexes.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns an indexed structure of all artists.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>musicFolderId</code></td>
        <td>No</td>
        <td></td>
        <td>If specified, only return artists in the music folder with the given ID. See <code>getMusicFolders</code>.
        </td>
    </tr>
    <tr>
        <td><code>ifModifiedSince</code></td>
        <td>No</td>
        <td></td>
        <td>If specified, only return a result if the artist collection has changed since the given time.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;indexes&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/indexes_example_1.xml?view=markup">Example</a>.
</p>
