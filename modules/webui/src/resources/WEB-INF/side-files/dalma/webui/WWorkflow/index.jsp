<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <h1>Workflow Application : ${it.name}</h1>
  <p>
    ${it.description}
  </p>
  <c:set var="convs" value="${it.conversations}" />
  <c:if test="${fn:length(convs)!=0}">
    <h2>On-going Conversations</h2>
    <div class="dashboard">
      <table id="projectstatus" class="pane">
        <tr>
          <th width=10%>&nbsp;</th>
          <th width=20%>Started On</th>
          <th width=60%>Title</th>
          <th width=10%>Status</th>
        </tr>
        <c:forEach var="c" items="${it.conversations}">
          <tr>
            <!--td>
              <img src="${rootURL}/images/box.png" />
            </td-->
            <td>
              <a href="conversation/${c.id}">
                #${c.id}
              </a>
            </td>
            <td>
              <fmt:formatDate value="${c.startDate}" type="both" timeStyle="short" dateStyle="short" />
            </td>
            <td>
            ${c.title}
            </td>
            <td align="center"><nowrap>
            ${c.state}
            </nowrap></td>
          </tr>
        </c:forEach>
      </table>
    </div>
  </c:if>
</l:main-panel>
<t:footer/>
