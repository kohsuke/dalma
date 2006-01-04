<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@attribute name="title" required="true" %>
<c:set var="rootURL" value="${pageContext.request.contextPath}" scope="request" />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
  <title>${title}</title>
  <link rel="stylesheet" href="${rootURL}/css/scotland.css" type="text/css">
  <link rel="stylesheet" href="${rootURL}/css/style.css" type="text/css">
  <meta name="ROBOTS" content="INDEX,NOFOLLOW">
  <c:if test="${param.auto_refresh}">
    <meta http-equiv="Refresh" content="10">
  </c:if>
</head>
<body>
<div id=nonFooter>
	<div id=logoBar>
	  <a href="http://localhost:8080/dalma/"><img src="${rootURL}/images/title.png" alt="Dalma" id="logo" /></a>
	</div>
	<div id=breadcrumbs>
		<div style="float:right">
      <c:choose>
        <c:when test="${param.auto_refresh}">
          <a href="?auto_refresh=false">DISABLE AUTO REFRESH</a>
        </c:when>
        <c:otherwise>
          <a href="?auto_refresh=true">ENABLE AUTO REFRESH</a>
        </c:otherwise>
      </c:choose>
		</div>

    <%-- breadcrumbs --%>
    <c:forEach var="anc" items="${pageContext.request.ancestors}">
      <c:if test="${anc.prev!=null}">
        &#187;
      </c:if>
      <a href="${anc.url}">
          ${anc.object.displayName}
      </a>
    </c:forEach>
	</div>
