package com.ericmatsuno.smart_fridge_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for {@code recipe_ingredients}: (recipe_id, ingredient_id).
 * JPA requires composite keys to implement equals/hashCode and Serializable.
 */
@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class RecipeIngredientId implements Serializable {

    @Column(name = "recipe_id")
    private Integer recipeId;

    @Column(name = "ingredient_id")
    private Integer ingredientId;
}
