<h2 class="div"><a name="changePassword"></a>changePassword</h2>

<p>
    <code>http://your-server/rest/changePassword.view</code>
    <br>Since <a href="#versions">1.1.0</a>
</p>

<p>
    Changes the password of an existing Subsonic user, using the following parameters.
    You can only change your own password unless you have admin privileges.
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
        <td>The name of the user which should change its password.</td>
    </tr>
    <tr>
        <td><code>password</code></td>
        <td>Yes</td>
        <td></td>
        <td>The new password of the new user, either in clear text of hex-encoded (see above).</td>
    </tr>
</table>

<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>
