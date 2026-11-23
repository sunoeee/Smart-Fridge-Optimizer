package com.ericmatsuno.smart_fridge_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One batch of an ingredient the user actually has (e.g. "12 eggs bought on Nov 10"). Maps to {@code fridge_items}. */
@Entity
@Table(name = "fridge_items")
@Getter
@Setter
@NoArgsConstructor
public class FridgeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // LAZY: don't load the ingredient row unless we actually touch it.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    // BigDecimal, not double: DECIMAL(6,2) is exact, and 0.1 + 0.2 should equal 0.3.
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "storage_type", columnDefinition = "storage_enum")
    private StorageType storageType;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    public boolean isExpired(LocalDate today) {
        return expirationDate.isBefore(today);
    }
}
