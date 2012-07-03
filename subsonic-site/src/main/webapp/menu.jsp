<div id="logo"><a href="index.jsp"><img src="inc/img/subsonic.png" alt="Subsonic"/></a></div>

<div class="hide">
</div>

<div id="search">
    <table><tr>
        <form method="post" action="search.jsp" name="searchForm">
            <td><input type="text" name="query" id="query" size="18" value="Search" onclick="document.searchForm.query.select();"/></td>
            <td><a href="javascript:document.searchForm.submit()"><img src="inc/img/search.png" alt="Search" title="Search"/></a></td>
        </form>
    </tr></table>
</div>

<hr/>
<div id="nav">
    <ul>
        <li id="menu-home" ><a href="index.jsp" class="<%=current.equals("home") ? "open" : ""%>"><span>Home</span></a></li>
        <li><a href="download.jsp" class="<%=current.equals("download") ? "open" : ""%>"><span>Download</span></a></li>
        <li><a href="hosting.jsp" class="<%=current.equals("hosting") ? "open" : ""%>"><span>Hosting</span></a></li>
        <li><a href="documentation.jsp" class="<%=current.equals("documentation") ? "open" : ""%>"><span>Documentation</span></a></li>
        <li><a href="features.jsp" class="<%=current.equals("features") ? "open" : ""%>"><span>Features</span></a></li>
        <li><a href="apps.jsp" class="<%=current.equals("apps") ? "open" : ""%>"><span>Apps</span></a></li>
        <li><a href="screenshots.jsp" class="<%=current.equals("screenshots") ? "open" : ""%>"><span>Screenshots</span></a></li>
        <li><a href="demo.jsp" class="<%=current.equals("demo") ? "open" : ""%>"><span>Demo</span></a></li>
        <li><a href="changelog.jsp" class="<%=current.equals("changelog") ? "open" : ""%>"><span>Change Log</span></a></li>
        <li><a href="forum.jsp" class="<%=current.equals("forum") ? "open" : ""%>"><span>Forum</span></a></li>
        <li><a href="api.jsp" class="<%=current.equals("api") ? "open" : ""%>"><span>API</span></a></li>
    </ul>
</div>
<hr/>

