<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
  response.setHeader("X-Dalma-Version",
      getServletConfig().getServletContext().getAttribute("version").toString());
%>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
  <p>
  main text
</l:main-panel>
<t:footer/>
