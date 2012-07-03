<h2 class="div"><a name="getMusicDirectory"></a>getMusicDirectory</h2>

<p>
    <code>http://your-server/rest/getMusicDirectory.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns a listing of all files in a music directory. Typically used to get list of albums for an artist,
    or list of songs for an album.
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
        <td>A string which uniquely identifies the music folder. Obtained by calls to getIndexes or getMusicDirectory.
        </td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;directory&gt;</code>
    element on success.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/directory_example_1.xml?view=markup">Example 1</a>.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/directory_example_2.xml?view=markup">Example 2</a>.
</p>
