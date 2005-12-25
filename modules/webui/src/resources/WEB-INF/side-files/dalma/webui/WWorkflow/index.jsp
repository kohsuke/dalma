<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <h1>Workflow Application : ${it.name}</h1>
  <p>
    ${it.description}
  </p>
  <h2>On-going Conversations</h2>
  <div class="dashboard">
    <table id="projectstatus" class="pane">
      <tr>
        <th>&nbsp;</th>
        <th>Title</th>
        <th>Status</th>
      </tr>
      <c:forEach var="c" items="${it.conversations}">
        <tr>
          <!--td>
            <img src="${rootURL}/images/box.png" />
          </td-->
          <td>
            <a href="conversations/${c.id}">
              #${c.id}
            </a>
          </td>
          <td>
            ${c.title}
          </td>
          <td align="center">
            ${c.state}
          </td>
        </tr>
      </c:forEach>
    </table>
  </div>
</l:main-panel>
<t:footer/>
