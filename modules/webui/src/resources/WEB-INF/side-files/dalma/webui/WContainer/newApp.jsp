<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="s" uri="http://scotland.dev.java.net/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  New Project page
--%>
<jsp:include page="sidepanel.jsp" />

<l:main-panel>
  <s:form method="post" action="createApp">
    <s:block>
      Select a .dar file to install
    </s:block>
    <s:entry title="name">
      <input type="text" name="name" class="setting-input" />
    </s:entry>
    <s:entry title="dar file">
      <input type="file" name="file" class="setting-input" />
    </s:entry>
    <s:block>
      <input type="submit" name="Submit" value="OK" />
    </s:block>
  </s:form>
</l:main-panel>
<t:footer/>
