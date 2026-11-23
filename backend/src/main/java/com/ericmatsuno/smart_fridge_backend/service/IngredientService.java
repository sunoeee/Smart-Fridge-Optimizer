package com.ericmatsuno.smart_fridge_backend.service;

import com.ericmatsuno.smart_fridge_backend.exception.ConflictException;
import com.ericmatsuno.smart_fridge_backend.exception.NotFoundException;
import com.ericmatsuno.smart_fridge_backend.model.Ingredient;
import com.ericmatsuno.smart_fridge_backend.repository.IngredientRepository;
import com.ericmatsuno.smart_fridge_backend.web.dto.IngredientDtos.CreateIngredientRequest;
import com.ericmatsuno.smart_fridge_backend.web.dto.IngredientDtos.IngredientResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for the ingredient catalog.
 *
 * <p>Class-level {@code @Transactional(readOnly = true)}: every method runs in a
 * read-only transaction by default; methods that write override it with plain
 * {@code @Transactional}.
 */
@Service
@Transactional(readOnly = true)
public class IngredientService {

    private final IngredientRepository ingredients;

    // Constructor injection: dependencies are final and visible, and the class can be
    // built with `new` in a unit test without starting Spring.
    public IngredientService(IngredientRepository ingredients) {
        this.ingredients = ingredients;
    }

    public List<IngredientResponse> list() {
        return ingredients.findAll(Sort.by("name")).stream().map(IngredientResponse::from).toList();
    }

    public IngredientResponse get(int id) {
        return IngredientResponse.from(find(id));
    }

    @Transactional
    public IngredientResponse create(CreateIngredientRequest req) {
        String name = req.name().trim();
        if (ingredients.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ingredient '" + name + "' already exists");
        }
        Ingredient saved = ingredients.save(new Ingredient(
                name, req.category(), req.defaultStorageType(), req.defaultShelfLifeDays()));
        return IngredientResponse.from(saved);
    }

    Ingredient find(int id) {
        return ingredients.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingredient " + id + " not found"));
    }
}
