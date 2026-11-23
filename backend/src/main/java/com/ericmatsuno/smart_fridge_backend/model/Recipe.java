package com.ericmatsuno.smart_fridge_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Maps to {@code recipes}. Owns its list of {@link RecipeIngredient} rows. */
@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String instructions;

    // cascade + orphanRemoval: saving/deleting a recipe saves/deletes its ingredient lines,
    // because a recipe line can't exist without its recipe.
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe(String title, String instructions) {
        this.title = title;
        this.instructions = instructions;
    }

    /** Keeps both sides of the relationship in sync. */
    public void addIngredient(RecipeIngredient line) {
        line.setRecipe(this);
        ingredients.add(line);
    }
}
