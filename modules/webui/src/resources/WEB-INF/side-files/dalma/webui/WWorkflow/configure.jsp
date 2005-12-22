<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="http://scotland.dev.java.net/form" %>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <s:form method="post" action="postConfigure">
    <s:entry title="Application Name">
      ${it.name}
    </s:entry>
    <s:section title="Application Configuration">
      <c:set var="config" value="${it.configProperties}" />
      <c:forEach var="prop" items="${it.model.parts}">
        <s:entry title="${prop.name}" description="${prop.description}">
          <input class="setting-input" name="config-${prop.name}"
            type="text"  value="${config[prop.name]}" />
        </s:entry>
      </c:forEach>
    </s:section>
    <s:block>
      <input type="submit" name="Submit" value="OK" />
    </s:block>
  </s:form>
</l:main-panel>
<t:footer/>
