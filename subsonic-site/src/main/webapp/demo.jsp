<%@ page import="java.util.Random" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "demo"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
    <%@ include file="menu.jsp" %>

    <div id="content">
        <div id="main-col">
            <h1>Online Demo</h1>
            <p>
                Try the online demo to get a taste of what Subsonic is all about!
            </p>

            <ul class="list">
                <li>
                    Not all Subsonic's features are available in the demo version. For instance, application settings can not be viewed
                    or changed. Please refer to the <a href="screenshots.jsp">screenshots</a> to see what you're missing.
                </li>
                <li>
                    All the music in the demo is free, and courtesy of <a href="http://www.jamendo.com/">Jamendo</a> and the respective artists.
                </li>
            </ul>

            <%
                Random random = new Random(System.currentTimeMillis());
                int userId = random.nextInt(5) + 1;
            %>
            <p style="text-align:center;font-size:1.3em"><b><a href="http://subsonic.org/demo/login.view?user=guest<%=userId%>&password=guest" target="_blank">&raquo; Start demo</a></b></p>
        </div>

        <div id="side-col">
            <%@ include file="google-translate.jsp" %>
            <%@ include file="download-subsonic.jsp" %>
            <%@ include file="donate.jsp" %>
        </div>

        <div class="clear">
        </div>
    </div>
    <hr/>
    <%@ include file="footer.jsp" %>
</div>


</body>
</html>
