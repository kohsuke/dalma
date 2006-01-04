<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  Side panel for the build view.
--%>

<t:header title="Dalma" />
<l:side-panel>
  <l:tasks>
    <l:task icon="images/caution.png" href="log" title="View Log" />
    <l:task icon="images/delete.png" href="delete" title="Delete" />
  </l:tasks>
</l:side-panel>
