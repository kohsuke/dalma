<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  Side panel for the workflow view.
--%>
<t:header title="Dalma" />
<l:side-panel>
  <l:tasks>
    <c:choose>
      <c:when test="${it.running}">
        <l:task icon="images/clockStop.png" href="stop" title="Stop" />
      </c:when>
      <c:otherwise>
        <l:task icon="images/clockGo.png" href="start" title="Start" />
      </c:otherwise>
    </c:choose>
  </l:tasks>
</l:side-panel>
