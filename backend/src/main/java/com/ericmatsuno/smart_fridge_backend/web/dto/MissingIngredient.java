package com.ericmatsuno.smart_fridge_backend.web.dto;

import java.math.BigDecimal;

/** "Recipe needs 3 pieces of Eggs, you have 1." */
public record MissingIngredient(String ingredientName, BigDecimal required, BigDecimal available, String unit) {
}
