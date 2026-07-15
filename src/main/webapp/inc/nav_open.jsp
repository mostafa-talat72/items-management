<%--
  NAV_OPEN.JSP — Shared top navigation bar (OPENING).

  This is the START of the navbar, included after head.jsp and the <body> tag.
  Each page then adds its own nav content (page title, buttons, user section)
  between this include and the closing </div></nav> tags.

  Structure:
    <nav> — fixed top bar with backdrop blur
      Left: Logo (IM badge) + "ItemManager" brand name
      Right: The including page adds its buttons/controls here

  The user section (login/logout/profile dropdown) is added via user_section.jsp,
  which is included by pages that have user authentication.

  The nav_open.jsp is NOT closed here — the closing </nav> tag is in the calling page.
--%>
<nav class="fixed top-0 left-0 right-0 h-[72px] bg-white/80 backdrop-blur-md border-b border-gray-100 z-50 flex items-center justify-between px-6 sm:px-10">
    <%-- Left side: Logo badge + brand name link to dashboard --%>
    <a href="itemController?action=showItems" class="flex items-center gap-2 no-underline">
        <%-- Gradient logo badge with "IM" initials --%>
        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xs font-bold shadow-md shadow-purple-500/20">IM</div>
        <span class="text-base font-bold text-gray-800">ItemManager</span>
    </a>
    <%-- Right side: container for page-specific buttons and user section --%>
    <div class="flex items-center gap-3">
