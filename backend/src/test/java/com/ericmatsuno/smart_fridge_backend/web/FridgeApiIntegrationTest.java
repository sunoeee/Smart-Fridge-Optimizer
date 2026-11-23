package com.ericmatsuno.smart_fridge_backend.web;

import com.ericmatsuno.smart_fridge_backend.TestcontainersConfiguration;
import com.ericmatsuno.smart_fridge_backend.repository.FridgeItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through every layer: HTTP -> controller -> service -> JPA -> real Postgres
 * (in Docker). Uses the seed data from the V2 migration (Spinach Omelette etc.).
 * "Today" is frozen at 2026-03-01 by TestcontainersConfiguration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FridgeApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    FridgeItemRepository fridgeItems;

    @BeforeEach
    void emptyFridge() {
        fridgeItems.deleteAll(); // each test starts from an empty kitchen
    }

    @Test
    void addingWithoutExpirationEstimatesIt() throws Exception {
        add("""
                {"ingredientName": "spinach", "quantity": 2, "unit": "cups"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredientName").value("Spinach"))
                .andExpect(jsonPath("$.storageType").value("refrigerated"))
                .andExpect(jsonPath("$.purchaseDate").value("2026-03-01"))
                .andExpect(jsonPath("$.expirationDate").value("2026-03-08")); // +7 day default
    }

    @Test
    void suggestionsPreferRecipeThatUsesFoodExpiringSoon() throws Exception {
        stockOmelette("2026-03-02");          // spinach expires tomorrow
        add("""
                {"ingredientName": "Chicken Breast", "quantity": 1, "unit": "pieces"}""");

        mvc.perform(get("/api/recipes/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Spinach Omelette"))
                .andExpect(jsonPath("$[0].canMakeNow").value(true))
                .andExpect(jsonPath("$[0].useSoon", contains("Spinach")))
                .andExpect(jsonPath("$[1].title").value("Garlic Chicken Breast"));
    }

    @Test
    void cookingUsesOldestBatchFirst() throws Exception {
        stockOmelette("2026-03-05");
        add("""
                {"ingredientName": "Eggs", "quantity": 2, "unit": "pieces", "expirationDate": "2026-03-03"}""");

        mvc.perform(post("/api/recipes/1/cook")).andExpect(status().isOk());

        // Needed 3 eggs: the 2 older ones are used up (row deleted), then 1 of the 12 fresh ones.
        mvc.perform(get("/api/fridge"))
                .andExpect(jsonPath("$[?(@.ingredientName == 'Eggs')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.ingredientName == 'Eggs')].quantity", contains(11.0)));
    }

    @Test
    void cookingWithoutEnoughChangesNothing() throws Exception {
        add("""
                {"ingredientName": "Eggs", "quantity": 12, "unit": "pieces"}""");

        mvc.perform(post("/api/recipes/1/cook"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.missing[*].ingredientName",
                        contains("Cheddar Cheese", "Spinach", "Garlic", "Olive Oil")));

        // All-or-nothing: the eggs we DID have are untouched.
        mvc.perform(get("/api/fridge"))
                .andExpect(jsonPath("$[0].quantity").value(12.0));
    }

    @Test
    void invalidInputGetsFieldLevelErrors() throws Exception {
        add("""
                {"quantity": -1}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").exists())
                .andExpect(jsonPath("$.errors.ingredientIdentified").value("provide ingredientId or ingredientName"));
    }

    @Test
    void unknownRecipeIs404() throws Exception {
        mvc.perform(post("/api/recipes/9999/cook")).andExpect(status().isNotFound());
    }

    // ---- helpers ----

    private ResultActions add(String json) throws Exception {
        return mvc.perform(post("/api/fridge").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void stockOmelette(String spinachExpires) throws Exception {
        add("""
                {"ingredientName": "Eggs", "quantity": 12, "unit": "pieces"}""");
        add("""
                {"ingredientName": "Cheddar Cheese", "quantity": 2, "unit": "cups"}""");
        add("""
                {"ingredientName": "Spinach", "quantity": 3, "unit": "cups", "expirationDate": "%s"}"""
                .formatted(spinachExpires));
        add("""
                {"ingredientName": "Garlic", "quantity": 6, "unit": "cloves"}""");
        add("""
                {"ingredientName": "Olive Oil", "quantity": 20, "unit": "tbsp"}""");
    }
}
