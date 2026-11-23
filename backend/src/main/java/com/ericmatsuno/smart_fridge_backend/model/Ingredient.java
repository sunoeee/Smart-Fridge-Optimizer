package com.ericmatsuno.smart_fridge_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A catalog entry: what an ingredient is and how long it usually lasts. Maps to {@code ingredients}. */
@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
public class Ingredient {

    // IDENTITY = let Postgres's SERIAL column generate the id on insert.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    // NAMED_ENUM tells Hibernate the column is a real Postgres ENUM type, not VARCHAR.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "default_storage_type", nullable = false, columnDefinition = "storage_enum")
    private StorageType defaultStorageType;

    @Column(name = "default_shelf_life_days", nullable = false)
    private int defaultShelfLifeDays;

    public Ingredient(String name, String category, StorageType defaultStorageType, int defaultShelfLifeDays) {
        this.name = name;
        this.category = category;
        this.defaultStorageType = defaultStorageType;
        this.defaultShelfLifeDays = defaultShelfLifeDays;
    }
}
