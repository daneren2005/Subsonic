<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="net.sourceforge.subsonic.command.NetworkSettingsCommand"--%>

<html>
<head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/prototype.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/multiService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/util.js"/>"></script>
    <script type="text/javascript" language="javascript">

        function init() {
            enableUrlRedirectionFields();
            refreshStatus();
        }

        function refreshStatus() {
            multiService.getNetworkStatus(updateStatus);
        }

        function updateStatus(networkStatus) {
            dwr.util.setValue("portForwardingStatus", networkStatus.portForwardingStatusText);
            dwr.util.setValue("urlRedirectionStatus", networkStatus.urlRedirectionStatusText);
            window.setTimeout("refreshStatus()", 1000);
        }

        function enableUrlRedirectionFields() {
            var checkbox = $("urlRedirectionEnabled");
            var field = $("urlRedirectFrom");

            if (checkbox && checkbox.checked) {
                field.enable();
            } else {
                field.disable();
            }
        }

    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="network"/>
</c:import>

<p style="padding-top:1em"><fmt:message key="networksettings.text"/></p>

<form:form commandName="command" action="networkSettings.view" method="post">
    <p style="padding-top:1em">
        <form:checkbox id="portForwardingEnabled" path="portForwardingEnabled"/>
        <label for="portForwardingEnabled"><fmt:message key="networksettings.portforwardingenabled"/></label>
    </p>

    <div style="padding-left:2em;max-width:60em">
        <p>
            <fmt:message key="networksettings.portforwardinghelp"><fmt:param>${command.port}</fmt:param></fmt:message>
        </p>

        <p class="detail">
            <fmt:message key="networksettings.status"/>
            <span id="portForwardingStatus" style="margin-left:0.25em"></span>
        </p>
    </div>

    <p style="padding-top:1em"><form:checkbox id="urlRedirectionEnabled" path="urlRedirectionEnabled"
                                              onclick="enableUrlRedirectionFields()"/>
        <label for="urlRedirectionEnabled"><fmt:message key="networksettings.urlredirectionenabled"/></label>
    </p>

    <div style="padding-left:2em">

        <p>http://<form:input id="urlRedirectFrom" path="urlRedirectFrom" size="16" cssStyle="margin-left:0.25em"/>.subsonic.org</p>

        <p class="detail">
            <fmt:message key="networksettings.status"/>
            <span id="urlRedirectionStatus" style="margin-left:0.25em"></span>
            <span id="urlRedirectionTestStatus" style="margin-left:0.25em"></span>
        </p>
    </div>

    <c:if test="${command.trial}">
        <fmt:formatDate value="${command.trialExpires}" dateStyle="long" var="expiryDate"/>

        <p class="warning" style="padding-top:1em">
            <c:choose>
                <c:when test="${command.trialExpired}">
                    <fmt:message key="networksettings.trialexpired"><fmt:param>${expiryDate}</fmt:param></fmt:message>
                </c:when>
                <c:otherwise>
                    <fmt:message
                            key="networksettings.trialnotexpired"><fmt:param>${expiryDate}</fmt:param></fmt:message>
                </c:otherwise>
            </c:choose>
        </p>
    </c:if>

    <p style="padding-top:1em">
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
        <input type="button" value="<fmt:message key="common.cancel"/>" onclick="location.href='nowPlaying.view'">
    </p>

</form:form>
</body>
</html>