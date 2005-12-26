<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="d" uri="http://dalma.dev.java.net/" %>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <div class="error">
    ${message}<br />
    <c:if test="${exception!=null}">
      <pre
        ><c:out value="${d:getExceptionDetail(exception)}" escapeXml="true"
      /></pre>
    </c:if>
  </div>
</l:main-panel>
<t:footer/>
