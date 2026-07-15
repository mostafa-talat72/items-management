<%--
  PROFILE.JSP — User profile page with Edit Profile and Change Password
  displayed side by side in a responsive two-column grid. This ensures
  both forms are always visible and validation errors appear correctly
  next to their respective fields.

  Sections:
    1. Profile header (avatar + name + username)
    2. Two-column grid:
       - Left: Edit Profile form (update name + email)
       - Right: Change Password form (current + new + confirm)
    3. Read-only Account Information section (below the forms)
    4. Delete Account button with styled modal confirmation

  Flow:
    - Edit form → POST to userController?action=updateUserInfo
    - Password form → POST to userController?action=updateUserPassword
    - Delete → GET to userController?action=deleteUser (after modal confirmation)

  Note: Both forms appear side-by-side on desktop (lg:grid-cols-2),
  stacked on mobile. Errors display inline via red borders + SVG icons.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>
<%
    request.setAttribute("pageTitle", "Profile");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    boolean hasErrors = errors != null && !errors.isEmpty();
    // Read submitted form values to preserve them on validation error
    String paramFullName = request.getParameter("fullName");
    String paramEmail = request.getParameter("email");
    String paramCurrentPassword = request.getParameter("currentPassword");
    String paramNewPassword = request.getParameter("newPassword");
    String paramConfirmNewPassword = request.getParameter("confirmNewPassword");
%>
<%@ include file="inc/head.jsp" %>
<%@ include file="inc/nav_open.jsp" %>
<%@ include file="inc/user_section.jsp" %>
    </div>
</nav>

<div class="pt-[104px] pb-10 px-6 sm:px-10 max-w-5xl mx-auto">
    <div class="animate-card-in">

        <%-- Profile header with avatar --%>
        <div class="flex items-center gap-5 mb-8">
            <div class="w-16 h-16 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xl font-bold shadow-lg shadow-purple-500/20"><%= user.getFullName().charAt(0) %></div>
            <div>
                <h1 class="text-2xl font-bold text-gray-900"><%= user.getFullName() %></h1>
                <p class="text-sm text-gray-400">@<%= user.getUsername() %></p>
            </div>
        </div>

        <%-- Success message --%>
        <% String success = request.getParameter("success"); %>
        <% if(success != null && !success.trim().isEmpty()){ %>
        <div class="mb-6 flex items-center gap-2 p-4 rounded-xl bg-green-50 border border-green-200 text-green-700 text-sm font-medium animate-fade-in">
            <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span><%= success %></span>
        </div>
        <% } %>

        <%-- Read-only account information section --%>
        <div class="mb-10 bg-white rounded-2xl p-8 shadow-md border border-gray-50">
            <h3 class="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">Account Information</h3>
            <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 text-sm">
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

        <%-- Two-column grid: Edit Profile (left) + Change Password (right) --%>
        <div class="grid gap-6 lg:grid-cols-2">

            <%-- ===== Edit Profile ===== --%>
            <div class="bg-white rounded-2xl p-8 shadow-md border border-gray-50">
                <h2 class="text-lg font-bold text-gray-900 mb-6">Edit Profile</h2>
                <form action="<%= request.getContextPath() %>/userController?action=updateUserInfo&id=<%= user.getId() %>" method="post">
                    <%-- Full Name field --%>
                    <div class="mb-5">
                        <label class="block text-sm font-semibold text-gray-600 mb-2">Full Name</label>
                        <input type="text" name="fullName" value="<%= hasErrors && paramFullName != null ? paramFullName : user.getFullName() %>"
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
                        <input type="email" name="email" value="<%= hasErrors && paramEmail != null ? paramEmail : user.getEmail() %>"
                            class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("email") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                        <% if(errors != null && errors.containsKey("email")){ %>
                        <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                            <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                            <p class="text-red-500 text-xs font-medium"><%= errors.get("email") %></p>
                        </div>
                        <% } %>
                    </div>
                    <button type="submit" class="w-full text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-6 py-2.5 rounded-xl font-semibold text-sm shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300 cursor-pointer border-none">Save Changes</button>
                </form>
            </div>

            <%-- ===== Change Password ===== --%>
            <div class="bg-white rounded-2xl p-8 shadow-md border border-gray-50">
                <h2 class="text-lg font-bold text-gray-900 mb-6">Change Password</h2>
                <form action="<%= request.getContextPath() %>/userController?action=updateUserPassword&id=<%= user.getId() %>&username=<%= user.getUsername() %>" method="post">
                    <%-- Current Password field --%>
                    <div class="mb-5">
                        <label class="block text-sm font-semibold text-gray-600 mb-2">Current Password</label>
                        <input type="password" name="currentPassword" placeholder="Enter current password" value="<%= hasErrors && paramCurrentPassword != null ? paramCurrentPassword : "" %>"
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
                        <input type="password" name="newPassword" placeholder="Min 6 characters" value="<%= hasErrors && paramNewPassword != null ? paramNewPassword : "" %>"
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
                        <input type="password" name="confirmNewPassword" placeholder="Repeat new password" value="<%= hasErrors && paramConfirmNewPassword != null ? paramConfirmNewPassword : "" %>"
                            class="w-full px-4 py-3 rounded-xl border-2 <%= errors != null && errors.containsKey("confirmNewPassword") ? "border-red-400 bg-red-50" : "border-gray-200" %> text-sm outline-none transition-all duration-200 focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 bg-gray-50 focus:bg-white">
                        <% if(errors != null && errors.containsKey("confirmNewPassword")){ %>
                        <div class="flex items-center gap-1.5 mt-1.5 animate-fade-in">
                            <svg class="w-3.5 h-3.5 text-red-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                            <p class="text-red-500 text-xs font-medium"><%= errors.get("confirmNewPassword") %></p>
                        </div>
                        <% } %>
                    </div>
                    <button type="submit" class="w-full text-white bg-gradient-to-r from-indigo-500 to-purple-600 px-6 py-2.5 rounded-xl font-semibold text-sm shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/35 hover:-translate-y-0.5 transition-all duration-300 cursor-pointer border-none">Update Password</button>
                </form>
            </div>

        </div>

        <%-- Delete Account section --%>
        <div class="mt-6 bg-white rounded-2xl p-8 shadow-md border border-gray-50">
            <div class="flex items-center justify-between">
                <div>
                    <h3 class="text-sm font-bold text-gray-900">Delete Account</h3>
                    <p class="text-xs text-gray-400 mt-1">Permanently remove your account and all data</p>
                </div>
                <button onclick="openDeleteModal()" class="text-sm font-semibold text-red-500 bg-red-50 px-5 py-2.5 rounded-xl hover:bg-red-100 active:scale-95 transition-all duration-200 cursor-pointer border-none">Delete Account</button>
            </div>
        </div>

        <%-- Delete confirmation modal --%>
        <div id="deleteModal" class="fixed inset-0 z-50 flex items-center justify-center p-5 bg-black/40 opacity-0 invisible transition-all duration-300">
            <div class="bg-white rounded-2xl p-8 w-full max-w-sm shadow-2xl animate-card-in text-center">
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

<%-- JavaScript for modal control only --%>
<script>
function openDeleteModal(){
    document.getElementById('deleteModal').classList.remove('opacity-0', 'invisible');
}
function closeDeleteModal(){
    document.getElementById('deleteModal').classList.add('opacity-0', 'invisible');
}
document.getElementById('deleteModal').addEventListener('click', function(e){
    if(e.target === this) closeDeleteModal();
});
</script>

</body>
</html>
