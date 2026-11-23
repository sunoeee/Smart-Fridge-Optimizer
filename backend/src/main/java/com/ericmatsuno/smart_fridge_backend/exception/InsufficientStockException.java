package com.ericmatsuno.smart_fridge_backend.exception;

import com.ericmatsuno.smart_fridge_backend.web.dto.MissingIngredient;

import java.util.List;

/** Thrown when cooking a recipe needs more than the fridge has. Lists every shortfall, not just the first. */
public class InsufficientStockException extends ConflictException {

    private final List<MissingIngredient> missing;

    public InsufficientStockException(String recipeTitle, List<MissingIngredient> missing) {
        super("Not enough ingredients to cook '" + recipeTitle + "'");
        this.missing = List.copyOf(missing);
    }

    public List<MissingIngredient> getMissing() {
        return missing;
    }
}
