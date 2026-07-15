<%--
  LOGIN.JSP — User login page.

  Accepts either a username OR an email address ("loginId") plus a password.
  Submits to userController?action=login.

  Features:
    - If a "rememberUserId" cookie exists, redirects straight to the dashboard
    - Shows error messages from the controller (invalid credentials, unverified email)
    - Shows success messages from OTP verification (passed as query param)
    - Field-level inline validation errors (red borders + SVG icons)
    - Animated card entrance and fade-in effects

  Flow:
    1. Check for remember-me cookie → redirect to dashboard
    2. Check for error message in request attributes → display red banner
    3. Check for success message (from OTP verification) → display green banner
    4. Render login form
    5. On submit → POST to userController?action=login

  Note: The successMessage is checked BOTH from request attribute AND from
  request parameter. This is because the OTP verification redirects with a
  query parameter, but the controller may also set it as an attribute.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>
<%
    request.setAttribute("pageTitle", "Sign In");

    // If the user already has a remember-me cookie, skip the login page
    Cookie[] cookies = request.getCookies();
    if(cookies != null) {
        for(Cookie c : cookies) {
            if("rememberUserId".equals(c.getName())) {
                response.sendRedirect(request.getContextPath() + "/itemController?action=showItems");
                return;
            }
        }
    }

    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    String errorMessage = (String) request.getAttribute("message");
    // successMessage can come from the OTP verification redirect (query param) or from an attribute
    String successMessage = (String) request.getAttribute("successMessage") == null? (String) request.getParameter("successMessage"): (String) request.getAttribute("successMessage");
    
%>
<%@ include file="inc/head.jsp" %>
<%@ include file="inc/nav_open.jsp" %>
        <%-- "Create Account" button for non-logged-in users --%>
        <a href="register.jsp" class="text-sm font-semibold text-white bg-gradient-to-r from-purple-500 to-pink-600 px-4 py-2 rounded-xl no-underline shadow-md shadow-purple-500/25 hover:shadow-lg hover:shadow-purple-500/35 hover:-translate-y-0.5 transition-all duration-300">Create Account</a>
    </div>
</nav>

<%-- Centered login card --%>
<div class="flex items-center justify-center min-h-[calc(100vh-72px)] p-5">
    <div class="bg-white rounded-2xl p-8 sm:p-10 w-full max-w-lg shadow-xl relative overflow-hidden animate-card-in">
        <%-- Top gradient accent bar --%>
        <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-400"></div>
        <h1 class="text-2xl font-bold text-gray-900 tracking-tight text-center mb-8">Welcome Back</h1>

        <%-- Error message banner (red) — shows when login fails --%>
        <% if(errorMessage != null){ %>
        <div class="flex items-center gap-2 bg-red-50 border-2 border-red-200 rounded-xl px-4 py-3 mb-6 animate-fade-in">
            <svg class="w-4 h-4 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <p class="text-red-500 text-sm font-medium"><%= errorMessage %></p>
        </div>
        <% } %>
        
        <%-- Success message banner (green) — shows after successful OTP verification --%>
        <% if(successMessage != null){ %>
        <div class="flex items-center gap-2 bg-green-50 border-2 border-green-200 rounded-xl px-4 py-3 mb-6 animate-fade-in">
			<svg class="w-4 h-4 text-green-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
			<p class="text-green-500 text-sm font-medium"><%= successMessage %></p>
			</div>
		<% } %>

        <%-- Login form — accepts email OR username and password --%>
        <form action="<%= request.getContextPath() %>/userController?action=login" method="post">
            <%-- Login ID field (accepts email or username) --%>
            <div class="mb-5">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Email or Username</label>
                <input type="text" name="loginId" placeholder="you@example.com or username" required
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("loginId") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("loginId")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("loginId") %></p>
                </div>
                <% } %>
            </div>
            <%-- Password field --%>
            <div class="mb-8">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Password</label>
                <input type="password" name="password" placeholder="Enter your password" required
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("password") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("password")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("password") %></p>
                </div>
                <% } %>
            </div>
            <%-- Submit button with gradient style --%>
            <button type="submit" class="w-full text-white bg-gradient-to-r from-indigo-500 to-purple-600 py-3 rounded-xl font-semibold text-sm shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300 cursor-pointer border-none">Sign In</button>
        </form>

        <%-- Link to registration page --%>
        <p class="text-center text-sm text-gray-400 mt-6">
            Don't have an account?
            <a href="register.jsp" class="text-indigo-500 font-semibold no-underline hover:text-indigo-600">Create One</a>
        </p>
    </div>
</div>

</body>
</html>
