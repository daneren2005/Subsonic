<h2 class="div"><a name="deleteUser"></a>deleteUser</h2>

<p>
    <code>http://your-server/rest/deleteUser.view</code>
    <br>Since <a href="#versions">1.3.0</a>
</p>

<p>
    Deletes an existing Subsonic user, using the following parameters:
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
        <td>The name of the user to delete.</td>
    </tr>
</table>

<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>
