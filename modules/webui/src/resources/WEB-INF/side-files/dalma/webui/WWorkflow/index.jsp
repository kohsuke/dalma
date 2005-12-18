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
</l:main-panel>
<t:footer/>
