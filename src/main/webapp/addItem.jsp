<%--
  ADDITEM.JSP — Add new item form page.

  Collects: name, price, totalNumber.
  Submits to itemController?action=addItem.

  Features:
    - Pre-fills submitted values on validation failure
    - Field-level inline validation errors (red borders + SVG icons)
    - Proper action URL using request.getContextPath() for deployment flexibility
    - Cancel button links back to dashboard
    - Animated card entrance and fade-in effects

  Validation (handled by ItemValidator on the server):
    - Name: required, non-empty
    - Price: required, must be a valid decimal number
    - Total Number: required, must be a valid integer
    - Additional business rules (negative/zero) are checked after DB constraint violations

  Note: Price and totalNumber are NOT pre-filled if their values are 0 or less
  (showing "0.00" is misleading when the user hasn't entered anything yet).
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="com.items.model.Item" %>
<%
    request.setAttribute("pageTitle", "Add Item");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    Item submitted = (Item) request.getAttribute("submitted");
%>
<%@ include file="inc/head.jsp" %>
<%@ include file="inc/nav_open.jsp" %>
        <a href="itemController?action=showItems" class="text-sm font-semibold text-gray-400 hover:text-gray-600 transition-colors no-underline">Back to Dashboard</a>
     <%@ include file="inc/user_section.jsp" %>
    </div>
</nav>

<div class="flex items-center justify-center min-h-[calc(100vh-72px)] p-5">
    <div class="bg-white rounded-2xl p-8 sm:p-10 w-full max-w-lg shadow-xl relative overflow-hidden animate-card-in">
        <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-400"></div>
        <h1 class="text-2xl font-bold text-gray-900 tracking-tight text-center mb-8">Add New Item</h1>
        <form action="<%= request.getContextPath() %>/itemController?action=addItem" method="post">
            <%-- Item name field --%>
            <div class="mb-5">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Item Name</label>
                <input type="text" name="name" placeholder="Enter item name"
                    value="<%= submitted != null ? submitted.getName() : "" %>"
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("name") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("name")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("name") %></p>
                </div>
                <% } %>
            </div>
            <%-- Price field --%>
            <div class="mb-5">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Price ($)</label>
                <input type="text" name="price" placeholder="0.00"
                    value="<%= submitted != null && submitted.getPrice() > 0 ? String.format("%.2f", submitted.getPrice()) : "" %>"
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("price") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("price")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("price") %></p>
                </div>
                <% } %>
            </div>
            <%-- Total number field --%>
            <div class="mb-8">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Total Number</label>
                <input type="text" name="totalNumber" placeholder="0"
                    value="<%= submitted != null && submitted.getTotalNumber() > 0 ? submitted.getTotalNumber() : "" %>"
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("totalNumber") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                <% if(errors != null && errors.containsKey("totalNumber")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("totalNumber") %></p>
                </div>
                <% } %>
            </div>
            <%-- Action buttons --%>
            <div class="flex gap-3">
                <button type="submit" class="flex-1 text-white bg-gradient-to-r from-indigo-500 to-purple-600 py-3 rounded-xl font-semibold text-sm shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300 cursor-pointer border-none">Save Item</button>
                <a href="itemController?action=showItems" class="flex-1 text-center no-underline bg-gray-100 text-gray-500 py-3 rounded-xl font-semibold text-sm hover:bg-gray-200 hover:text-gray-700 transition-all duration-200">Cancel</a>
            </div>
        </form>
    </div>
</div>

</body>
</html>
