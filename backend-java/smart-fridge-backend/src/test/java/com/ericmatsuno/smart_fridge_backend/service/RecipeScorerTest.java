package com.ericmatsuno.smart_fridge_backend.service;

import com.ericmatsuno.smart_fridge_backend.model.FridgeItem;
import com.ericmatsuno.smart_fridge_backend.model.Ingredient;
import com.ericmatsuno.smart_fridge_backend.model.Recipe;
import com.ericmatsuno.smart_fridge_backend.model.RecipeIngredient;
import com.ericmatsuno.smart_fridge_backend.model.StorageType;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.RecipeSuggestion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeScorerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);
    private final RecipeScorer scorer = new RecipeScorer();

    private final Ingredient eggs = ingredient(1, "Eggs");
    private final Ingredient spinach = ingredient(2, "Spinach");

    private final Recipe omelette = recipe("Omelette",
            line(eggs, "3", "pieces"),
            line(spinach, "1", "cups"));

    @Test
    void everythingInStockAndNothingExpiringScoresExactly100() {
        var stock = Map.of(
                1, List.of(item(eggs, "6", "pieces", 20)),
                2, List.of(item(spinach, "2", "cup", 10))); // "cup" vs "cups" still matches

        RecipeSuggestion s = scorer.score(omelette, stock, TODAY, 3);

        assertThat(s.canMakeNow()).isTrue();
        assertThat(s.coveragePercent()).isEqualTo(100);
        assertThat(s.score()).isEqualTo(100.0);
        assertThat(s.useSoon()).isEmpty();
    }

    @Test
    void ingredientExpiringTomorrowAddsUrgencyBonus() {
        var stock = Map.of(
                1, List.of(item(eggs, "6", "pieces", 20)),
                2, List.of(item(spinach, "2", "cups", 1)));

        RecipeSuggestion s = scorer.score(omelette, stock, TODAY, 3);

        // 100 * 1.0 coverage + 25 * (1 / (1 + 1)) urgency = 112.5
        assertThat(s.score()).isEqualTo(112.5);
        assertThat(s.useSoon()).containsExactly("Spinach");
    }

    @Test
    void notEnoughIsReportedAsMissing() {
        var stock = Map.of(1, List.of(item(eggs, "1", "pieces", 20)));

        RecipeSuggestion s = scorer.score(omelette, stock, TODAY, 3);

        assertThat(s.canMakeNow()).isFalse();
        assertThat(s.coveragePercent()).isZero();
        assertThat(s.missing()).extracting("ingredientName").containsExactly("Eggs", "Spinach");
        assertThat(s.missing().get(0).available()).isEqualByComparingTo("1");
    }

    @Test
    void differentUnitsAreNotAddedTogether() {
        var stock = Map.of(
                1, List.of(item(eggs, "6", "pieces", 20)),
                2, List.of(item(spinach, "500", "grams", 5))); // recipe wants cups

        RecipeSuggestion s = scorer.score(omelette, stock, TODAY, 3);

        assertThat(s.coveragePercent()).isEqualTo(50);
        assertThat(s.missing()).extracting("ingredientName").containsExactly("Spinach");
    }

    // ---- tiny builders so each test reads like a scenario ----

    private static Ingredient ingredient(int id, String name) {
        Ingredient i = new Ingredient(name, null, StorageType.refrigerated, 7);
        i.setId(id);
        return i;
    }

    private static RecipeIngredient line(Ingredient i, String qty, String unit) {
        return new RecipeIngredient(i, new BigDecimal(qty), unit);
    }

    private static Recipe recipe(String title, RecipeIngredient... lines) {
        Recipe r = new Recipe(title, "...");
        for (RecipeIngredient l : lines) {
            r.addIngredient(l);
        }
        return r;
    }

    private static FridgeItem item(Ingredient i, String qty, String unit, int daysLeft) {
        FridgeItem f = new FridgeItem();
        f.setIngredient(i);
        f.setQuantity(new BigDecimal(qty));
        f.setUnit(unit);
        f.setExpirationDate(TODAY.plusDays(daysLeft));
        return f;
    }
}
