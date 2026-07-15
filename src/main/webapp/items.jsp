<%--
  ITEMS.JSP — Main dashboard page showing all inventory items as a responsive card grid.

  Features:
    - If no items exist → shows an empty state with an "Add First Item" prompt
    - If items exist → shows a 1/2/3-column responsive card grid
    - Each card shows: ID badge, item name (truncated), price, stock quantity
    - Card body is clickable → links to itemDetails.jsp (one-to-one detail view)
    - Each card has Edit and Delete buttons
    - Delete uses a styled modal confirmation (not browser confirm())
    - Staggered card entrance animation (each card delays slightly more than the last)
    - Page header with gradient accent and "Add Item" button

  Process:
    1. The ItemController sets the "items" request attribute (List of Item objects)
    2. This page checks if the list is empty or null
    3. If not empty, iterates and renders each item as a card
    4. Delete modal: JavaScript sets the href dynamically before showing the modal
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="com.items.model.Item" %>
<%
    request.setAttribute("pageTitle", "Dashboard");
	request.setAttribute("pagePath", "itemController?action=showItems");
    List<Item> items = (List<Item>) request.getAttribute("items");
%>
<%@ include file="inc/head.jsp" %>
<%@ include file="inc/nav_open.jsp" %>
<%@ include file="inc/user_section.jsp" %>
    </div>
</nav>

<div class="pt-[104px] pb-10 px-6 sm:px-10 max-w-6xl mx-auto">

    <%-- Page header with gradient accent and add button --%>
    <div class="flex items-center justify-between mb-8 animate-card-in">
        <div>
            <h1 class="text-3xl font-bold text-gray-900 tracking-tight">Dashboard</h1>
            <p class="text-sm text-gray-400 mt-1 font-medium">Manage your inventory items</p>
        </div>
        <a href="addItem.jsp" class="no-underline text-sm font-semibold text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-5 py-2.5 rounded-xl shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300 inline-flex items-center gap-2">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            Add Item
        </a>
    </div>

    <%-- Empty state card (shown when no items exist) --%>
    <% if(items == null || items.isEmpty()){ %>
    <div class="text-center py-20 animate-card-in">
        <%-- Empty box icon --%>
        <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-indigo-50 to-purple-50 flex items-center justify-center mx-auto mb-6">
            <svg class="w-10 h-10 text-indigo-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                <polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>
            </svg>
        </div>
        <h3 class="text-lg font-bold text-gray-700 mb-2">No items yet</h3>
        <p class="text-sm text-gray-400 mb-6">Start by adding your first inventory item.</p>
        <a href="addItem.jsp" class="no-underline inline-flex text-sm font-semibold text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-5 py-2.5 rounded-xl shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300">Add First Item</a>
    </div>
    <% }else{ %>

    <%-- Item cards grid — responsive: 1 col on mobile, 2 on sm, 3 on lg --%>
    <div class="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <% for(int i = 0; i < items.size(); i++){ Item item = items.get(i); %>
        <div class="bg-white rounded-2xl shadow-sm border border-gray-100 hover:shadow-lg hover:border-indigo-100 hover:-translate-y-1 transition-all duration-300 animate-card-in relative group overflow-hidden" style="animation-delay: <%= i * 0.05 %>s">
            <%-- Entire card body is clickable — links to item details page --%>
            <a href="itemController?action=showItemDetails&itemId=<%= item.getId() %>" class="block no-underline">
                <%-- Top gradient accent bar (indigo → purple → pink) --%>
                <div class="h-1 bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-400"></div>

                <div class="p-5 pb-3">
                    <%-- ID badge + item name --%>
                    <div class="flex items-center gap-2 mb-3">
                        <span class="text-[10px] font-bold text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-2 py-0.5 rounded-md tracking-wider">#<%= item.getId() %></span>
                        <h3 class="text-base font-bold text-gray-900 truncate flex-1 min-w-0"><%= item.getName() %></h3>
                    </div>

                    <%-- Price and quantity stats in two boxes --%>
                    <div class="flex gap-3">
                        <div class="flex-1 bg-gray-50 rounded-xl px-4 py-3">
                            <span class="text-[10px] uppercase tracking-wider text-gray-400 font-semibold block mb-0.5">Price</span>
                            <span class="text-lg font-bold text-gray-800">$<%= String.format("%.2f", item.getPrice()) %></span>
                        </div>
                        <div class="flex-1 bg-gray-50 rounded-xl px-4 py-3">
                            <span class="text-[10px] uppercase tracking-wider text-gray-400 font-semibold block mb-0.5">Stock</span>
                            <span class="text-lg font-bold text-gray-800"><%= item.getTotalNumber() %></span>
                        </div>
                    </div>
                </div>
            </a>

            <%-- Edit and Delete action buttons (below the clickable card body) --%>
            <div class="px-5 pb-5 pt-0 flex gap-2">
                <a href="itemController?action=showItem&id=<%= item.getId() %>" class="flex-1 text-center no-underline text-sm font-semibold text-indigo-600 bg-indigo-50 py-2.5 rounded-xl hover:bg-indigo-100 active:scale-95 transition-all duration-200">Edit</a>
                <button onclick="openModal(<%= item.getId() %>)" class="flex-1 text-sm font-semibold text-red-500 bg-red-50 py-2.5 rounded-xl hover:bg-red-100 active:scale-95 transition-all duration-200 cursor-pointer border-none">Delete</button>
            </div>
        </div>
        <% } %>
    </div>
    <% } %>
</div>

<%-- Delete confirmation modal (styled, replaces browser confirm()) --%>
<div id="deleteModal" class="fixed inset-0 z-50 flex items-center justify-center p-5 bg-black/40 opacity-0 invisible transition-all duration-300">
    <div class="bg-white rounded-2xl p-8 w-full max-w-sm shadow-2xl animate-card-in text-center">
        <%-- Warning icon --%>
        <div class="w-14 h-14 rounded-xl bg-red-50 flex items-center justify-center mx-auto mb-4 text-red-500">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
        </div>
        <h3 class="text-lg font-bold text-gray-900 mb-2">Delete Item?</h3>
        <p class="text-sm text-gray-400 mb-6">This action cannot be undone.</p>
        <div class="flex gap-3">
            <button onclick="closeModal()" class="flex-1 bg-gray-100 text-gray-500 py-2.5 rounded-xl font-semibold text-sm hover:bg-gray-200 transition-all duration-200 cursor-pointer border-none">Cancel</button>
            <a id="confirmDelete" href="#" class="flex-1 text-center no-underline text-white bg-gradient-to-r from-red-500 to-pink-600 py-2.5 rounded-xl font-semibold text-sm shadow-md shadow-red-500/25 hover:shadow-lg hover:shadow-red-500/35 transition-all duration-300">Delete</a>
        </div>
    </div>
</div>

<%-- JavaScript to toggle the modal and set the dynamic delete link --%>
<script>
// Opens the modal and sets the delete link href to the selected item
function openModal(id){
    document.getElementById('confirmDelete').href = 'itemController?action=deleteItem&id=' + id;
    document.getElementById('deleteModal').classList.remove('opacity-0', 'invisible');
}
// Closes the modal
function closeModal(){
    document.getElementById('deleteModal').classList.add('opacity-0', 'invisible');
}
// Close modal when clicking the backdrop (outside the card)
document.getElementById('deleteModal').addEventListener('click', function(e){
    if(e.target === this) closeModal();
});
</script>

</body>
</html>
