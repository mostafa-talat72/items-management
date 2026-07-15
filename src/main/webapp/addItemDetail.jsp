<%--
  ADDITEMDETAIL.JSP — Add item detail form page.

  Collects one field: description (textarea).
  Submits to itemController?action=addItemDetail.

  Features:
    - Displays the parent item name for context
    - Pre-fills submitted description on validation failure
    - Field-level inline validation errors (red borders + SVG icons)
    - Hidden field passes the itemId to the controller
    - Back link returns to the item details page

  Validation:
    - Description: required, must be at least 3 characters
    - These rules match the DB CHECK constraint CHK_ITEM_DETAILS_DESCRIPTION_LENGTH
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="com.items.model.ItemDetails" %>
<%
    request.setAttribute("pageTitle", "Add Item Detail");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    ItemDetails submitted = (ItemDetails) request.getAttribute("submitted");
    Long itemId = (Long) request.getAttribute("itemId");
    String itemName = (String) request.getAttribute("itemName");
%>
<%@ include file="inc/head.jsp" %>
<%@ include file="inc/nav_open.jsp" %>
        <a href="<%= request.getContextPath() %>/itemController?action=showItemDetails&itemId=<%= itemId %>" class="text-sm font-semibold text-gray-400 hover:text-gray-600 transition-colors no-underline">Back to Item</a>
     <%@ include file="inc/user_section.jsp" %>
    </div>
</nav>

<div class="flex items-center justify-center min-h-[calc(100vh-72px)] p-5">
    <div class="bg-white rounded-2xl p-8 sm:p-10 w-full max-w-lg shadow-xl relative overflow-hidden animate-card-in">
        <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-400"></div>
        <h1 class="text-2xl font-bold text-gray-900 tracking-tight text-center mb-2">Add Item Detail</h1>
        <p class="text-center text-sm text-gray-400 mb-8">for <span class="font-semibold text-gray-600"><%= itemName %></span></p>
        <form action="<%= request.getContextPath() %>/itemController?action=addItemDetail" method="post">
            <%-- Hidden field: sends the parent item ID to the controller --%>
            <input type="hidden" name="itemId" value="<%= itemId %>">
            <%-- Description textarea field --%>
            <div class="mb-8">
                <label class="block text-sm font-semibold text-gray-600 mb-2">Description</label>
                <textarea name="description" placeholder="Enter description" rows="4"
                    class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("description") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white resize-none"><%= submitted != null ? submitted.getDescription() : "" %></textarea>
                <% if(errors != null && errors.containsKey("description")){ %>
                <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                    <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <p class="text-red-500 text-xs font-medium"><%= errors.get("description") %></p>
                </div>
                <% } %>
            </div>
            <%-- Action buttons --%>
            <div class="flex gap-3">
                <button type="submit" class="flex-1 text-white bg-gradient-to-r from-indigo-500 to-purple-600 py-3 rounded-xl font-semibold text-sm shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300 cursor-pointer border-none">Save Detail</button>
                <a href="<%= request.getContextPath() %>/itemController?action=showItemDetails&itemId=<%= itemId %>" class="flex-1 text-center no-underline bg-gray-100 text-gray-500 py-3 rounded-xl font-semibold text-sm hover:bg-gray-200 hover:text-gray-700 transition-all duration-200">Cancel</a>
            </div>
        </form>
    </div>
</div>

</body>
</html>
