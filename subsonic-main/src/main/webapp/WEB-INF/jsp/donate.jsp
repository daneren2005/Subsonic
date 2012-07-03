<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="net.sourceforge.subsonic.command.DonateCommand"--%>
<html>
<head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe bgcolor1">

<h1>
    <img src="<spring:theme code="donateImage"/>" alt=""/>
    <fmt:message key="donate.title"/>
</h1>
<c:if test="${not empty command.path}">
    <sub:url value="main.view" var="backUrl">
        <sub:param name="path" value="${command.path}"/>
    </sub:url>
    <div class="back"><a href="${backUrl}">
        <fmt:message key="common.back"/>
    </a></div>
    <br/>
</c:if>

<c:url value="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=E5RNJMDJ7C862" var="donate10Url"/>
<c:url value="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=CKRS9A4J99TFN" var="donate15Url"/>
<c:url value="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=H79PAZLVHFT6E" var="donate20Url"/>
<c:url value="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=2TGXFN7AVREEN" var="donate25Url"/>
<c:url value="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=BXJVAQALLFREC" var="donate30Url"/>
<c:url value="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=M5PX55AC4ER9Y" var="donate50Url"/>

<div style="width:50em; max-width:50em">

<fmt:message key="donate.textbefore"><fmt:param value="${command.brand}"/></fmt:message>

<table cellpadding="10">
    <tr>
        <td>
            <table>
                <tr>
                    <td><a href="${donate10Url}" target="_blank"><img src="<spring:theme code="paypalImage"/>" alt=""/></a> </td>
                </tr>
                <tr>
                    <td class="detail" style="text-align:center;"><fmt:message key="donate.amount"><fmt:param value="&euro;10"/></fmt:message></td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <td><a href="${donate15Url}" target="_blank"><img src="<spring:theme code="paypalImage"/>" alt=""/></a> </td>
                </tr>
                <tr>
                    <td class="detail" style="text-align:center;"><fmt:message key="donate.amount"><fmt:param value="&euro;15"/></fmt:message></td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <td><a href="${donate20Url}" target="_blank"><img src="<spring:theme code="paypalImage"/>" alt=""/></a> </td>
                </tr>
                <tr>
                    <td class="detail" style="text-align:center;"><fmt:message key="donate.amount"><fmt:param value="&euro;20"/></fmt:message></td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <td><a href="${donate25Url}" target="_blank"><img src="<spring:theme code="paypalImage"/>" alt=""/></a> </td>
                </tr>
                <tr>
                    <td class="detail" style="text-align:center;"><fmt:message key="donate.amount"><fmt:param value="&euro;25"/></fmt:message></td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <td><a href="${donate30Url}" target="_blank"><img src="<spring:theme code="paypalImage"/>" alt=""/></a> </td>
                </tr>
                <tr>
                    <td class="detail" style="text-align:center;"><fmt:message key="donate.amount"><fmt:param value="&euro;30"/></fmt:message></td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <td><a href="${donate50Url}" target="_blank"><img src="<spring:theme code="paypalImage"/>" alt=""/></a> </td>
                </tr>
                <tr>
                    <td class="detail" style="text-align:center;"><fmt:message key="donate.amount"><fmt:param value="&euro;50"/></fmt:message></td>
                </tr>
            </table>
        </td>
    </tr>
</table>

<fmt:message key="donate.textafter"/>

<c:choose>
    <c:when test="${command.licenseValid}">
        <p>
            <b>
                <fmt:formatDate value="${command.licenseDate}" dateStyle="long" var="licenseDate"/>
                <fmt:message key="donate.licensed">
                    <fmt:param value="${command.emailAddress}"/>
                    <fmt:param value="${licenseDate}"/>
                    <fmt:param value="${command.brand}"/>
                </fmt:message>
        </p>
    </c:when>
    <c:otherwise>

        <p><fmt:message key="donate.register"/></p>

        <form:form commandName="command" method="post" action="donate.view">
            <form:hidden path="path"/>
            <table>
                <tr>
                    <td><fmt:message key="donate.register.email"/></td>
                    <td>
                        <form:input path="emailAddress" size="40"/>
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="donate.register.license"/></td>
                    <td>
                        <form:input path="license" size="40"/>
                    </td>
                    <td><input type="submit" value="<fmt:message key="common.ok"/>"/></td>
                </tr>
                <tr>
                    <td/>
                    <td class="warning"><form:errors path="license"/></td>
                </tr>
            </table>
        </form:form>

        <p><fmt:message key="donate.resend"/></p>

    </c:otherwise>
</c:choose>

</div>
</body>
</html>