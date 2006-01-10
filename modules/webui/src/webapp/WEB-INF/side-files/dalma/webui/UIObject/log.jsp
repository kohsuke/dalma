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
  <l:tabBar>
    <l:tab name="Inclusive" active="true" href="#" />
    <l:tab name="Exclusive" active="fase" href="#" />
  </l:tabBar>
  <table width="100%" class="datatable" style="border-top:none;">
    <tr style="border-top: none;">
      <th>
        Log Records
      </th>
    </tr>
    <c:forEach var="log" items="${it.logs}" varStatus="loop">
      <tr><td class="log-row">
        <div class="log-preamble">
          <div><fmt:formatDate value="${d:createDate(log.millis)}" type="both" timeStyle="short" dateStyle="short" /></div>
          <div style="float:right">${log.loggerName}</div>
        </div>
        <div style="clear:both"></div>
        <div class="log-text">
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
        </div>
      </td></tr>
    </c:forEach>
  </table>
</l:main-panel>
<t:footer/>
