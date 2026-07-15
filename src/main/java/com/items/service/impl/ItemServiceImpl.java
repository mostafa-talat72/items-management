// Package declaration — JDBC-based implementation of the ItemService interface
package com.items.service.impl;

// Core JDBC classes for database interaction
import java.sql.*;
// List and ArrayList for returning query results
import java.util.ArrayList;
import java.util.List;

// DataSource is the connection pool obtained from Tomcat's JNDI context.xml
import javax.sql.DataSource;

// Item is the domain model we're persisting
import com.items.model.Item;
// ItemService is the interface we're implementing
import com.items.service.ItemService;

/**
 * JDBC implementation of ItemService.
 *
 * How it works:
 *   1. The controller injects a DataSource via @Resource(name = "jdbc/items/connection")
 *   2. This class borrows connections from the pool, runs SQL via PreparedStatement,
 *      maps ResultSet rows to Item objects, and returns them
 *   3. All SQLException are wrapped in RuntimeException so the controller can catch
 *      them uniformly with a single catch block
 *
 * Thread safety: DataSource.getConnection() is thread-safe. Each method creates
 * its own connection, statement, and result set within a try-with-resources block.
 */
public class ItemServiceImpl implements ItemService {

    // The connection pool — injected by the controller and shared across all service methods
    private DataSource dataSource;

    /**
     * Constructor that receives a DataSource from the controller.
     * The DataSource is injected into the controller via @Resource (JNDI lookup)
     * and then passed here.
     *
     * @param dataSource the Tomcat connection pool (from context.xml)
     */
    public ItemServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Borrows a single connection from the pool.
     * The caller is responsible for closing it (typically via try-with-resources).
     *
     * @return a java.sql.Connection from the pool
     * @throws SQLException if the pool is exhausted or the DB is unreachable
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Maps the CURRENT ROW of a ResultSet into an Item object.
     * This does NOT call resultSet.next() — the caller must advance the cursor first.
     *
     * @param resultSet the ResultSet positioned at the row to extract
     * @return a new Item populated with the row's column values
     * @throws SQLException if column access fails
     */
    private Item extractItem(ResultSet resultSet) throws SQLException {
        // Create an Item using the full constructor with all four fields from the DB
        return new Item(
            resultSet.getLong("ID"),              // Primary key — NUMBER column
            resultSet.getString("NAME"),          // Item display name — VARCHAR2 column
            resultSet.getDouble("PRICE"),         // Unit price — NUMBER column
            resultSet.getInt("TOTAL_NUMBER")      // Stock quantity — NUMBER column
        );
    }

    /**
     * Returns every item in the ITEMS table.
     * Uses a simple SELECT * with no WHERE clause — suitable for listing the full inventory.
     *
     * @return a List of all Item records (never null, may be empty)
     */
    @Override
    public List<Item> getItems() {
        // SQL query: fetch all columns from all rows
        String query = "SELECT * FROM ITEMS";
        // Try-with-resources: connection, statement, and resultSet are auto-closed
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            // Build the result list by iterating through every row
            List<Item> items = new ArrayList<>();
            while(resultSet.next()) items.add(extractItem(resultSet));
            return items;

        } catch (SQLException e) {
            // Wrap in RuntimeException so the controller's generic catch works
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds a single item by its primary key.
     * Uses a parameterized query (PreparedStatement) to prevent SQL injection.
     *
     * @param id the item's ID to look up
     * @return the matching Item, or null if no record has this ID
     */
    @Override
    public Item getItemById(long id) {
        // Parameterized query — the ? placeholder is filled with the item ID
        String query = "SELECT * FROM ITEMS WHERE ID = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Set the ID parameter (1-based index)
            preparedStatement.setLong(1, id);
            // Execute the query and process the result set
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) return extractItem(resultSet);
            }
            // No item found with this ID — return null
            return null;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts a new item into the database.
     * The ID is auto-generated by the database (sequence + trigger), so the INSERT
     * only specifies name, price, and total_number.
     *
     * @param item the Item to insert (ID field is ignored)
     * @return true on successful insert
     * @throws RuntimeException wrapping SQLException on constraint violations like:
     *         - ORA-00001: duplicate item name (unique constraint UQ_ITEMS_NAME)
     *         - ORA-02290: CHECK constraint violation (negative price, etc.)
     *         - ORA-01400: NOT NULL violation (missing required field)
     */
    @Override
    public boolean addItem(Item item) {
        // INSERT with only the user-provided fields — ID is auto-generated
        String query = "INSERT INTO ITEMS (NAME, PRICE, TOTAL_NUMBER) VALUES (?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Bind the Item's fields to the query parameters
            preparedStatement.setString(1, item.getName());
            preparedStatement.setDouble(2, item.getPrice());
            preparedStatement.setInt(3, item.getTotalNumber());
            // Execute the insert — no rows returned since we don't need the generated ID
            preparedStatement.execute();
            return true;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates an existing item's name, price, and quantity.
     * The item is identified by its ID — the WHERE clause ensures only
     * the matching record is modified.
     *
     * @param item the Item with updated values (must have a valid ID)
     * @return true if at least one row was affected
     */
    @Override
    public boolean updateItem(Item item) {
        // UPDATE all editable fields, identified by the item's ID
        String query = "UPDATE ITEMS SET NAME = ?, PRICE = ?, TOTAL_NUMBER = ? WHERE ID = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Bind updated values (name, price, quantity) and the ID for the WHERE clause
            preparedStatement.setString(1, item.getName());
            preparedStatement.setDouble(2, item.getPrice());
            preparedStatement.setInt(3, item.getTotalNumber());
            preparedStatement.setLong(4, item.getId());
            // executeUpdate returns the count of affected rows
            preparedStatement.execute();
            return true;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes an item by its ID.
     * The caller is responsible for first deleting any child records
     * (like ITEM_DETAILS) to avoid foreign-key constraint violations.
     *
     * @param id the ID of the item to remove
     * @return true if a row was deleted
     * @throws RuntimeException wrapping SQLException with ORA-02292
     *         if child records prevent deletion
     */
    @Override
    public boolean removeItemById(long id) {
        // DELETE by primary key — simple and direct
        String query = "DELETE FROM ITEMS WHERE ID = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setLong(1, id);
            preparedStatement.execute();
            return true;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
