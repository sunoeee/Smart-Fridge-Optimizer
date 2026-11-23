package com.ericmatsuno.smart_fridge_backend.repository;

import com.ericmatsuno.smart_fridge_backend.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data generates the implementation at startup. The SQL for
 * findByNameIgnoreCase is derived from the method name itself.
 */
public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {

    Optional<Ingredient> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
