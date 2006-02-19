<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <form method="get" action="doDelete">
    Are you sure about removing this conversation?
    <input type="submit" value="Yes" />
  </form>
</l:main-panel>
<t:footer/>
