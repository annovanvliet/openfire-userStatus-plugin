<%@ page contentType="text/html; charset=UTF-8" %>
<%--
  -
  - Copyright (C) 2004-2008 Jive Software. All rights reserved.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
--%>

<%@ page import="com.reucon.openfire.plugins.userstatus.UserStatusPlugin,
                 com.reucon.openfire.plugins.userstatus.ConnectionStatus,
                 com.reucon.openfire.plugins.userstatus.ConnectionStatus.Direction,
                 org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.util.StringUtils,
				         org.jivesoftware.util.JiveGlobals,
                 java.net.URLEncoder,
                 java.util.Date,
                 java.util.*"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%!
    final int DEFAULT_RANGE = 15;
    final int[] RANGE_PRESETS = {15, 25, 50, 75, 100};
%>

<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters
    int start = ParamUtils.getIntParameter(request,"start",0);
    int range = ParamUtils.getIntParameter(request,"range",webManager.getRowsPerPage("server-session-history", DEFAULT_RANGE));
    String hostname = ParamUtils.getParameter(request,"hostname");

    if (request.getParameter("range") != null) {
        webManager.setRowsPerPage("server-session-history", range);
    }

    UserStatusPlugin plugin = (UserStatusPlugin)webManager.getXMPPServer().getPluginManager(
            ).getPlugin("userstatus");

    // Get the session count
    int sessionCount = plugin.retrieveConnectionsCount();

    // paginator vars
    int numPages = (int)Math.ceil((double)sessionCount/(double)range);
    int curPage = (start/range) + 1;
    int maxIndex = (start+range <= sessionCount ? start+range : sessionCount);
%>

<html>
    <head>
        <title><fmt:message key="user-status.server.session.history.title"/></title>
        <meta name="pageID" content="server-session-history"/>
        <meta name="helpPage" content="view_active_server_sessions.html"/>
    </head>
    <body>


<p>
<fmt:message key="user-status.server.session.history.active" />: <b><%= sessionCount %></b>

<%  if (numPages > 1) { %>

    - <fmt:message key="global.showing" /> <%= (start+1) %>-<%= (start+range) %>

<%  } %>
 - <fmt:message key="user-status.server.session.history.sessions_per_page" />:
<select size="1" onchange="location.href='server-session-history.jsp?start=0&range=' + this.options[this.selectedIndex].value;">

    <% for (int aRANGE_PRESETS : RANGE_PRESETS) { %>

    <option value="<%= aRANGE_PRESETS %>"
            <%= (aRANGE_PRESETS == range ? "selected" : "") %>><%= aRANGE_PRESETS %>
    </option>

    <% } %>

</select>
</p>

<%  if (numPages > 1) { %>

    <p>
    <fmt:message key="global.pages" />:
    [
    <%  for (int i=0; i<numPages; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="server-session-history.jsp?start=<%= (i*range) %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>
    ]
    </p>

<%  } %>

<p>
<fmt:message key="user-status.server.session.history.info" />
</p>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th>&nbsp;</th>
        <th nowrap><fmt:message key="user-status.server.label.host" /></th>
        <th nowrap><fmt:message key="user-status.server.label.address" /></th>
        <th nowrap><fmt:message key="user-status.server.label.first_active" /></th>
        <th nowrap><fmt:message key="user-status.server.label.last_active" /></th>
        <th nowrap><fmt:message key="user-status.server.label.last_disconnect" /></th>
    </tr>
</thead>
<tbody>
    <%  // Check if no out/in connection to/from a remote server exists
        if (sessionCount == 0) {
    %>
        <tr>
            <td colspan="9">

                <fmt:message key="user-status.server.session.history.not_session" />

            </td>
        </tr>

    <%  } %>

    <% int count = 0;
        List<ConnectionStatus> connections = plugin.retrieveConnections(start, maxIndex);
          
        for (ConnectionStatus connectionStatus : connections) {
			count++;
    %>

    <tr class="jive-<%= (((count % 2) == 0) ? "even" : "odd") %>">
      <td width="1%" nowrap><%= count %></td>
      <td nowrap>
        <table cellpadding="0" cellspacing="0" border="0">
            <tr>
            <td width="1%" ><img src="getFavicon?host=<%=URLEncoder.encode(connectionStatus.getHost(), "UTF-8")%>" width="16" height="16" alt=""></td>
            <td><%= StringUtils.escapeHTMLTags(connectionStatus.getHost()) %></a></td>
            </tr>
        </table>
      </td>
      <td nowrap>
        <%= StringUtils.escapeHTMLTags(connectionStatus.getAddress()) %>
      </td>
      <td align="center" nowrap>
        <%= connectionStatus.getFirstActiveAsString() %>
      </td>
      <td align="center" nowrap>
        <%= connectionStatus.getLastActiveAsString() %>
      </td>
      <td align="center" nowrap>
        <%= connectionStatus.getLastDisconnectAsString() %>
      </td>
    
    </tr>

    
    
    <%  } %>

</tbody>
</table>
</div>

<%  if (numPages > 1) { %>

    <p>
    <fmt:message key="global.pages" />:
    [
    <%  for (int i=0; i<numPages; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="server-session-history.jsp?start=<%= (i*range) %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>
    ]
    </p>

<%  } %>

<br>
<p>
<fmt:message key="user-status.server.session.history.last_update" />: <%= JiveGlobals.formatDateTime(new Date()) %>
</p>

    </body>
</html>
