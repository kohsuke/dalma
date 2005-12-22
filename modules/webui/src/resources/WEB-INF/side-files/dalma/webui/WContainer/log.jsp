<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="d" uri="http://dalma.dev.java.net/" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <table>
    <c:forEach var="log" items="${it.logs}">
      <tr class="log-preamble">
        <td><fmt:formatDate value="${d:createDate(log.millis)}" type="both" timeStyle="short" dateStyle="short" /></td>
        <td>${log.loggerName}</td>
      </tr>
      <tr class="log-text">
        <td colspan="2">
          <span class="log-level-${log.level}">
            ${log.message}
          </span>
          <hr>
        </td>
      </tr>
    </c:forEach>
  </table>
</l:main-panel>
<t:footer/>
