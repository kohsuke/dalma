<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" uri="http://scotland.dev.java.net/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="d" uri="http://dalma.dev.java.net/" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <script type="text/javascript">
    function doSection(detail,image){
      if(detail.style.display=="none") {
        detail.style.display="";
        image.src = "${rootURL}/images/treeMinus.gif";
      } else{
        detail.style.display="none";
        image.src = "${rootURL}/images/treePlus.gif";
      }
    }
  </script>
  <table>
    <c:forEach var="log" items="${it.logs}" varStatus="loop">
      <tr class="log-preamble">
        <td><fmt:formatDate value="${d:createDate(log.millis)}" type="both" timeStyle="short" dateStyle="short" /></td>
        <td>${log.loggerName}</td>
      </tr>
      <tr class="log-text">
        <td colspan="2">
          <div>
            <c:if test="${log.thrown!=null}">
              <div style="float:right">
                <img src="${rootURL}/images/treePlus.gif"
                  id="key${loop.index}"
                  onclick="doSection(exception${loop.index},key${loop.index})" />
              </div>
            </c:if>
            <span class="log-level-${log.level}">
            ${log.message}
            </span>
          </div>
          <c:if test="${log.thrown!=null}">
            <pre class="log-detail" style="display:none" id="exception${loop.index}"
              ><c:out value="${d:getExceptionDetail(log.thrown)}" escapeXml="true"
            /></pre>
          </c:if>
          <hr />
        </td>
      </tr>
    </c:forEach>
  </table>
</l:main-panel>
<t:footer/>
