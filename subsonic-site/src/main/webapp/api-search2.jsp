<h2 class="div"><a name="search2"></a>search2</h2>

<p>
    <code>http://your-server/rest/search2.view</code>
    <br>Since <a href="#versions">1.4.0</a>
</p>

<p>
    Returns albums, artists and songs matching the given search criteria. Supports paging through the result.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>query</code></td>
        <td>Yes</td>
        <td></td>
        <td>Search query.</td>
    </tr>
    <tr>
        <td><code>artistCount</code></td>
        <td>No</td>
        <td>20</td>
        <td>Maximum number of artists to return.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>artistOffset</code></td>
        <td>No</td>
        <td>0</td>
        <td>Search result offset for artists. Used for paging.</td>
    </tr>
    <tr>
        <td><code>albumCount</code></td>
        <td>No</td>
        <td>20</td>
        <td>Maximum number of albums to return.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>albumOffset</code></td>
        <td>No</td>
        <td>0</td>
        <td>Search result offset for albums. Used for paging.</td>
    </tr>
    <tr>
        <td><code>songCount</code></td>
        <td>No</td>
        <td>20</td>
        <td>Maximum number of songs to return.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>songOffset</code></td>
        <td>No</td>
        <td>0</td>
        <td>Search result offset for songs. Used for paging.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;searchResult2&gt;</code>
    element on success.  <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/searchResult2_example_1.xml?view=markup">Example</a>.
</p>

