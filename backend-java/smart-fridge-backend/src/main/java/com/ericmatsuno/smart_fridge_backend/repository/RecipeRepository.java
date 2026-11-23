package com.ericmatsuno.smart_fridge_backend.repository;

import com.ericmatsuno.smart_fridge_backend.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Integer> {

    // "join fetch" loads recipes + their lines + each line's ingredient in ONE query.
    // Without it, listing 50 recipes would run 1 + 50 + (lines) queries.
    @Query("""
            select distinct r from Recipe r
            left join fetch r.ingredients ri
            left join fetch ri.ingredient
            order by r.title
            """)
    List<Recipe> findAllWithIngredients();

    @Query("""
            select r from Recipe r
            left join fetch r.ingredients ri
            left join fetch ri.ingredient
            where r.id = :id
            """)
    Optional<Recipe> findByIdWithIngredients(@Param("id") Integer id);

    boolean existsByTitleIgnoreCase(String title);
}
