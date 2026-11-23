package com.ericmatsuno.smart_fridge_backend.web.dto;

import com.ericmatsuno.smart_fridge_backend.model.FridgeItem;
import com.ericmatsuno.smart_fridge_backend.model.StorageType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class FridgeDtos {

    private FridgeDtos() {
    }

    /**
     * "I bought this." Identify the ingredient by id OR by name. Everything after
     * unit is optional: storage defaults to the ingredient's usual storage, purchase
     * date to today, and the expiration date is estimated if left out.
     */
    public record AddFridgeItemRequest(
            Integer ingredientId,
            @Size(max = 100) String ingredientName,
            // @Digits matches DECIMAL(6,2): at most 4 digits before the point, 2 after.
            @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal quantity,
            @Size(max = 20) String unit,
            StorageType storageType,
            LocalDate purchaseDate,
            LocalDate expirationDate) {

        @AssertTrue(message = "provide ingredientId or ingredientName")
        public boolean isIngredientIdentified() {
            return ingredientId != null || (ingredientName != null && !ingredientName.isBlank());
        }
    }

    /** "I used some of this." */
    public record ConsumeRequest(@NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal quantity) {
    }

    public record FridgeItemResponse(
            Integer id, Integer ingredientId, String ingredientName,
            BigDecimal quantity, String unit, StorageType storageType,
            LocalDate purchaseDate, LocalDate expirationDate,
            long daysUntilExpiration, boolean expired) {

        public static FridgeItemResponse from(FridgeItem f, LocalDate today) {
            long days = ChronoUnit.DAYS.between(today, f.getExpirationDate());
            return new FridgeItemResponse(f.getId(), f.getIngredient().getId(), f.getIngredient().getName(),
                    f.getQuantity(), f.getUnit(), f.getStorageType(),
                    f.getPurchaseDate(), f.getExpirationDate(), days, days < 0);
        }
    }
}
