<h2 class="div"><a name="search"></a>search</h2>

<p>
    <code>http://your-server/rest/search.view</code>
    <br>Since <a href="#versions">1.0.0</a>
    <br>Deprecated since <a href="#versions">1.4.0</a>, use <code>search2</code> instead.
</p>

<p>
    Returns a listing of files matching the given search criteria. Supports paging through the result.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>artist</code></td>
        <td>No</td>
        <td></td>
        <td>Artist to search for.</td>
    </tr>
    <tr>
        <td><code>album</code></td>
        <td>No</td>
        <td></td>
        <td>Album to searh for.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>title</code></td>
        <td>No</td>
        <td></td>
        <td>Song title to search for.</td>
    </tr>
    <tr>
        <td><code>any</code></td>
        <td>No</td>
        <td></td>
        <td>Searches all fields.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>count</code></td>
        <td>No</td>
        <td>20</td>
        <td>Maximum number of results to return.</td>
    </tr>
    <tr>
        <td><code>offset</code></td>
        <td>No</td>
        <td>0</td>
        <td>Search result offset. Used for paging.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>newerThan</code></td>
        <td>No</td>
        <td></td>
        <td>Only return matches that are newer than this. Given as milliseconds since 1970.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;searchResult&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/searchResult_example_1.xml?view=markup">Example</a>.
</p>
