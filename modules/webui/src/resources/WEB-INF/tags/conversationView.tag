<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@attribute name="convs" required="true" type="java.util.Collection" %>
<%@attribute name="title" required="true" %>
<%--
  Generate a list view that shows a list of conversations
--%>
<c:if test="${fn:length(convs)!=0}">
  <h2>${title}</h2>
  <div class="dashboard">
    <table class="datatable">
      <tr>
        <th width=10%>&nbsp;</th>
        <th width=20%>Started On</th>
        <th width=60%>Title</th>
        <th width=10%>Status</th>
      </tr>
      <c:forEach var="c" items="${convs}">
        <tr>
          <%--td>
            <img src="${rootURL}/images/box.png" />
          </td--%>
          <td>
            <a href="conversation/${c.id}">
              #${c.id}
            </a>
          </td>
          <td>
            <fmt:formatDate value="${c.startDate}" type="both" timeStyle="short" dateStyle="short" />
          </td>
          <td>
            ${c.title}
          </td>
          <td align="center"><nowrap>
            ${c.state}
          </nowrap></td>
        </tr>
      </c:forEach>
    </table>
  </div>
</c:if>
