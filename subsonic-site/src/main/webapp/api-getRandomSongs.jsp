<h2 class="div"><a name="getRandomSongs"></a>getRandomSongs</h2>

<p>
    <code>http://your-server/rest/getRandomSongs.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Returns random songs matching the given criteria.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>size</code></td>
        <td>No</td>
        <td>10</td>
        <td>The maximum number of songs to return. Max 500.</td>
    </tr>
    <tr>
        <td><code>genre</code></td>
        <td>No</td>
        <td></td>
        <td>Only returns songs belonging to this genre.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>fromYear</code></td>
        <td>No</td>
        <td></td>
        <td>Only return songs published after or in this year.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>toYear</code></td>
        <td>No</td>
        <td></td>
        <td>Only return songs published before or in this year.</td>
    </tr>
    <tr>
        <td><code>musicFolderId</code></td>
        <td>No</td>
        <td></td>
        <td>Only return songs in the music folder with the given ID. See getMusicFolders.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;randomSongs&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/randomSongs_example_1.xml?view=markup">Example</a>.
</p>
