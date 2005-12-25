<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <h1>#${it.id} : ${it.title}</h1>
  <p>
    Started on <fmt:formatDate value="${it.startDate}" type="both" />.
  </p>
</l:main-panel>
<t:footer/>
