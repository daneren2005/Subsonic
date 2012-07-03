<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%!
    String current = "hosting";
    String gigaProsUrl = "http://www.gigapros.com/affiliate/scripts/click.php?a_aid=subsonic&desturl=http://www.gigapros.com/portal/index.php/products-a-services/specialty-hosting/subsonic-server.html";
    String zazeenUrl = "https://www.zazeen.com/OnlinePC.html";
%>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
    <%@ include file="menu.jsp" %>

    <div id="content">
        <div id="main-col">
            <h1 class="bottomspace">Subsonic Hosting</h1>

            <p>
                An alternative to running the Subsonic on your own computer is to get a pre-installed Subsonic server from
                one of our hosting partners.
            </p>

            <div class="featureitem">
                <div class="heading">Zazeen</div>
                <div class="content">
                    <div class="wide-content">

                        <a href="<%=zazeenUrl%>" target="_blank"><img src="inc/img/zazeen.gif" alt="Zazeen" class="img-left"/></a>
                        <p>
                            Zazeen's <em>Online PC</em> comes with Ubuntu and a full range of applications and services, including
                            a ready-to-use Subsonic server.
                        </p>
                        <p>
                            Zazeen provides multiple 10Gbit fiber optic backbone and
                            peering arrangements to most ISPs in North America and Europe.
                        </p>

                        <p>
                            <b><a href="<%=zazeenUrl%>" target="_blank">Check out Zazeen's server plans and prices.</a></b>
                        </p>
                    </div>
                </div>
            </div>

            <div class="featureitem">
                <div class="heading">GigaPros</div>
                <div class="content">
                    <div class="wide-content">

                        <a href="<%=gigaProsUrl%>" target="_blank"><img src="inc/img/gigapros.png" alt="GigaPros" class="img-left"/></a>
                        <p>
                            GigaPros' Subsonic hosting servers are actually powerful Virtual Private Servers (VPS), which are highly optimized to run Subsonic.
                        </p>
                        <p>
                            These VPS'es have fully functional pre-installed Subsonic server and they behave exactly like your own Dedicated Server
                            with full root access.
                        </p>

                        <a href="http://www.trialpay.com/productpage/?c=ee8eacd&tid=RHyuuKT" target="_blank">
                            <img class="img-right" src="http://www.trialpay.com/mi/?rc=v&ri=1368898&p=VtA3333j&t=RHyuuKT&type=img" alt=""/>
                        </a>
                        <p>
                            <b><a href="<%=gigaProsUrl%>" target="_blank">Check out GigaPros' server plans and prices.</a></b>
                        </p>


                    </div>
                </div>
            </div> 

        </div>

        <div id="side-col">
            <%@ include file="google-translate.jsp" %>
        </div>

        <div class="clear">
        </div>
    </div>
    <hr/>
    <%@ include file="footer.jsp" %>
</div>


</body>
</html>
