<h2 class="div"><a name="getPlaylists"></a>getPlaylists</h2>

<p>
    <code>http://your-server/rest/getPlaylists.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns all playlists a user is allowed to play.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>username</code></td>
        <td>no</td>
        <td></td>
        <td>(Since <a href="#versions">1.8.0</a>) If specified, return playlists for this user rather than for the authenticated user. The authenticated user must
            have admin role if this parameter is used.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;playlists&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/playlists_example_1.xml?view=markup">Example</a>.
</p>
