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
  <div class="dashboard">
    <table id="projectstatus" class="pane">
      <tr>
        <th>&nbsp;</th>
        <th>Workflow</th>
        <th># of Conversations</th>
        <th>Last Activated</th>
        <th>Status</th>
      </tr>

    <c:forEach var="w" items="${app.workflows}">
      <tr>
        <td>
          <img src="${rootURL}/images/box.png" />
        </td>
        <td>
          <a href="workflow/${w.name}">
            ${w.name}
          </a>
        </td>
        <td align="center">
          0
        </td>
        <td align="center">
          0
        </td>
        <td align="center">
          <c:choose>
            <c:when test="${w.running}">
              <img src="${rootURL}/images/greenFlag.png" />
              <span style="color: #008000">Running</span>
            </c:when>
            <c:otherwise>
              <img src="${rootURL}/images/redFlag.png" />
              <span style="color: #008000">Stopping</span>
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    </c:forEach>
  </table>
</l:main-panel>
<t:footer/>
