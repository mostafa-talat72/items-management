<%--
  USER_SECTION.JSP — Shared user authentication section for the navbar.

  Two display modes:
    1. LOGGED IN: Shows a user avatar with the first letter of the username,
       a dropdown menu with:
         - Profile link (redirects through UserController.selectUser for session refresh)
         - Sign Out button
    2. NOT LOGGED IN: Shows "Sign In" and "Register" links

  How it works:
    - Checks session.getAttribute("IsLoggedIn") to determine authentication state
    - The User object is retrieved from session.getAttribute("user")
    - The profile link encodes the return URL to redirect back after login refresh

  Dependencies:
    - Must be included AFTER nav_open.jsp (inside the nav's right-side div)
    - Requires session to have "IsLoggedIn" and "user" attributes
    - Imports User model for type safety and URLEncoder for redirect path encoding
--%>
<%@ page import="com.items.model.User" %>
<%@ page import="java.net.URLEncoder" %>
<%
    Boolean loggedIn = (Boolean) session.getAttribute("IsLoggedIn");
    User user = (User) session.getAttribute("user");
%>
<% if(Boolean.TRUE.equals(loggedIn)) { %>
    <%-- Logged-in state: avatar + dropdown --%>
    <div class="relative group">
        <button class="flex items-center gap-2 text-sm font-semibold text-gray-600 hover:text-gray-900 transition-colors cursor-pointer border-none bg-transparent py-2">
            <%-- Circular avatar with the first letter of the username --%>
            <div class="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xs font-bold shadow-sm"><%= user != null ? user.getUsername().charAt(0) : "U" %></div>
            <span><%= user != null ? user.getUsername() : "User" %></span>
        </button>
        <%-- Dropdown menu (appears on hover via Tailwind group-hover) --%>
        <div class="absolute right-0 top-full pt-2 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 translate-y-1 group-hover:translate-y-0">
            <div class="bg-white rounded-xl shadow-xl border border-gray-100 py-2 min-w-[180px]">
                <%-- User info section at the top of the dropdown --%>
                <div class="px-4 pb-2 mb-1 border-b border-gray-100">
                    <p class="text-sm font-semibold text-gray-800"><%= user != null ? user.getUsername() : "User" %></p>
                    <p class="text-xs text-gray-400 truncate"><%= user != null ? user.getEmail() :  "" %></p>
                </div>
                <%-- Profile link — goes through selectUser to refresh the session with latest data --%>
                <a href="<%= request.getContextPath() %>/userController?action=selectUser&id=<%= user.getId() %>&redirectPath=<%= URLEncoder.encode(request.getContextPath() + "/profile.jsp", "UTF-8") %>" class="block px-4 py-2 text-sm text-gray-600 hover:text-indigo-600 hover:bg-indigo-50 no-underline transition-colors">Profile</a>
                <%-- Logout link --%>
                <a href="<%= request.getContextPath() %>/userController?action=logout" class="block px-4 py-2 text-sm text-gray-600 hover:text-red-500 hover:bg-red-50 no-underline transition-colors">Sign Out</a>
            </div>
        </div>
    </div>
<% } else { %>
    <%-- Not logged in: simple Sign In / Register links --%>
    <a href="<%= request.getContextPath() %>/login.jsp" class="text-sm font-semibold text-gray-600 hover:text-gray-900 no-underline transition-colors">Sign In</a>
    <a href="<%= request.getContextPath() %>/register.jsp" class="text-sm font-semibold text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-4 py-2 rounded-xl no-underline shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300">Register</a>
<% } %>
