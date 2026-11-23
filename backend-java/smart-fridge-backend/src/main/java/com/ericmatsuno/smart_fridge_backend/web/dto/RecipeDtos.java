package com.ericmatsuno.smart_fridge_backend.web.dto;

import com.ericmatsuno.smart_fridge_backend.model.Recipe;
import com.ericmatsuno.smart_fridge_backend.model.RecipeIngredient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class RecipeDtos {

    private RecipeDtos() {
    }

    public record CreateRecipeRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank String instructions,
            @NotEmpty List<@Valid Line> ingredients) {

        public record Line(
                @NotNull Integer ingredientId,
                @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal quantity,
                @Size(max = 20) String unit) {
        }
    }

    public record RecipeResponse(Integer id, String title, String instructions, List<LineResponse> ingredients) {

        public record LineResponse(Integer ingredientId, String ingredientName, BigDecimal quantity, String unit) {
            static LineResponse from(RecipeIngredient ri) {
                return new LineResponse(ri.getIngredient().getId(), ri.getIngredient().getName(),
                        ri.getQuantityRequired(), ri.getUnit());
            }
        }

        public static RecipeResponse from(Recipe r) {
            List<LineResponse> lines = r.getIngredients().stream()
                    .map(LineResponse::from)
                    .sorted(Comparator.comparing(LineResponse::ingredientName))
                    .toList();
            return new RecipeResponse(r.getId(), r.getTitle(), r.getInstructions(), lines);
        }
    }

    /**
     * One ranked suggestion.
     *
     * @param score           higher = better pick right now (see RecipeScorer for the formula)
     * @param coveragePercent share of the recipe's ingredients you have enough of
     * @param useSoon         ingredients in this recipe that are about to expire
     * @param missing         what you'd still need to buy
     */
    public record RecipeSuggestion(
            Integer recipeId, String title, double score, int coveragePercent, boolean canMakeNow,
            List<String> useSoon, List<MissingIngredient> missing) {
    }

    /** What cooking a recipe took out of the fridge. */
    public record CookResponse(Integer recipeId, String title, List<UsedIngredient> used) {
        public record UsedIngredient(String ingredientName, BigDecimal quantity, String unit) {
        }
    }
}
