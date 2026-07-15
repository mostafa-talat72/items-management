// Package declaration — this servlet handles ALL item-related HTTP requests
package com.items.controller;

// Standard Java I/O and web packages
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// JNDI resource injection for the database connection pool
import javax.annotation.Resource;
// Servlet API classes
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
// DataSource from the connection pool (Tomcat JNDI)
import javax.sql.DataSource;

// Domain and service classes
import com.items.model.Item;
import com.items.model.ItemDetails;
import com.items.service.ItemDetailsService;
import com.items.service.ItemService;
import com.items.service.impl.ItemDetailsServiceImpl;
import com.items.service.impl.ItemServiceImpl;
import com.items.util.ItemDetailsValidator;
import com.items.util.ItemValidator;

/**
 * Servlet handling ALL item-related operations:
 *
 *   Dashboard (list items with cards)
 *   View item details (one-to-one with ItemDetails)
 *   CRUD for items (add, update, delete)
 *   CRUD for item details (add, update, delete)
 *
 * URL pattern: /itemController?action=[actionName]
 *
 * This servlet uses the Post/Redirect/Get (PRG) pattern:
 *   - Both GET and POST are routed through doGet()
 *   - Form processing always ends with a redirect (not a forward)
 *   - This prevents duplicate form submissions on page refresh
 *
 * Session checking:
 *   - All item operations require a valid session
 *   - If no session exists, checks for a "rememberUserId" cookie
 *   - If no cookie, redirects to login page
 *
 * Validation strategy (two-phase):
 *   Phase 1 — Before DB: check required fields and format (empty strings, number parse)
 *   Phase 2 — After DB error: parse SQL constraint violations into field-level messages
 */
@WebServlet("/itemController")
public class ItemController extends HttpServlet {

    // Database connection pool injected by Tomcat (configured in META-INF/context.xml)
    @Resource(name = "jdbc/items/connection")
    private DataSource dataSource;

    // Service objects handling business logic and database operations
    private ItemService itemService;
    private ItemDetailsService itemDetailsService;

    // Default redirect target for "Home" or "Back to Dashboard"
    private static final String HOME = "itemController?action=showItems";

    /**
     * Initializes the servlet — called once when the servlet is first loaded.
     * Creates all service implementations with the injected DataSource.
     */
    @Override
    public void init() throws ServletException {
        itemService = new ItemServiceImpl(dataSource);
        itemDetailsService = new ItemDetailsServiceImpl(dataSource);
    }

    /**
     * Routes ALL GET requests based on the "action" parameter.
     * Checks for an active session first — if missing, tries "remember me" cookie.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Session check: if no user session exists, try the remember-me cookie
        HttpSession session = request.getSession(false);
        if(session == null || session.getAttribute("user") == null) {
            Cookie[] cookies = request.getCookies();
            if(cookies != null) {
                for(Cookie cookie : cookies) {
                    if("rememberUserId".equals(cookie.getName())) {
                        // Save the current URL so we can redirect back after auto-login
                        String currentUri = request.getRequestURI();
                        String qs = request.getQueryString();
                        String fullPath = currentUri + (qs != null ? "?" + qs : "");
                        String encodedPath = java.net.URLEncoder.encode(fullPath, "UTF-8");
                        // Auto-login via UserController
                        response.sendRedirect(request.getContextPath() + "/userController?action=selectUser&id=" + cookie.getValue() + "&redirectPath=" + encodedPath);
                        return;
                    }
                }
            }
            // No session and no cookie — redirect to login
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        // Read the action parameter — default to "showItems" if not provided
        String action = request.getParameter("action");
        if(Objects.isNull(action)) action = "showItems";

        // Route to the appropriate handler method based on the action parameter
        switch(action) {
            case "showItem":         showItem(request, response);              break;
            case "showItemDetails":     showItemDetails(request, response);         break;
            case "showAddItemDetail":      showAddItemDetail(request, response);       break;
            case "showUpdateItemDetail":   showUpdateItemDetail(request, response);    break;
            case "addItemDetail":          addItemDetail(request, response);           break;
            case "updateItemDetail":       updateItemDetail(request, response);        break;
            case "deleteItemDetail":       deleteItemDetail(request, response);        break;
            case "addItem":             addItem(request, response);                 break;
            case "updateItem":       updateItem(request, response);            break;
            case "deleteItem":       deleteItem(request, response);            break;
            default:                 showItems(request, response);
        }
    }

    /**
     * POST requests are handled identically to GET (PRG pattern).
     * The actual form processing happens in the same methods.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Fetches all items from the database and forwards to the items.jsp dashboard.
     * This is the default action (when no action parameter is provided).
     */
    private void showItems(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        List<Item> items = itemService.getItems();
        forwardTo(request, response, "/items.jsp", "items", items);
    }

    /**
     * Fetches a single item by ID and forwards to the update form (updateItem.jsp).
     * Used when the user clicks the "Edit" button on an item card.
     */
    private void showItem(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        long id = Long.parseLong(request.getParameter("id"));
        Item item = itemService.getItemById(id);
        if(Objects.nonNull(item)) {
            forwardTo(request, response, "/updateItem.jsp", "item", item);
        } else {
            throw new ServletException("Item with ID " + id + " not found.");
        }
    }

    /**
     * Fetches an item AND its detail record, then forwards to itemDetails.jsp.
     * This is the one-to-one detail view — shows item summary + detail card.
     */
    private void showItemDetails(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        long itemId = Long.parseLong(request.getParameter("itemId"));
        Item item = itemService.getItemById(itemId);
        if(Objects.isNull(item)) throw new ServletException("Item with ID " + itemId + " not found.");

        // Set both the item and its detail record as request attributes
        request.setAttribute("item", item);
        request.setAttribute("itemDetail", itemDetailsService.getItemDetailsByItemId(itemId));
        try {
            request.getRequestDispatcher("/itemDetails.jsp").forward(request, response);
        } catch (ServletException | IOException e) {
            throw new ServletException("Error forwarding to itemDetails.jsp", e);
        }
    }

    /**
     * Shows the add-item-detail form (addItemDetail.jsp) for a given item.
     * Passes the item's name and ID so the form can display context.
     */
    private void showAddItemDetail(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        long itemId = Long.parseLong(request.getParameter("itemId"));
        Item item = itemService.getItemById(itemId);
        if(Objects.isNull(item)) throw new ServletException("Item with ID " + itemId + " not found.");
        request.setAttribute("itemId", item.getId());
        request.setAttribute("itemName", item.getName());
        try {
            request.getRequestDispatcher("/addItemDetail.jsp").forward(request, response);
        } catch (ServletException | IOException e) {
            throw new ServletException("Error forwarding to addItemDetail.jsp", e);
        }
    }

    /**
     * Shows the update-item-detail form (updateItemDetail.jsp) for an existing detail record.
     * Pre-fills the description field from the database.
     */
    private void showUpdateItemDetail(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        long id = Long.parseLong(request.getParameter("id"));
        long itemId = Long.parseLong(request.getParameter("itemId"));
        ItemDetails detail = itemDetailsService.getItemDetailById(id);
        if(Objects.isNull(detail)) throw new ServletException("Item detail with ID " + id + " not found.");
        request.setAttribute("itemDetail", detail);
        loadItem(request, itemId);
        try {
            request.getRequestDispatcher("/updateItemDetail.jsp").forward(request, response);
        } catch (ServletException | IOException e) {
            throw new ServletException("Error forwarding to updateItemDetail.jsp", e);
        }
    }

    /**
     * Handles the ADD item detail form submission.
     *
     * Two-phase validation:
     *   1. validateEmptyAndFormat — checks that description is not empty and is ≥ 3 chars
     *   2. On SQLException — parses known constraint violations (FK, CHECK)
     */
    private void addItemDetail(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String itemIdStr = request.getParameter("itemId");
        String description = request.getParameter("description");

        // Phase 1: Client-side validation
        Map<String, String> errors = ItemDetailsValidator.validateEmptyAndFormat(description);

        long itemId = Long.parseLong(itemIdStr);

        if(!errors.isEmpty()) {
            // Validation failed — re-display form with errors
            request.setAttribute("errors", errors);
            request.setAttribute("submitted", new ItemDetails(itemId, description));
            request.setAttribute("itemId", itemId);
            request.setAttribute("itemName", itemService.getItemById(itemId).getName());
            request.getRequestDispatcher("/addItemDetail.jsp").forward(request, response);
            return;
        }

        // Attempt the database INSERT
        ItemDetails detail = new ItemDetails(itemId, description);
        try {
            if(itemDetailsService.addItemDetail(detail)) {
                // Success — redirect to the item details page
                response.sendRedirect(request.getContextPath() + "/itemController?action=showItemDetails&itemId=" + itemId);
            }
        } catch(RuntimeException e) {
            // Phase 2: Database error — parse SQL constraint violations
            Map<String, String> bizErrors = new HashMap<>();
            handleSqlError(e, bizErrors);
            if(!bizErrors.isEmpty()) {
                // Known constraint — re-display form with error messages
                request.setAttribute("errors", bizErrors);
                request.setAttribute("submitted", detail);
                request.setAttribute("itemId", itemId);
                request.setAttribute("itemName", itemService.getItemById(itemId).getName());
                request.getRequestDispatcher("/addItemDetail.jsp").forward(request, response);
                return;
            }
            // Unknown/unexpected error — let the ErrorFilter handle it
            throw new ServletException("Failed to add item detail for item: " + itemId, e);
        }
    }

    /**
     * Handles the UPDATE item detail form submission.
     *
     * Same two-phase validation as addItemDetail.
     */
    private void updateItemDetail(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idStr = request.getParameter("id");
        String itemIdStr = request.getParameter("itemId");
        String description = request.getParameter("description");

        // Phase 1: Client-side validation
        Map<String, String> errors = ItemDetailsValidator.validateEmptyAndFormat(description);
        long id = Long.parseLong(idStr);
        long itemId = Long.parseLong(itemIdStr);

        if(!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("itemDetail", new ItemDetails(id, itemId, description));
            loadItem(request, itemId);
            request.getRequestDispatcher("/updateItemDetail.jsp").forward(request, response);
            return;
        }

        // Attempt the database UPDATE
        ItemDetails detail = new ItemDetails(id, itemId, description);
        try {
            if(itemDetailsService.updateItemDetail(detail)) {
                response.sendRedirect(request.getContextPath() + "/itemController?action=showItemDetails&itemId=" + itemId);
            }
        } catch(RuntimeException e) {
            // Phase 2: Database error — parse SQL constraint violations
            errors.putAll(ItemDetailsValidator.parseSqlError(extractSqlMessage(e)));
            if(!errors.isEmpty()) {
                request.setAttribute("errors", errors);
                request.setAttribute("itemDetail", detail);
                loadItem(request, itemId);
                request.getRequestDispatcher("/updateItemDetail.jsp").forward(request, response);
                return;
            }
            throw new ServletException("Failed to update item detail: " + id, e);
        }
    }

    /**
     * Deletes an item detail record and redirects back to the item details page.
     * The modal confirmation is handled client-side — this method just performs the deletion.
     */
    private void deleteItemDetail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long id = Long.parseLong(request.getParameter("id"));
        long itemId = Long.parseLong(request.getParameter("itemId"));
        itemDetailsService.deleteItemDetail(id);
        response.sendRedirect(request.getContextPath() + "/itemController?action=showItemDetails&itemId=" + itemId);
    }

    /**
     * Loads an Item into request attributes for JSP display.
     * Used when forwarding to pages that need to show the parent item name.
     */
    private void loadItem(HttpServletRequest request, long itemId) {
        request.setAttribute("item", itemService.getItemById(itemId));
    }

    /**
     * Handles the ADD item form submission.
     *
     * Two-phase validation:
     *   1. validateEmptyAndFormat — checks required fields and number parsing
     *   2. On SQLException — parses known constraints + checks business rules (negative/zero)
     */
    private void addItem(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getParameter("name");
        String priceStr = request.getParameter("price");
        String totalNumberStr = request.getParameter("totalNumber");

        // Phase 1: Client-side validation (required fields, number format)
        Map<String, String> errors = ItemValidator.validateEmptyAndFormat(name, priceStr, totalNumberStr);

        if(!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("submitted", buildItem(name, priceStr, totalNumberStr));
            request.getRequestDispatcher("/addItem.jsp").forward(request, response);
            return;
        }

        // Attempt the database INSERT
        Item item = new Item(name, Double.parseDouble(priceStr), Integer.parseInt(totalNumberStr));
        try {
            if(itemService.addItem(item)) {
                response.sendRedirect(HOME);
            }
        } catch(RuntimeException e) {
            // Phase 2: Database error — merge business rules with SQL constraint messages
            Map<String, String> bizErrors = ItemValidator.validateBusinessRules(priceStr, totalNumberStr);
            handleSqlError(e, bizErrors);
            if(!bizErrors.isEmpty()) {
                request.setAttribute("errors", bizErrors);
                request.setAttribute("submitted", item);
                request.getRequestDispatcher("/addItem.jsp").forward(request, response);
                return;
            }
            throw new ServletException("Failed to add item: " + name, e);
        }
    }

    /**
     * Handles the UPDATE item form submission.
     *
     * Same two-phase validation as addItem, plus validates the item ID.
     */
    private void updateItem(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idStr = request.getParameter("id");
        String name = request.getParameter("name");
        String priceStr = request.getParameter("price");
        String totalNumberStr = request.getParameter("totalNumber");

        // Phase 1: Client-side validation
        Map<String, String> errors = ItemValidator.validateEmptyAndFormat(name, priceStr, totalNumberStr);

        // Validate the item ID separately — it must be present and parseable
        if(idStr == null || idStr.trim().isEmpty()) {
            errors.put("id", "Item ID is missing.");
        } else {
            try { Long.parseLong(idStr); }
            catch(NumberFormatException e) { errors.put("id", "Invalid item ID."); }
        }

        if(!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("item", buildItem(idStr, name, priceStr, totalNumberStr));
            request.getRequestDispatcher("/updateItem.jsp").forward(request, response);
            return;
        }

        // Attempt the database UPDATE
        Item item = new Item(Long.parseLong(idStr), name, Double.parseDouble(priceStr), Integer.parseInt(totalNumberStr));
        try {
            if(itemService.updateItem(item)) {
                response.sendRedirect(HOME);
            }
        } catch(RuntimeException e) {
            // Phase 2: Database error
            Map<String, String> bizErrors = ItemValidator.validateBusinessRules(priceStr, totalNumberStr);
            handleSqlError(e, bizErrors);
            if(!bizErrors.isEmpty()) {
                request.setAttribute("errors", bizErrors);
                request.setAttribute("item", item);
                request.getRequestDispatcher("/updateItem.jsp").forward(request, response);
                return;
            }
            throw new ServletException("Failed to update item with ID: " + idStr, e);
        }
    }

    /**
     * Deletes an item by its ID.
     * First deletes any associated item detail (if it exists), then deletes the item itself.
     * This cascading delete prevents foreign-key constraint violations.
     */
    private void deleteItem(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long id = Long.parseLong(request.getParameter("id"));
        try {
            // Delete any associated detail record first (to avoid FK violation)
            ItemDetails itemDetails = itemDetailsService.getItemDetailsByItemId(id);
            if(itemDetails != null) {
                itemDetailsService.deleteItemDetail(itemDetails.getId());
            }
            // Now delete the item itself
            if(itemService.removeItemById(id)) {
                response.sendRedirect(HOME);
            }
        } catch(RuntimeException e) {
            // Handle any remaining constraint violations
            String msg = extractSqlMessage(e);
            if(msg != null && msg.contains("ORA-02292")) {
                throw new ServletException("Cannot delete this item. It is linked to other records.");
            }
            throw new ServletException("Failed to delete item with ID: " + id, e);
        }
    }

    /**
     * Handles SQL errors by walking the exception cause chain and merging
     * parsed constraint messages with the existing error map.
     * Tries ItemValidator first, then ItemDetailsValidator.
     *
     * @param e      the RuntimeException wrapping a SQLException
     * @param errors the error map to add field-level messages to
     */
    private void handleSqlError(RuntimeException e, Map<String, String> errors) {
        String msg = extractSqlMessage(e);
        if(msg == null) return;
        errors.putAll(ItemValidator.parseSqlError(msg));
        if(errors.isEmpty()) errors.putAll(ItemDetailsValidator.parseSqlError(msg));
    }

    /**
     * Walks the nested exception cause chain to find the first SQLException message.
     * The service layer wraps SQLException in RuntimeException, and there may be
     * multiple layers of wrapping.
     *
     * @param e the RuntimeException to analyze
     * @return the SQL error message string, or null if no SQLException is in the chain
     */
    private String extractSqlMessage(RuntimeException e) {
        Throwable cause = e;
        while(cause != null) {
            if(cause instanceof SQLException) {
                return cause.getMessage();
            }
            cause = cause.getCause();
        }
        return null;
    }

    /**
     * Builds an Item from raw form strings for re-displaying the form.
     * Parse exceptions are deliberately ignored because:
     *   - If validation passed, the strings are guaranteed parseable
     *   - If validation failed, error messages are already in the errors map,
     *     and defaulting to 0 is harmless since the form re-renders with field errors
     */
    private Item buildItem(String name, String priceStr, String totalNumberStr) {
        double price = 0;
        int totalNumber = 0;
        try { price = Double.parseDouble(priceStr); } catch(Exception ignored) {}
        try { totalNumber = Integer.parseInt(totalNumberStr); } catch(Exception ignored) {}
        return new Item(name, price, totalNumber);
    }

    /**
     * Overloaded buildItem that also parses the ID string.
     * Same ignore rationale applies — used by the update form.
     */
    private Item buildItem(String idStr, String name, String priceStr, String totalNumberStr) {
        long id = 0;
        double price = 0;
        int totalNumber = 0;
        try { id = Long.parseLong(idStr); } catch(Exception ignored) {}
        try { price = Double.parseDouble(priceStr); } catch(Exception ignored) {}
        try { totalNumber = Integer.parseInt(totalNumberStr); } catch(Exception ignored) {}
        return new Item(id, name, price, totalNumber);
    }

    /**
     * Helper method that sets a request attribute and forwards to a JSP.
     * Wraps checked exceptions in ServletException for clean error handling.
     */
    private void forwardTo(HttpServletRequest request, HttpServletResponse response, String path, String attr, Object val) throws ServletException {
        request.setAttribute(attr, val);
        try {
            request.getRequestDispatcher(path).forward(request, response);
        } catch (ServletException | IOException e) {
            throw new ServletException("Error forwarding to " + path, e);
        }
    }
}
