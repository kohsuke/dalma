<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  Side panel for the build view.
--%>

<t:header title="Dalma" />
<l:side-panel>
  <l:tasks>
    <l:task icon="images/box.png" href="newApp" title="Install new App" />
    <l:task icon="images/wrench.gif" href="configure" title="Configure" />
  </l:tasks>
</l:side-panel>
