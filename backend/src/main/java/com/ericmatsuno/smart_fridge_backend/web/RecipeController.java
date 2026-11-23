package com.ericmatsuno.smart_fridge_backend.web;

import com.ericmatsuno.smart_fridge_backend.service.RecipeService;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.CookResponse;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.CreateRecipeRequest;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.RecipeResponse;
import com.ericmatsuno.smart_fridge_backend.web.dto.RecipeDtos.RecipeSuggestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecipeResponse> list() {
        return service.list();
    }

    // Declared before "/{id}" for readability; Spring would match the literal path first anyway.
    @GetMapping("/suggestions")
    public List<RecipeSuggestion> suggestions(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
        return service.suggest(limit);
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@PathVariable int id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody CreateRecipeRequest req) {
        RecipeResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/recipes/" + created.id())).body(created);
    }

    /** "I made this" -> subtracts the ingredients from the fridge. 409 + a list of shortfalls if you can't. */
    @PostMapping("/{id}/cook")
    public CookResponse cook(@PathVariable int id) {
        return service.cook(id);
    }
}
