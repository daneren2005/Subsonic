<%
    String query = request.getParameter("query") + "+site:subsonic.org";
    response.sendRedirect("http://www.google.com/search?q=" + query);
%>
