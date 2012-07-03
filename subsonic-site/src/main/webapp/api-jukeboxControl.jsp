<h2 class="div"><a name="jukeboxControl"></a>jukeboxControl</h2>

<p>
    <code>http://your-server/rest/jukeboxControl.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Controls the jukebox, i.e., playback directly on the server's audio hardware. Note: The user must
    be authorized to control the jukebox (see Settings &gt; Users &gt; User is allowed to play files in jukebox mode).
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>action</code></td>
        <td>Yes</td>
        <td></td>
        <td>The operation to perform. Must be one of: <code>get</code>, <code>status</code> (since <a href="#versions">1.7.0</a>), <code>set</code> (since <a href="#versions">1.7.0</a>),
            <code>start</code>, <code>stop</code>, <code>skip</code>, <code>add</code>, <code>clear</code>, <code>remove</code>, <code>shuffle</code>, <code>setGain</code>
        </td>
    </tr>
    <tr>
        <td><code>index</code></td>
        <td>No</td>
        <td></td>
        <td>Used by <code>skip</code> and <code>remove</code>. Zero-based index of the song to skip to or remove.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>offset</code></td>
        <td>No</td>
        <td></td>
        <td>(Since <a href="#versions">1.7.0</a>) Used by <code>skip</code>. Start playing this many seconds into the track.</td>
    </tr>
    <tr>
        <td><code>id</code></td>
        <td>No</td>
        <td></td>
        <td>Used by <code>add</code> and <code>set</code>. ID of song to add to the jukebox playlist. Use multiple <code>id</code> parameters
            to add many songs in the same request. (<code>set</code> is similar to a <code>clear</code> followed by a <code>add</code>, but
            will not change the currently playing track.)
        </td>
    </tr>
    <tr class="table-altrow">
        <td><code>gain</code></td>
        <td>No</td>
        <td></td>
        <td>Used by <code>setGain</code> to control the playback volume. A float value between 0.0 and 1.0.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;jukeboxStatus&gt;</code> element on success, unless the <code>get</code>
    action is used, in which case a nested <code>&lt;jukeboxPlaylist&gt;</code> element is returned.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/jukeboxStatus_example_1.xml?view=markup">Example 1</a>.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/jukeboxPlaylist_example_1.xml?view=markup">Example 2</a>.
</p>
