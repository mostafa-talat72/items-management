// Package declaration — model layer POJO representing the ITEM_DETAILS database table
package com.items.model;

// Timestamp is used to store the exact creation and last-update timestamps from the DB
import java.sql.Timestamp;

/**
 * Represents a one-to-one detail record for an inventory item from the ITEM_DETAILS table.
 * Relationship: one Item has exactly one ItemDetails (though in the DB this is enforced
 * as a regular FK, not a UNIQUE constraint — the application treats it as one-to-one).
 *
 * Database columns:
 *   ID          NUMBER(10) PK          — unique detail record ID
 *   ITEM_ID     NUMBER(10) FK → ITEMS  — references the parent item
 *   DESCRIPTION VARCHAR2(500) NOT NULL — the actual detail text (min 3 chars)
 *   CREATED_AT  TIMESTAMP              — when this detail record was created
 *   UPDATED_AT  TIMESTAMP              — when this detail record was last updated
 */
public class ItemDetails {

    // Primary key — unique ID for this detail record
    private long id;
    // Foreign key — points to the ITEMS table (which item this detail belongs to)
    private long itemId;
    // The detail text content — must be at least 3 characters (validated client + DB)
    private String description;
    // Timestamp of when this record was first created (set by DB default)
    private Timestamp createdAt;
    // Timestamp of when this record was last updated (updated on every modification)
    private Timestamp updatedAt;

    /** Default constructor — required for frameworks that use reflection. */
    public ItemDetails() {}

    /**
     * Constructor without an ID — used when creating a NEW detail record (INSERT).
     *
     * @param itemId      the ID of the parent item
     * @param description the detail description text
     */
    public ItemDetails(long itemId, String description) {
        this.itemId = itemId;
        this.description = description;
    }

    /**
     * Full constructor with ID — used when updating or reading an existing detail.
     *
     * @param id          the detail record's primary key
     * @param itemId      the ID of the parent item
     * @param description the detail description text
     */
    public ItemDetails(long id, long itemId, String description) {
        this.id = id;
        this.itemId = itemId;
        this.description = description;
    }

    // --- Getters and setters ---
    // Standard Java Bean conventions — every field has a getter and setter.

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Debug-friendly string representation showing the key fields.
     * Excludes updatedAt to keep the output concise.
     */
    @Override
    public String toString() {
        return "ItemDetails{" +
                "id=" + id +
                ", itemId=" + itemId +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
