// Package declaration — service-layer interface for the ITEM_DETAILS entity (one-to-one with Item)
package com.items.service;

// List (if needed for future expansion) — currently each item has at most one detail
// import java.util.List;
// ItemDetails is the domain model this service manages
import com.items.model.ItemDetails;

/**
 * Service-layer interface for the one-to-one ItemDetails entity.
 *
 * Each Item in the system can have at most one ItemDetails record
 * (enforced by application logic — the DB uses a regular FK, not a UNIQUE constraint).
 * This service provides full CRUD operations for the detail record.
 *
 * The pattern follows ItemService: controller → interface → implementation → JDBC.
 */
public interface ItemDetailsService {

    /**
     * Retrieves the detail record associated with a specific item.
     * Since the relationship is one-to-one, this returns at most one record.
     *
     * @param itemId the parent item's database ID
     * @return the ItemDetails if one exists, or null if the item has no detail record
     */
    ItemDetails getItemDetailsByItemId(long itemId);

    /**
     * Retrieves a specific detail record by its own primary key.
     * Used by the update form to pre-fill the existing description.
     *
     * @param id the detail record's ID (not the item's ID)
     * @return the ItemDetails if found, or null if no such record exists
     */
    ItemDetails getItemDetailById(long id);

    /**
     * Creates a NEW detail record for an item.
     * The itemId links this detail to its parent item in the database.
     *
     * @param detail the ItemDetails to insert (must have itemId set, no ID)
     * @return true if the insert was successful
     * @throws RuntimeException wrapping SQLException on FK or CHECK violations
     */
    boolean addItemDetail(ItemDetails detail);

    /**
     * Updates an EXISTING detail record's description.
     * The record is identified by its own ID.
     *
     * @param detail the ItemDetails with updated fields (must have a valid ID)
     * @return true if a row was updated
     * @throws RuntimeException wrapping SQLException on CHECK constraint violations
     */
    boolean updateItemDetail(ItemDetails detail);

    /**
     * Deletes a detail record by its own primary key.
     * This does NOT delete the parent item — only the associated detail.
     *
     * @param id the detail record's ID to remove
     * @return true if a record was deleted
     */
    boolean deleteItemDetail(long id);
}
