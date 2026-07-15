<%--
  HEAD.JSP — Shared HTML <head> snippet included by EVERY page.

  What this provides:
    - Tailwind CSS v4 (loaded via CDN — no build step needed)
    - Google Fonts: Poppins (300–700 weights) for modern typography
    - Custom theme configuration via Tailwind's @theme directive:
        * font-family: Poppins as the default sans-serif
        * Custom animations: cardIn (card entrance), fadeIn (error/fade elements)
    - The page title is set dynamically via request.getAttribute("pageTitle")

  How to use:
    Every JSP should set request.setAttribute("pageTitle", "My Title") BEFORE including head.jsp.
    head.jsp uses "ItemManager" as the fallback title if pageTitle is not set.

  Note: Tailwind CSS v4 CDN requires the <style type="text/tailwindcss"> block
  to configure theme and custom styles — this is NOT the same as v3's CDN approach.
--%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title><%= request.getAttribute("pageTitle") != null ? request.getAttribute("pageTitle") : "ItemManager" %></title>
<%-- Tailwind CSS v4 CDN — no npm or build-step required --%>
<script src="https://cdn.tailwindcss.com"></script>
<%-- Custom Tailwind theme extension and keyframe animations --%>
<style type="text/tailwindcss">
@theme {
    --font-family-sans: 'Poppins', sans-serif;
    --animate-card-in: cardIn .5s ease backwards;
    --animate-fade-in: fadeIn .2s ease;
}
@keyframes cardIn {
    /* Cards slide up from below with a slight scale — used for all page content containers */
    0% { opacity: 0; transform: translateY(24px) scale(.97); }
    100% { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes fadeIn {
    /* Simple opacity fade — used for error banners and success messages */
    0% { opacity: 0; }
    100% { opacity: 1; }
}
</style>
<%-- Google Fonts preconnect for faster font loading --%>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap" rel="stylesheet">
</head>
