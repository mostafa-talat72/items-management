<%--
  ITEMDETAILS.JSP — Item details page showing the one-to-one relationship between
  an item and its detail record.

  Two main sections:
    1. Item summary card (price + stock in two stat boxes)
    2. Detail record card — either:
       a. Empty state with "Add Detail" button (if no detail exists)
       b. Read-only detail view with description, timestamps, Edit/Delete buttons

  Features:
    - Back button to return to the dashboard
    - Item name and ID header
    - Styled delete modal (not browser confirm())
    - Animated card entrance

  Flow:
    1. ItemController sets "item" and "itemDetail" request attributes
    2. If itemDetail is null → show empty state with Add button
    3. If itemDetail exists → show description, created/updated timestamps, Edit/Delete
    4. Delete modal uses JavaScript to set the delete link dynamically
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.items.model.Item" %>
<%@ page import="com.items.model.ItemDetails" %>
<%
    request.setAttribute("pageTitle", "Item Details");
    Item item = (Item) request.getAttribute("item");
    ItemDetails detail = (ItemDetails) request.getAttribute("itemDetail");
%>
<%@ include file="inc/head.jsp" %>
<%@ include file="inc/nav_open.jsp" %>
<%@ include file="inc/user_section.jsp" %>
    </div>
</nav>

<div class="pt-[104px] pb-10 px-6 sm:px-10 max-w-3xl mx-auto">

    <%-- Back button + item name header --%>
    <div class="flex items-center gap-4 mb-8 animate-card-in">
        <a href="itemController?action=showItems" class="flex items-center justify-center w-10 h-10 rounded-xl bg-white border-2 border-gray-200 text-gray-400 hover:text-indigo-600 hover:border-indigo-200 transition-all duration-200 no-underline">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></svg>
        </a>
        <div>
            <h1 class="text-2xl font-bold text-gray-900 tracking-tight"><%= item.getName() %></h1>
            <p class="text-sm text-gray-400 font-medium">Item #<%= item.getId() %></p>
        </div>
    </div>

    <%-- Item summary card (price + stock) --%>
    <div class="grid grid-cols-2 gap-4 mb-8 animate-card-in">
        <div class="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
            <span class="text-[10px] uppercase tracking-wider text-gray-400 font-semibold block mb-1">Price</span>
            <span class="text-2xl font-bold text-gray-900">$<%= String.format("%.2f", item.getPrice()) %></span>
        </div>
        <div class="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
            <span class="text-[10px] uppercase tracking-wider text-gray-400 font-semibold block mb-1">Stock</span>
            <span class="text-2xl font-bold text-gray-900"><%= item.getTotalNumber() %></span>
        </div>
    </div>

    <%-- Detail record card --%>
    <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden animate-card-in">
        <div class="px-6 py-5 border-b border-gray-100 flex items-center justify-between">
            <h2 class="text-lg font-bold text-gray-900">Item Detail</h2>
        </div>

        <% if(detail == null){ %>
            <%-- Empty state: no detail record exists yet --%>
            <div class="text-center py-16">
                <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-50 to-purple-50 flex items-center justify-center mx-auto mb-4">
                    <svg class="w-8 h-8 text-indigo-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                </div>
                <h3 class="text-base font-bold text-gray-700 mb-1">No detail record</h3>
                <p class="text-sm text-gray-400 mb-6">This item does not have a detail record yet.</p>
                <a href="<%= request.getContextPath() %>/itemController?action=showAddItemDetail&itemId=<%= item.getId() %>" class="inline-flex items-center gap-2 text-sm font-semibold text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-5 py-2.5 rounded-xl no-underline shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    Add Detail
                </a>
            </div>
        <% }else{ %>

        <%-- Existing detail: show description, timestamps, edit/delete buttons --%>
        <div class="p-6">
            <div>
                <label class="block text-sm font-semibold text-gray-600 mb-1.5">Description</label>
                <p class="text-gray-900 leading-relaxed whitespace-pre-wrap"><%= detail.getDescription() %></p>
            </div>
            <%-- Timestamps section --%>
            <% if(detail.getCreatedAt() != null){ %>
            <div class="mt-6 pt-4 border-t border-gray-100 flex gap-6 text-xs text-gray-400">
                <span>Created: <%= detail.getCreatedAt().toString().substring(0, 16) %></span>
                <% if(detail.getUpdatedAt() != null){ %>
                <span>Updated: <%= detail.getUpdatedAt().toString().substring(0, 16) %></span>
                <% } %>
            </div>
            <% } %>
            <%-- Edit and Delete action buttons --%>
            <div class="mt-6 pt-4 border-t border-gray-100 flex gap-3">
                <a href="<%= request.getContextPath() %>/itemController?action=showUpdateItemDetail&id=<%= detail.getId() %>&itemId=<%= item.getId() %>" class="flex-1 text-center no-underline text-sm font-semibold text-indigo-600 bg-indigo-50 py-2.5 rounded-xl hover:bg-indigo-100 active:scale-95 transition-all duration-200">Edit</a>
                <button onclick="openDeleteModal(<%= detail.getId() %>, <%= item.getId() %>)" class="flex-1 text-sm font-semibold text-red-500 bg-red-50 py-2.5 rounded-xl hover:bg-red-100 active:scale-95 transition-all duration-200 cursor-pointer border-none">Delete</button>
            </div>
        </div>
        <% } %>
    </div>
</div>

<% if(detail != null){ %>
<%-- Delete confirmation modal (shown only when a detail record exists) --%>
<div id="deleteModal" class="fixed inset-0 z-50 flex items-center justify-center p-5 bg-black/40 opacity-0 invisible transition-all duration-300">
    <div class="bg-white rounded-2xl p-8 w-full max-w-sm shadow-2xl animate-card-in text-center">
        <div class="w-14 h-14 rounded-xl bg-red-50 flex items-center justify-center mx-auto mb-4 text-red-500">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
        </div>
        <h3 class="text-lg font-bold text-gray-900 mb-2">Delete Detail?</h3>
        <p class="text-sm text-gray-400 mb-6">This action cannot be undone.</p>
        <div class="flex gap-3">
            <button onclick="closeDeleteModal()" class="flex-1 bg-gray-100 text-gray-500 py-2.5 rounded-xl font-semibold text-sm hover:bg-gray-200 transition-all duration-200 cursor-pointer border-none">Cancel</button>
            <a id="confirmDeleteDetail" href="#" class="flex-1 text-center no-underline text-white bg-gradient-to-r from-red-500 to-pink-600 py-2.5 rounded-xl font-semibold text-sm shadow-md shadow-red-500/25 hover:shadow-lg hover:shadow-red-500/35 transition-all duration-300">Delete</a>
        </div>
    </div>
</div>
<% } %>

<script>
// Opens the delete modal with the specific detail and item IDs
function openDeleteModal(detailId, itemId){
    document.getElementById('confirmDeleteDetail').href = '<%= request.getContextPath() %>/itemController?action=deleteItemDetail&id=' + detailId + '&itemId=' + itemId;
    document.getElementById('deleteModal').classList.remove('opacity-0', 'invisible');
}
// Closes the delete modal
function closeDeleteModal(){
    document.getElementById('deleteModal').classList.add('opacity-0', 'invisible');
}
// Close modal when clicking the backdrop
document.addEventListener('click', function(e){
    var modal = document.getElementById('deleteModal');
    if(modal && e.target === modal) closeDeleteModal();
});
</script>

</body>
</html>
