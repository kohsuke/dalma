<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <div class="error">
    ${message}
  </div>
</l:main-panel>
<t:footer/>
