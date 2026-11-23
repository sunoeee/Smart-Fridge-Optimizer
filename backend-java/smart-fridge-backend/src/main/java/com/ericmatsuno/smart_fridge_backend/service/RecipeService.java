package com.ericmatsuno.smart_fridge_backend.service;

import com.ericmatsuno.smart_fridge_backend.config.FridgeProperties;
import com.ericmatsuno.smart_fridge_backend.exception.ConflictException;
import com.ericmatsuno.smart_fridge_backend.exception.InsufficientStockException;
import com.ericmatsuno.smart_fridge_backend.exception.NotFoundException;
import com.ericmatsuno.smart_fridge_backend.model.FridgeItem;
import com.ericmatsuno.smart_fridge_backend.model.Ingredient;
import com.ericmatsuno.smart_fridge_backend.model.Recipe;
import com.ericmatsuno.smart_fridge_backend.model.RecipeIngredient;
import com.ericmatsuno.smart_fridge_backend.model.Units;
import com.ericmatsuno.smart_fridge_backend.repository.FridgeItemRepository;
import com.ericmatsuno.smart_fridge_backend.repository.IngredientRepository;
import com.ericmatsuno.smart_fridge_backend.repository.RecipeRepository;
import com.ericmatsuno.smart_fridge_backend.web.dto.MissingIngredient;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.CookResponse;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.CookResponse.UsedIngredient;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.CreateRecipeRequest;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.RecipeResponse;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.RecipeSuggestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Recipes: browsing, creating, ranking suggestions, and "cooking" (which uses up fridge stock). */
@Service
@Transactional(readOnly = true)
public class RecipeService {

    private final RecipeRepository recipes;
    private final IngredientRepository ingredients;
    private final FridgeItemRepository fridgeItems;
    private final RecipeScorer scorer;
    private final FridgeProperties props;
    private final Clock clock;

    public RecipeService(RecipeRepository recipes, IngredientRepository ingredients,
                         FridgeItemRepository fridgeItems, RecipeScorer scorer,
                         FridgeProperties props, Clock clock) {
        this.recipes = recipes;
        this.ingredients = ingredients;
        this.fridgeItems = fridgeItems;
        this.scorer = scorer;
        this.props = props;
        this.clock = clock;
    }

    public List<RecipeResponse> list() {
        return recipes.findAllWithIngredients().stream().map(RecipeResponse::from).toList();
    }

    public RecipeResponse get(int id) {
        return RecipeResponse.from(find(id));
    }

    @Transactional
    public RecipeResponse create(CreateRecipeRequest req) {
        String title = req.title().trim();
        if (recipes.existsByTitleIgnoreCase(title)) {
            throw new ConflictException("Recipe '" + title + "' already exists");
        }
        Recipe recipe = new Recipe(title, req.instructions());
        Set<Integer> seen = new HashSet<>();
        for (CreateRecipeRequest.Line line : req.ingredients()) {
            if (!seen.add(line.ingredientId())) {
                // (recipe_id, ingredient_id) is the primary key, so each ingredient may appear once.
                throw new IllegalArgumentException("Ingredient " + line.ingredientId() + " is listed twice");
            }
            Ingredient ingredient = ingredients.findById(line.ingredientId())
                    .orElseThrow(() -> new NotFoundException("Ingredient " + line.ingredientId() + " not found"));
            recipe.addIngredient(new RecipeIngredient(ingredient, line.quantity(), line.unit()));
        }
        return RecipeResponse.from(recipes.save(recipe));
    }

    /**
     * Ranks every recipe against what's in the fridge right now. Loads all recipes and all
     * usable stock in 2 queries and scores in memory. That's fine for a personal kitchen
     * (tens of recipes); with thousands we'd push the matching into SQL instead.
     */
    public List<RecipeSuggestion> suggest(int limit) {
        LocalDate today = LocalDate.now(clock);
        Map<Integer, List<FridgeItem>> stock = fridgeItems.findByExpirationDateGreaterThanEqual(today).stream()
                .collect(Collectors.groupingBy(f -> f.getIngredient().getId()));

        return recipes.findAllWithIngredients().stream()
                .map(r -> scorer.score(r, stock, today, props.expiringSoonDays()))
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingDouble(RecipeSuggestion::score).reversed()
                        .thenComparing(RecipeSuggestion::title))
                .limit(limit)
                .toList();
    }

    /**
     * "I cooked this": subtracts every ingredient from the fridge, oldest-expiring batch
     * first. It is all-or-nothing: we check every ingredient BEFORE changing anything, and
     * the whole method is one transaction, so a failure can never leave half the
     * ingredients subtracted.
     */
    @Transactional
    public CookResponse cook(int recipeId) {
        LocalDate today = LocalDate.now(clock);
        Recipe recipe = find(recipeId);
        List<Integer> ingredientIds = recipe.getIngredients().stream().map(ri -> ri.getIngredient().getId()).toList();
        Map<Integer, List<FridgeItem>> stock = fridgeItems.lockUsableStock(ingredientIds, today).stream()
                .collect(Collectors.groupingBy(f -> f.getIngredient().getId()));

        // Pass 1: check everything.
        List<MissingIngredient> missing = new ArrayList<>();
        for (RecipeIngredient line : recipe.getIngredients()) {
            BigDecimal have = matchingBatches(line, stock).stream()
                    .map(FridgeItem::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (have.compareTo(line.getQuantityRequired()) < 0) {
                missing.add(new MissingIngredient(line.getIngredient().getName(),
                        line.getQuantityRequired(), have, line.getUnit()));
            }
        }
        if (!missing.isEmpty()) {
            throw new InsufficientStockException(recipe.getTitle(), missing);
        }

        // Pass 2: subtract, oldest batch first, deleting batches that hit zero.
        List<UsedIngredient> used = new ArrayList<>();
        for (RecipeIngredient line : recipe.getIngredients()) {
            BigDecimal stillNeeded = line.getQuantityRequired();
            for (FridgeItem batch : matchingBatches(line, stock)) {
                if (stillNeeded.signum() == 0) {
                    break;
                }
                BigDecimal take = batch.getQuantity().min(stillNeeded);
                batch.setQuantity(batch.getQuantity().subtract(take));
                stillNeeded = stillNeeded.subtract(take);
                if (batch.getQuantity().signum() == 0) {
                    fridgeItems.delete(batch);
                }
            }
            used.add(new UsedIngredient(line.getIngredient().getName(), line.getQuantityRequired(), line.getUnit()));
        }
        return new CookResponse(recipe.getId(), recipe.getTitle(), used);
    }

    /** Batches of this line's ingredient in a compatible unit, already sorted oldest-expiring first. */
    private static List<FridgeItem> matchingBatches(RecipeIngredient line, Map<Integer, List<FridgeItem>> stock) {
        return stock.getOrDefault(line.getIngredient().getId(), List.of()).stream()
                .filter(f -> Units.compatible(f.getUnit(), line.getUnit()))
                .toList();
    }

    private Recipe find(int id) {
        return recipes.findByIdWithIngredients(id)
                .orElseThrow(() -> new NotFoundException("Recipe " + id + " not found"));
    }
}
