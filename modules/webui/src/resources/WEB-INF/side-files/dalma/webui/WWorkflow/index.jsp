<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <h1>Workflow Application : ${it.name}</h1>
  <c:if test="${!it.configured}">
    <p class="error">
      <img src="${rootURL}/images/caution.png" style="vertical-align:middle;" alt="(!)"/>
      (This application needs to be <a href="configure">configured</a>)
    </p>
  </c:if>
  <p>
    ${it.description}
  </p>
  <t:conversationView title="On-going Conversations" convs="${it.conversations}" />
  <t:conversationView title="Completed Conversations" convs="${it.completedConversations}" />
</l:main-panel>
<t:footer/>
