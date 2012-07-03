<%@ page import="java.net.URL" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%! String current = "screenshots"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
    <%@ include file="menu.jsp" %>

    <div id="content">
        <div id="main-col">
            <a href="inc/img/screenshots/screen02.png"><img src="inc/img/screenshots/thumb02.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen01.png"><img src="inc/img/screenshots/thumb01.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen05.png"><img src="inc/img/screenshots/thumb05.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen06.png"><img src="inc/img/screenshots/thumb06.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen03.png"><img src="inc/img/screenshots/thumb03.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen07.png"><img src="inc/img/screenshots/thumb07.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen08.png"><img src="inc/img/screenshots/thumb08.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen09.png"><img src="inc/img/screenshots/thumb09.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen10.png"><img src="inc/img/screenshots/thumb10.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen11.png"><img src="inc/img/screenshots/thumb11.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen16.png" title="SubAir app"><img src="inc/img/screenshots/thumb16.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen17.png" title="Z-Subsonic app for iPhone"><img src="inc/img/screenshots/thumb17.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen20.png" title="iSub app for iPhone"><img src="inc/img/screenshots/thumb20.png" alt="" style="padding:3px"/></a>
            <a href="inc/img/screenshots/screen12.png" title="Subsonic app for Android"><img src="inc/img/screenshots/thumb12.png" alt="" style="padding:3px;padding-right:20px"/></a>
            <a href="inc/img/screenshots/screen13.png" title="Subsonic app for Android"><img src="inc/img/screenshots/thumb13.png" alt="" style="padding:3px;padding-right:20px"/></a>
            <a href="inc/img/screenshots/screen14.png" title="Subsonic app for Android"><img src="inc/img/screenshots/thumb14.png" alt="" style="padding:3px;padding-right:20px"/></a>
            <a href="inc/img/screenshots/screen15.png" title="Subsonic app for Android"><img src="inc/img/screenshots/thumb15.png" alt="" style="padding:3px;padding-right:20px"/></a>
            <a href="inc/img/screenshots/screen23.png" title="Subsonic app for Windows Phone"><img src="inc/img/screenshots/thumb23.png" alt="" style="padding:3px;padding-right:10px"/></a>
            <a href="inc/img/screenshots/screen24.png" title="Subsonic app for Windows Phone"><img src="inc/img/screenshots/thumb24.png" alt="" style="padding:3px;padding-right:10px"/></a>
            <a href="inc/img/screenshots/screen25.png" title="Subsonic app for Windows Phone"><img src="inc/img/screenshots/thumb25.png" alt="" style="padding:3px"/></a>

            <div class="bottomspace"></div>

            <a name="video"><div></div></a>

            <object width="640" height="385"><param name="movie" value="http://www.youtube.com/v/EBDrdWxd95k?fs=1&amp;hl=en_GB"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/EBDrdWxd95k?fs=1&amp;hl=en_GB" type="application/x-shockwave-flash" allowscriptaccess="always" allowfullscreen="true" width="640" height="385"></embed></object>        
            <div class="bottomspace"></div>
            <object width="640" height="385"><param name="movie" value="http://www.youtube.com/v/xhgK9ShSpWg?fs=1&amp;hl=en_GB"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/xhgK9ShSpWg?fs=1&amp;hl=en_GB" type="application/x-shockwave-flash" allowscriptaccess="always" allowfullscreen="true" width="640" height="385"></embed></object>
            <div class="bottomspace"></div>
            <object width="640" height="385"><param name="movie" value="http://www.youtube.com/v/PvsQk3IoOt4?fs=1&amp;hl=en_GB"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/PvsQk3IoOt4?fs=1&amp;hl=en_GB" type="application/x-shockwave-flash" allowscriptaccess="always" allowfullscreen="true" width="640" height="385"></embed></object>
            <div class="bottomspace"></div>
            <object width="640" height="385"><param name="movie" value="http://www.youtube.com/v/UNqLjV10sTA?fs=1&amp;hl=en_GB"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/UNqLjV10sTA?fs=1&amp;hl=en_GB" type="application/x-shockwave-flash" allowscriptaccess="always" allowfullscreen="true" width="640" height="385"></embed></object>
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
