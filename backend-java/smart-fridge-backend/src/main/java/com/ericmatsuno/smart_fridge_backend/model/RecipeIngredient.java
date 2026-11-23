package com.ericmatsuno.smart_fridge_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One line of a recipe ("3 pieces of Eggs"). The join table between recipes and
 * ingredients is its own entity (not a plain @ManyToMany) because it carries extra
 * data: the quantity and unit.
 */
@Entity
@Table(name = "recipe_ingredients")
@Getter
@Setter
@NoArgsConstructor
public class RecipeIngredient {

    @EmbeddedId
    private RecipeIngredientId id = new RecipeIngredientId();

    // @MapsId: fill the "recipeId" part of the composite key from this relationship,
    // so the id is never set by hand.
    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @MapsId("ingredientId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Column(name = "quantity_required", nullable = false, precision = 6, scale = 2)
    private BigDecimal quantityRequired;

    @Column(length = 20)
    private String unit;

    public RecipeIngredient(Ingredient ingredient, BigDecimal quantityRequired, String unit) {
        this.ingredient = ingredient;
        this.quantityRequired = quantityRequired;
        this.unit = unit;
    }
}
