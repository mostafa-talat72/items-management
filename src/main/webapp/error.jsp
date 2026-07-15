<%--
  ERROR.JSP — Styled error page for displaying HTTP errors and application exceptions.

  This page is used in two scenarios:
    1. By the ErrorFilter — catches ALL unhandled exceptions and forwards here
    2. By web.xml error-page configuration — for 404, 500, and java.lang.Throwable

  Features:
    - Displays the HTTP status code prominently (e.g., 500, 404)
    - Shows a user-friendly error message (not a raw stack trace)
    - "Back to Home" button that redirects to the items dashboard
    - Red/pink gradient accent to visually indicate an error state
    - Animated card entrance

  The error message is resolved in this priority order:
    1. javax.servlet.error.status_code — the HTTP status code
    2. javax.servlet.error.message — the error description
    3. javax.servlet.error.exception — the exception object (fallback for its message)
    4. Hardcoded fallback: "An unexpected error occurred."

  This page is excluded from the ErrorFilter to prevent infinite redirect loops.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"%>
<%
    request.setAttribute("pageTitle", "Error");

    // Get the HTTP status code (default to 500 Internal Server Error)
    Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
    if(statusCode == null) statusCode = 500;

    // Get the error message, with fallbacks
    String errorMessage = (String) request.getAttribute("javax.servlet.error.message");
    if(errorMessage == null || errorMessage.trim().isEmpty()) {
        Throwable ex = (Throwable) request.getAttribute("javax.servlet.error.exception");
        if(ex != null) errorMessage = ex.getMessage();
    }
    if(errorMessage == null || errorMessage.trim().isEmpty()) {
        errorMessage = "An unexpected error occurred.";
    }
%>
<%@ include file="inc/head.jsp" %>
<body class="bg-gray-50 min-h-screen flex items-center justify-center p-5">

<div class="bg-white rounded-2xl p-8 sm:p-10 w-full max-w-md shadow-xl relative overflow-hidden animate-card-in text-center">
    <%-- Red/pink gradient accent bar (error themed) --%>
    <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-red-500 via-pink-500 to-purple-500"></div>

    <%-- Error icon (exclamation circle) --%>
    <div class="w-16 h-16 rounded-xl bg-red-50 flex items-center justify-center mx-auto mb-5 text-red-500">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
    </div>

    <%-- Status code (large, faded) --%>
    <h1 class="text-6xl font-bold text-gray-200 mb-2"><%= statusCode %></h1>
    <p class="text-sm text-gray-900 font-semibold mb-1">Something went wrong</p>
    <%-- Error message (user-friendly) --%>
    <p class="text-sm text-gray-400 mb-6"><%= errorMessage %></p>

    <%-- "Back to Home" button — redirects to the items dashboard --%>
    <a href="itemController?action=showItems" class="inline-flex items-center gap-2 text-sm font-semibold text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-5 py-2.5 rounded-xl no-underline shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
        Back to Home
    </a>
</div>

</body>
</html>
