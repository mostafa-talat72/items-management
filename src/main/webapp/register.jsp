<%--
  REGISTER.JSP — User registration (signup) page.

  Collects: fullName, username, email, password, confirmPassword.
  Submits to userController?action=signup.

  Features:
    - If a "rememberUserId" cookie exists, redirects to dashboard (user is already logged in)
    - Pre-fills submitted values on validation failure (the User object is in "submitted" attribute)
    - Field-level inline validation errors (red borders + SVG icons + fade-in animation)
    - Consistent styling with the rest of the app (gradient card, Poppins font)

  Flow:
    1. Check for remember-me cookie → redirect to dashboard
    2. If errors exist (from controller), they're displayed next to each field
    3. Submitted values are pre-filled so the user doesn't lose their input
    4. On submit → POST to userController?action=signup
    5. On success → redirect to verifyOtp.jsp for email verification

  Note: Password and confirmPassword are NEVER pre-filled (security best practice).
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="com.items.model.User" %>
<%
    request.setAttribute("pageTitle", "Register");

    // If already logged in, redirect to dashboard
    Cookie[] cookies = request.getCookies();
    if(cookies != null) {
        for(Cookie c : cookies) {
            if("rememberUserId".equals(c.getName())) {
                response.sendRedirect(request.getContextPath() + "/itemController?action=showItems");
                return;
            }
        }
    }

    // Error map and submitted values from the controller (null if first load)
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    User submitted = (User) request.getAttribute("submitted");
%>
<%@ include file="inc/head.jsp" %>
<%@ include file="inc/nav_open.jsp" %>
        <%-- "Sign In" button for existing users --%>
        <a href="login.jsp" class="text-sm font-semibold text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-4 py-2 rounded-xl no-underline shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300">Sign In</a>
    </div>
</nav>

<%-- Centered registration card --%>
<div class="flex items-center justify-center min-h-[calc(100vh-72px)] p-5">
    <div class="bg-white rounded-2xl p-8 sm:p-10 w-full max-w-lg shadow-xl relative overflow-hidden animate-card-in">
        <%-- Top gradient accent bar --%>
        <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-400"></div>
        <h1 class="text-2xl font-bold text-gray-900 tracking-tight text-center mb-8">Create Account</h1>

        <%-- Registration form --%>
        <form action="<%= request.getContextPath() %>/userController?action=signup" method="post">
            <%-- Full Name field --%>
            <div class="mb-5">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Full Name</label>
                <input type="text" name="fullName" placeholder="John Doe"
                    value="<%= submitted != null ? submitted.getFullName() : "" %>"
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("fullName") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("fullName")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("fullName") %></p>
                </div>
                <% } %>
            </div>
            <%-- Username field --%>
            <div class="mb-5">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Username</label>
                <input type="text" name="username" placeholder="johndoe"
                    value="<%= submitted != null ? submitted.getUsername() : "" %>"
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("username") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("username")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("username") %></p>
                </div>
                <% } %>
            </div>
            <%-- Email field --%>
            <div class="mb-5">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Email</label>
                <input type="email" name="email" placeholder="you@example.com"
                    value="<%= submitted != null ? submitted.getEmail() : "" %>"
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("email") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("email")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("email") %></p>
                </div>
                <% } %>
            </div>
            <%-- Password field (never pre-filled) --%>
            <div class="mb-5">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Password</label>
                <input type="password" name="password" placeholder="Min 6 characters"
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("password") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("password")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("password") %></p>
                </div>
                <% } %>
            </div>
            <%-- Confirm Password field (never pre-filled) --%>
            <div class="mb-8">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Confirm Password</label>
                <input type="password" name="confirmPassword" placeholder="Repeat password"
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("confirmPassword") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("confirmPassword")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("confirmPassword") %></p>
                </div>
                <% } %>
            </div>
            <%-- Submit button with gradient style --%>
            <button type="submit" class="w-full text-white bg-gradient-to-r from-indigo-500 to-purple-600 py-3 rounded-xl font-semibold text-sm shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300 cursor-pointer border-none">Create Account</button>
        </form>

        <%-- Link to login page for existing users --%>
        <p class="text-center text-sm text-gray-400 mt-6">
            Already have an account?
            <a href="login.jsp" class="text-indigo-500 font-semibold no-underline hover:text-indigo-600">Sign In</a>
        </p>
    </div>
</div>

</body>
</html>
