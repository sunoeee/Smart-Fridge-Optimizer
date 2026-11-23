package com.ericmatsuno.smart_fridge_backend.service;

import com.ericmatsuno.smart_fridge_backend.model.Ingredient;
import com.ericmatsuno.smart_fridge_backend.model.StorageType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: no Spring, no database, runs in milliseconds. */
class ShelfLifeCalculatorTest {

    private final ShelfLifeCalculator calc = new ShelfLifeCalculator();

    private final Ingredient spinach = new Ingredient("Spinach", "Produce", StorageType.refrigerated, 7);
    private final Ingredient chicken = new Ingredient("Chicken Breast", "Meat", StorageType.freezer, 90);
    private final Ingredient garlic = new Ingredient("Garlic", "Produce", StorageType.pantry, 60);

    @Test
    void usualStorageUsesDefaultShelfLife() {
        LocalDate bought = LocalDate.of(2026, 3, 1);
        assertThat(calc.estimateExpiration(spinach, StorageType.refrigerated, bought))
                .isEqualTo(LocalDate.of(2026, 3, 8));
        assertThat(calc.estimateShelfLifeDays(spinach, null)).isEqualTo(7);
    }

    @Test
    void freezingExtendsShelfLife() {
        assertThat(calc.estimateShelfLifeDays(spinach, StorageType.freezer)).isEqualTo(21);
    }

    @Test
    void normallyFrozenFoodKeptWarmerIsCappedAtTwoDays() {
        assertThat(calc.estimateShelfLifeDays(chicken, StorageType.refrigerated)).isEqualTo(2);
    }

    @Test
    void fridgeFoodLeftInPantrySpoilsFaster() {
        assertThat(calc.estimateShelfLifeDays(spinach, StorageType.pantry)).isEqualTo(1); // 7/4 = 1
    }

    @Test
    void pantryFoodInFridgeKeepsDefault() {
        assertThat(calc.estimateShelfLifeDays(garlic, StorageType.refrigerated)).isEqualTo(60);
    }
}
