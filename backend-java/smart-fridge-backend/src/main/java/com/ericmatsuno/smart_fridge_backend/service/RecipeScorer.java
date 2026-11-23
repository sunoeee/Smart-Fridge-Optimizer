package com.ericmatsuno.smart_fridge_backend.service;

import com.ericmatsuno.smart_fridge_backend.model.FridgeItem;
import com.ericmatsuno.smart_fridge_backend.model.Recipe;
import com.ericmatsuno.smart_fridge_backend.model.RecipeIngredient;
import com.ericmatsuno.smart_fridge_backend.model.Units;
import com.ericmatsuno.smart_fridge_backend.web.dto.MissingIngredient;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.RecipeSuggestion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ranks how good a recipe is to cook right now. Pure logic: no database access, so it
 * is easy to unit test and easy to replace later (e.g. with the Python AI service).
 *
 * <pre>
 *   score = 100 * coverage  +  25 * urgency
 *
 *   coverage = (ingredients you have enough of) / (ingredients in recipe)      0..1
 *   urgency  = sum over the recipe's ingredients that expire within N days
 *              of 1 / (daysLeft + 1)    -> expires today = 1, tomorrow = 0.5, ...
 * </pre>
 * Coverage dominates (you should be able to cook it), and urgency breaks ties in
 * favour of recipes that use food before it goes bad, which is the point of the app.
 */
@Component
public class RecipeScorer {

    static final double COVERAGE_WEIGHT = 100.0;
    static final double URGENCY_WEIGHT = 25.0;

    /**
     * @param stockByIngredient usable (non-expired) fridge items, grouped by ingredient id
     */
    public RecipeSuggestion score(Recipe recipe, Map<Integer, List<FridgeItem>> stockByIngredient,
                                  LocalDate today, int expiringSoonDays) {
        int total = recipe.getIngredients().size();
        int satisfied = 0;
        double urgency = 0;
        List<String> useSoon = new ArrayList<>();
        List<MissingIngredient> missing = new ArrayList<>();

        for (RecipeIngredient line : recipe.getIngredients()) {
            List<FridgeItem> matching = stockByIngredient.getOrDefault(line.getIngredient().getId(), List.of())
                    .stream()
                    .filter(f -> Units.compatible(f.getUnit(), line.getUnit()))
                    .toList();

            BigDecimal have = matching.stream().map(FridgeItem::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (have.compareTo(line.getQuantityRequired()) >= 0) {
                satisfied++;
            } else {
                missing.add(new MissingIngredient(line.getIngredient().getName(),
                        line.getQuantityRequired(), have, line.getUnit()));
            }

            // Days until the soonest-expiring matching batch goes bad.
            var soonest = matching.stream()
                    .map(f -> ChronoUnit.DAYS.between(today, f.getExpirationDate()))
                    .filter(daysLeft -> daysLeft >= 0)
                    .min(Long::compare);
            if (soonest.isPresent() && soonest.get() <= expiringSoonDays) {
                useSoon.add(line.getIngredient().getName());
                urgency += 1.0 / (soonest.get() + 1);
            }
        }

        double coverage = total == 0 ? 0 : (double) satisfied / total;
        double score = Math.round((COVERAGE_WEIGHT * coverage + URGENCY_WEIGHT * urgency) * 10) / 10.0;
        return new RecipeSuggestion(recipe.getId(), recipe.getTitle(), score,
                (int) Math.round(coverage * 100), total > 0 && missing.isEmpty(), useSoon, missing);
    }
}
