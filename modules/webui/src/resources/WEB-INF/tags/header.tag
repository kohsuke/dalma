<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@attribute name="title" required="true" %>
<%--
    defines the header
--%>
<c:set var="rootURL" value="${pageContext.request.contextPath}" scope="request" />
<html>
<head>
  <title>${title}</title>
  <link rel="stylesheet" href="${rootURL}/css/style.css" type="text/css">
  <link rel="stylesheet" href="${rootURL}/css/color.css" type="text/css">
  <meta name="ROBOTS" content="INDEX,NOFOLLOW">
  <c:if test="${param.auto_refresh}">
    <meta http-equiv="Refresh" content="10">
  </c:if>
</head>
<body>
<table id="header" cellpadding="0" cellspacing="0" width="100%" border="0">
  <tr id="top-panel">
    <td colspan="2">
      <table cellpadding="0" cellspacing="0" width="100%" border="0">
        <tr><td style="font-weight:bold; font-size: 2em; width:200px">
          <a href="${rootURL}/"><img src="${rootURL}/images/title.png" alt="Dalma" /></a>
        </td><td style="background: url(${rootURL}/images/title-fill.png) repeat-x;">
          &nbsp;
          <%--form action="search">
            <div id="searchform">
              <img width="24" height="24" src="${rootURL}/images/24x24/find.gif"/>
              <b>Search:</b>
              <input name="search" size="12" value=""/>
              <input type="submit" value="Go"/> &nbsp;
            </div>
          </form--%>
        </td></tr>
        <tr bgcolor=white><td colspan=2 height=1></td></tr>
        <tr><td colspan=2 height=1></td></tr>
        <tr bgcolor=white><td colspan=2 height=1></td></tr>
        <tr><td colspan=2 height=1></td></tr>
        <tr bgcolor=white><td colspan=2 height=1></td></tr>
        <tr><td colspan=2 height=1></td></tr>
      </table>
    </td>
  </tr>

  <tr id="top-nav">
    <td id="left-top-nav">
      <c:forEach var="anc" items="${pageContext.request.ancestors}">
        <c:if test="${anc.prev!=null}">
          &#187;
        </c:if>
        <a href="${anc.url}">
          ${anc.object.displayName}
        </a>
      </c:forEach>
    </td>
    <td id="right-top-nav">
      <span class="smallfont">
        <c:choose>
          <c:when test="${param.auto_refresh}">
            <a href="?auto_refresh=false">DISABLE AUTO REFRESH</a>
          </c:when>
          <c:otherwise>
            <a href="?auto_refresh=true">ENABLE AUTO REFRESH</a>
          </c:otherwise>
        </c:choose>
      </span>
    </td>
  </tr>
</table>