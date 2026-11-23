package com.ericmatsuno.smart_fridge_backend.web.dto;

import com.ericmatsuno.smart_fridge_backend.model.Ingredient;
import com.ericmatsuno.smart_fridge_backend.model.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTOs (Data Transfer Objects) = the JSON shapes the API accepts and returns.
 * They are Java records: immutable, with equals/hashCode/toString generated.
 * Keeping them separate from the @Entity classes means the database layout and the
 * public API can change independently, and clients can't set fields like "id".
 */
public final class IngredientDtos {

    private IngredientDtos() {
    }

    public record CreateIngredientRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 50) String category,
            @NotNull StorageType defaultStorageType,
            @NotNull @Positive Integer defaultShelfLifeDays) {
    }

    public record IngredientResponse(
            Integer id, String name, String category,
            StorageType defaultStorageType, int defaultShelfLifeDays) {

        public static IngredientResponse from(Ingredient i) {
            return new IngredientResponse(i.getId(), i.getName(), i.getCategory(),
                    i.getDefaultStorageType(), i.getDefaultShelfLifeDays());
        }
    }
}
