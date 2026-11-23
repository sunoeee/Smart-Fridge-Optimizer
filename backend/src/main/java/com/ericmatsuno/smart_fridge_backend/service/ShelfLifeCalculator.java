package com.ericmatsuno.smart_fridge_backend.service;

import com.ericmatsuno.smart_fridge_backend.model.Ingredient;
import com.ericmatsuno.smart_fridge_backend.model.StorageType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Estimates an expiration date when the user doesn't type one in.
 *
 * <p>Base rule: purchase date + the ingredient's default shelf life. If the user stores
 * the item somewhere other than its usual place, we adjust:
 * <ul>
 *   <li>Frozen when it's normally not frozen: freezing slows spoilage, so x3.</li>
 *   <li>Normally frozen but kept in the fridge/pantry (e.g. thawed chicken): at most
 *       {@value #THAWED_MAX_DAYS} days, the usual food-safety guidance for thawed meat.</li>
 *   <li>Normally refrigerated but left in the pantry: a quarter of the time, at least 1 day.</li>
 *   <li>Normally pantry but put in the fridge: keep the default (being colder doesn't hurt,
 *       and we have no data that it helps).</li>
 * </ul>
 *
 * <p>These numbers are a rough heuristic. The rule lives in this one class so it can be
 * swapped later for real per-storage shelf-life data (e.g. a table keyed by
 * (ingredient, storage_type)) without touching the rest of the code.
 */
@Component
public class ShelfLifeCalculator {

    static final int FREEZER_MULTIPLIER = 3;
    static final int THAWED_MAX_DAYS = 2;
    static final int WARMER_DIVISOR = 4;

    public LocalDate estimateExpiration(Ingredient ingredient, StorageType actualStorage, LocalDate purchaseDate) {
        return purchaseDate.plusDays(estimateShelfLifeDays(ingredient, actualStorage));
    }

    public int estimateShelfLifeDays(Ingredient ingredient, StorageType actualStorage) {
        int base = ingredient.getDefaultShelfLifeDays();
        StorageType usual = ingredient.getDefaultStorageType();

        if (actualStorage == null || actualStorage == usual) {
            return base;
        }
        if (actualStorage == StorageType.freezer) {
            return base * FREEZER_MULTIPLIER;
        }
        if (usual == StorageType.freezer) {
            return Math.min(base, THAWED_MAX_DAYS);
        }
        if (actualStorage.isColderThan(usual)) {
            return base;
        }
        return Math.max(1, base / WARMER_DIVISOR);
    }
}
