// Package declaration — JDBC-based implementation of the ItemDetailsService interface
package com.items.service.impl;

// Core JDBC classes for database interaction (Connection, PreparedStatement, ResultSet, SQLException)
import java.sql.*;

// DataSource is the connection pool obtained from Tomcat's JNDI context.xml
import javax.sql.DataSource;

// ItemDetails is the domain model we're persisting (one-to-one with Item)
import com.items.model.ItemDetails;
// ItemDetailsService is the interface we're implementing
import com.items.service.ItemDetailsService;

/**
 * JDBC implementation of ItemDetailsService.
 *
 * This manages the ITEM_DETAILS table which stores one detail record per item.
 * The table structure:
 *   ID          NUMBER(10) PK       — auto-generated detail record ID
 *   ITEM_ID     NUMBER(10) NOT NULL — FK to ITEMS(ID) — which item this belongs to
 *   DESCRIPTION VARCHAR2(500)       — the detail text (min 3 chars via CHECK constraint)
 *   CREATED_AT  TIMESTAMP           — auto-set on INSERT
 *   UPDATED_AT  TIMESTAMP           — auto-updated on every modification
 *
 * All SQLException instances are wrapped in RuntimeException so the controller
 * can handle them in a single generic catch block.
 */
public class ItemDetailsServiceImpl implements ItemDetailsService {

    // The connection pool from Tomcat's JNDI — injected by the controller
    private DataSource dataSource;

    /**
     * Constructor receiving a DataSource from the controller.
     *
     * @param dataSource the Tomcat connection pool (defined in META-INF/context.xml)
     */
    public ItemDetailsServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Borrows a single connection from the connection pool.
     *
     * @return a pooled java.sql.Connection
     * @throws SQLException if the pool is empty or the database is down
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Maps the CURRENT ROW of a ResultSet into an ItemDetails object.
     * Includes timestamps (createdAt, updatedAt) which are set separately after the constructor.
     *
     * @param resultSet the ResultSet positioned at the row to extract
     * @return a fully populated ItemDetails object
     * @throws SQLException if column access fails
     */
    private ItemDetails extractItemDetail(ResultSet resultSet) throws SQLException {
        // Create ItemDetails with the three main columns
        ItemDetails d = new ItemDetails(
            resultSet.getLong("ID"),              // Primary key
            resultSet.getLong("ITEM_ID"),         // Foreign key — parent item ID
            resultSet.getString("DESCRIPTION")     // Detail text content
        );
        // Set timestamps — these are fetched separately since the constructor doesn't include them
        d.setCreatedAt(resultSet.getTimestamp("CREATED_AT"));
        d.setUpdatedAt(resultSet.getTimestamp("UPDATED_AT"));
        return d;
    }

    /**
     * Retrieves the detail record for a specific item by its FK (ITEM_ID).
     * Since the relationship is treated as one-to-one, this returns at most one record.
     *
     * @param itemId the parent item's database ID
     * @return the ItemDetails if one exists, or null if the item has no detail record
     */
    @Override
    public ItemDetails getItemDetailsByItemId(long itemId) {
        // Query: find the detail record linked to this item
        String query = "SELECT * FROM ITEM_DETAILS WHERE ITEM_ID = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Bind the item ID parameter
            preparedStatement.setLong(1, itemId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) return extractItemDetail(resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Retrieves a specific detail record by its own primary key.
     * Used by the update form to pre-fill the description field.
     *
     * @param id the detail record's ID (not the item's ID)
     * @return the ItemDetails if found, or null
     */
    @Override
    public ItemDetails getItemDetailById(long id) {
        // Query: find the detail record by its own primary key
        String query = "SELECT * FROM ITEM_DETAILS WHERE ID = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setLong(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) return extractItemDetail(resultSet);
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new detail record for an item.
     * The ITEM_ID foreign key links this detail to its parent item.
     *
     * @param detail the ItemDetails to insert (must have itemId and description set)
     * @return true on successful insert
     * @throws RuntimeException wrapping SQLException on:
     *         - ORA-02291: FK violation (itemId doesn't exist in ITEMS table)
     *         - ORA-02290: CHECK constraint violation (description too short)
     *         - ORA-01400: NOT NULL violation (missing required field)
     */
    @Override
    public boolean addItemDetail(ItemDetails detail) {
        // INSERT — ID is auto-generated; timestamps use DB defaults
        String query = "INSERT INTO ITEM_DETAILS (ITEM_ID, DESCRIPTION) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setLong(1, detail.getItemId());
            preparedStatement.setString(2, detail.getDescription());
            preparedStatement.execute();
            return true;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates an existing detail record's description.
     * The record is identified by its own ID — the UPDATED_AT timestamp
     * is automatically set by the database trigger or CURRENT_TIMESTAMP default.
     *
     * @param detail the ItemDetails with the updated description (must have a valid ID)
     * @return true if a row was affected
     */
    @Override
    public boolean updateItemDetail(ItemDetails detail) {
        // UPDATE only the description; timestamps are handled by the DB
        String query = "UPDATE ITEM_DETAILS SET DESCRIPTION = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE ID = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, detail.getDescription());
            preparedStatement.setLong(2, detail.getId());
            preparedStatement.execute();
            return true;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a detail record by its primary key.
     * This removes only the detail — the parent item remains untouched.
     *
     * @param id the detail record's ID to delete
     * @return true if a row was deleted
     */
    @Override
    public boolean deleteItemDetail(long id) {
        // Simple DELETE by primary key
        String query = "DELETE FROM ITEM_DETAILS WHERE ID = ?";
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
