<%--
  PROFILE.JSP — User profile page with tabbed interface.

  Two tabs:
    1. Edit Profile — update full name and email
    2. Change Password — change current password with old password verification

  Plus:
    - Read-only account information section (username, email, created/updated dates, status)
    - Delete Account section with styled modal confirmation

  Features:
    - Tab switching via JavaScript (no page reload)
    - Field-level validation errors (red borders + SVG icons)
    - Delete confirmation modal (styled, not browser confirm())
    - Staggered card entrance animations

  Flow:
    1. User is loaded from session (user_section.jsp is included)
    2. Tab navigation switches between edit and password forms
    3. Edit form → POST to userController?action=updateUserInfo
    4. Password form → POST to userController?action=updateUserPassword
    5. Delete → GET to userController?action=deleteUser (after modal confirmation)

  Note: This page uses request.getContextPath() for form actions to ensure
  correct URL resolution regardless of deployment context.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>
<%
    request.setAttribute("pageTitle", "Profile");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
%>
<%@ include file="inc/head.jsp" %>
<%@ include file="inc/nav_open.jsp" %>
<%@ include file="inc/user_section.jsp" %>
    </div>
</nav>

<div class="pt-[104px] pb-10 px-6 sm:px-10 max-w-3xl mx-auto">
    <div class="animate-card-in">
        <%-- Profile header with avatar --%>
        <div class="flex items-center gap-5 mb-8">
            <%-- Gradient avatar with first letter of full name --%>
            <div class="w-16 h-16 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xl font-bold shadow-lg shadow-purple-500/20"><%= user.getFullName().charAt(0) %></div>
            <div>
                <h1 class="text-2xl font-bold text-gray-900"><%= user.getFullName() %></h1>
                <p class="text-sm text-gray-400">@<%= user.getUsername() %></p>
            </div>
        </div>

        <%-- Tab navigation --%>
        <div class="flex gap-1 bg-gray-100 p-1 rounded-xl mb-8">
            <button onclick="switchTab('edit')" id="tabEditBtn" class="flex-1 py-2.5 px-4 rounded-lg text-sm font-semibold transition-all duration-200 cursor-pointer border-none bg-white text-gray-900 shadow-sm">Edit Profile</button>
            <button onclick="switchTab('password')" id="tabPassBtn" class="flex-1 py-2.5 px-4 rounded-lg text-sm font-semibold transition-all duration-200 cursor-pointer border-none bg-transparent text-gray-500 hover:text-gray-700">Change Password</button>
        </div>

        <%-- Edit Profile tab --%>
        <div id="tabEdit" class="bg-white rounded-2xl p-8 shadow-md border border-gray-50">
            <h2 class="text-lg font-bold text-gray-900 mb-6">Edit Profile</h2>
            <form action="<%= request.getContextPath() %>/userController?action=updateUserInfo&id=<%= user.getId() %>" method="post">
                <%-- Full Name field --%>
                <div class="mb-5">
                    <label class="block text-sm font-semibold text-gray-600 mb-2">Full Name</label>
                    <input type="text" name="fullName" value="<%= user.getFullName() %>"
                        class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("fullName") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                    <% if(errors != null && errors.containsKey("fullName")){ %>
                    <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                        <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <p class="text-red-500 text-xs font-medium"><%= errors.get("fullName") %></p>
                    </div>
                    <% } %>
                </div>
                <%-- Email field --%>
                <div class="mb-5">
                    <label class="block text-sm font-semibold text-gray-600 mb-2">Email</label>
                    <input type="email" name="email" value="<%= user.getEmail() %>"
                        class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("email") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                    <% if(errors != null && errors.containsKey("email")){ %>
                    <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                        <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <p class="text-red-500 text-xs font-medium"><%= errors.get("email") %></p>
                    </div>
                    <% } %>
                </div>
                <button type="submit" class="text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-6 py-2.5 rounded-xl font-semibold text-sm shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300 cursor-pointer border-none">Save Changes</button>
            </form>

            <%-- Read-only account information section --%>
            <div class="mt-8 pt-6 border-t border-gray-100">
                <h3 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">Account Information</h3>
                <div class="grid grid-cols-2 gap-4 text-sm">
                    <div><span class="text-gray-400">Username</span><p class="font-semibold text-gray-800 mt-0.5"><%= user.getUsername() %></p></div>
                    <div><span class="text-gray-400">Email</span><p class="font-semibold text-gray-800 mt-0.5"><%= user.getEmail() %></p></div>
                    <div><span class="text-gray-400">Created</span><p class="font-semibold text-gray-800 mt-0.5"><%= user.getCreatedAt() != null ? user.getCreatedAt().toString() : "N/A" %></p></div>
                    <div><span class="text-gray-400">Updated</span><p class="font-semibold text-gray-800 mt-0.5"><%= user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "N/A" %></p></div>
                    <div><span class="text-gray-400">Status</span>
                        <p class="font-semibold mt-0.5 <%=  user.getIsActive() == 1 ? "text-green-600" : "text-red-500" %>">
                            <%= user.getIsActive() == 1 ? "Active" : "Inactive" %>
                        </p>
                    </div>
                </div>
            </div>
        </div>

        <%-- Change Password tab (hidden by default via Tailwind 'hidden' class) --%>
        <div id="tabPassword" class="bg-white rounded-2xl p-8 shadow-md border border-gray-50 hidden">
            <h2 class="text-lg font-bold text-gray-900 mb-6">Change Password</h2>
            <form action="<%= request.getContextPath() %>/userController?action=updateUserPassword&id=<%= user.getId() %>&username=<%= user.getUsername() %>" method="post">
                <%-- Current Password field — used to verify identity before allowing change --%>
                <div class="mb-5">
                    <label class="block text-sm font-semibold text-gray-600 mb-2">Current Password</label>
                    <input type="password" name="currentPassword" placeholder="Enter current password"
                        class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("currentPassword") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                    <% if(errors != null && errors.containsKey("currentPassword")){ %>
                    <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                        <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <p class="text-red-500 text-xs font-medium"><%= errors.get("currentPassword") %></p>
                    </div>
                    <% } %>
                </div>
                <%-- New Password field --%>
                <div class="mb-5">
                    <label class="block text-sm font-semibold text-gray-600 mb-2">New Password</label>
                    <input type="password" name="newPassword" placeholder="Min 6 characters"
                        class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("newPassword") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                    <% if(errors != null && errors.containsKey("newPassword")){ %>
                    <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                        <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <p class="text-red-500 text-xs font-medium"><%= errors.get("newPassword") %></p>
                    </div>
                    <% } %>
                </div>
                <%-- Confirm New Password field --%>
                <div class="mb-8">
                    <label class="block text-sm font-semibold text-gray-600 mb-2">Confirm New Password</label>
                    <input type="password" name="confirmNewPassword" placeholder="Repeat new password"
                        class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("confirmNewPassword") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                    <% if(errors != null && errors.containsKey("confirmNewPassword")){ %>
                    <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                        <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        <p class="text-red-500 text-xs font-medium"><%= errors.get("confirmNewPassword") %></p>
                    </div>
                    <% } %>
                </div>
                <button type="submit" class="text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-6 py-2.5 rounded-xl font-semibold text-sm shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300 cursor-pointer border-none">Update Password</button>
            </form>
        </div>

        <%-- Delete Account section --%>
        <div class="mt-8 bg-white rounded-2xl p-8 shadow-md border border-gray-50">
            <div class="flex items-center justify-between">
                <div>
                    <h3 class="text-sm font-bold text-gray-900">Delete Account</h3>
                    <p class="text-xs text-gray-400 mt-1">Permanently remove your account and all data</p>
                </div>
                <button onclick="openDeleteModal()" class="text-sm font-semibold text-red-500 bg-red-50 px-5 py-2.5 rounded-xl hover:bg-red-100 active:scale-95 transition-all duration-200 cursor-pointer border-none">Delete Account</button>
            </div>
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
                <h3 class="text-lg font-bold text-gray-900 mb-2">Delete Account?</h3>
                <p class="text-sm text-gray-400 mb-6">This action is permanent and cannot be undone.</p>
                <div class="flex gap-3">
                    <button onclick="closeDeleteModal()" class="flex-1 bg-gray-100 text-gray-500 py-2.5 rounded-xl font-semibold text-sm hover:bg-gray-200 transition-all duration-200 cursor-pointer border-none">Cancel</button>
                    <a href="<%= request.getContextPath() %>/userController?action=deleteUser&id=<%= user.getId() %>" class="flex-1 text-center no-underline text-white bg-gradient-to-r from-red-500 to-pink-600 py-2.5 rounded-xl font-semibold text-sm shadow-md shadow-red-500/25 hover:shadow-lg hover:shadow-red-500/35 transition-all duration-300">Delete</a>
                </div>
            </div>
        </div>

    </div>
</div>

<%-- JavaScript for tab switching and modal control --%>
<script>
// Switches between Edit Profile and Change Password tabs
function switchTab(tab){
    document.getElementById('tabEdit').classList.toggle('hidden', tab !== 'edit');
    document.getElementById('tabPassword').classList.toggle('hidden', tab !== 'password');
    document.getElementById('tabEditBtn').classList.toggle('bg-white', tab === 'edit');
    document.getElementById('tabEditBtn').classList.toggle('shadow-sm', tab === 'edit');
    document.getElementById('tabEditBtn').classList.toggle('bg-transparent', tab !== 'edit');
    document.getElementById('tabPassBtn').classList.toggle('bg-white', tab === 'password');
    document.getElementById('tabPassBtn').classList.toggle('shadow-sm', tab === 'password');
    document.getElementById('tabPassBtn').classList.toggle('bg-transparent', tab !== 'password');
}
// Opens the delete confirmation modal
function openDeleteModal(){
    document.getElementById('deleteModal').classList.remove('opacity-0', 'invisible');
}
// Closes the delete confirmation modal
function closeDeleteModal(){
    document.getElementById('deleteModal').classList.add('opacity-0', 'invisible');
}
// Close modal when clicking outside the modal card (on the backdrop)
document.getElementById('deleteModal').addEventListener('click', function(e){
    if(e.target === this) closeDeleteModal();
});
</script>

</body>
</html>
