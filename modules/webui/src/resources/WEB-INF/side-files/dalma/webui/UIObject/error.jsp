<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <div class="error">
    ${message}
  </div>
</l:main-panel>
<t:footer/>
