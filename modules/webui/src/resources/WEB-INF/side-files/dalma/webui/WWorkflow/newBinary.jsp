<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="s" uri="http://scotland.dev.java.net/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  Posts a new binary
--%>
<jsp:include page="sidepanel.jsp" />

<l:main-panel>
  <p>
    Upload a new .dar file to replace the current one.
    <b>A change to the .dar file may break on-going conversations, so be careful.</b>
  </p>
  <s:form method="post" action="submitNewBinary" enctype="multipart/form-data">
    <s:entry title="dar file">
      <input type="file" name="file" class="setting-input" />
    </s:entry>
    <s:block>
      <input type="submit" name="Submit" value="OK" />
    </s:block>
  </s:form>
</l:main-panel>
<t:footer/>
