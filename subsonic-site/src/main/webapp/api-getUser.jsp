<h2 class="div"><a name="getUser"></a>getUser</h2>

<p>
    <code>http://your-server/rest/getUser.view</code>
    <br>Since <a href="#versions">1.3.0</a>
</p>

<p>
    Get details about a given user, including which authorization roles it has.
    Can be used to enable/disable certain features in the client, such as jukebox control.
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
        <td>Yes</td>
        <td></td>
        <td>The name of the user to retrieve. You can only retrieve your own user unless you have admin privileges.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;user&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/user_example_1.xml?view=markup">Example</a>.
</p>
