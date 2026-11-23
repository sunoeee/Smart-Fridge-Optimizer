package com.ericmatsuno.smart_fridge_backend.model;

/**
 * Mirrors the Postgres type {@code storage_enum}.
 *
 * <p>The constants are lowercase on purpose: Hibernate writes {@code name()} straight
 * into the Postgres enum column, so the Java names must match the SQL labels exactly
 * ('refrigerated', 'pantry', 'freezer').
 *
 * <p>{@code coldness} lets us compare storage types ("is this colder than the default?")
 * without a chain of if-statements.
 */
public enum StorageType {
    pantry(0),
    refrigerated(1),
    freezer(2);

    private final int coldness;

    StorageType(int coldness) {
        this.coldness = coldness;
    }

    public boolean isColderThan(StorageType other) {
        return this.coldness > other.coldness;
    }
}
