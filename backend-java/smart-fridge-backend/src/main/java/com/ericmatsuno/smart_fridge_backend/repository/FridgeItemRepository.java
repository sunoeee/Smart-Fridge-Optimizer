package com.ericmatsuno.smart_fridge_backend.repository;

import com.ericmatsuno.smart_fridge_backend.model.FridgeItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface FridgeItemRepository extends JpaRepository<FridgeItem, Integer> {

    // @EntityGraph loads each item's ingredient in the SAME query (a JOIN), instead of
    // one extra query per item later (the "N+1 query problem").
    @EntityGraph(attributePaths = "ingredient")
    List<FridgeItem> findAllByOrderByExpirationDateAsc();

    @EntityGraph(attributePaths = "ingredient")
    List<FridgeItem> findByExpirationDateLessThanEqualOrderByExpirationDateAsc(LocalDate date);

    /** Everything that is still usable today. Used for recipe suggestions. */
    @EntityGraph(attributePaths = "ingredient")
    List<FridgeItem> findByExpirationDateGreaterThanEqual(LocalDate today);

    /**
     * Usable stock for the given ingredients, oldest-expiring first (so we use up food
     * that is about to go bad before fresher food: FEFO, "first expired, first out").
     *
     * <p>PESSIMISTIC_WRITE makes this a {@code SELECT ... FOR UPDATE}: if two requests
     * try to cook at the same time, the second waits for the first to finish, so the
     * same eggs can't be used twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select f from FridgeItem f
            where f.ingredient.id in :ingredientIds
              and f.expirationDate >= :today
            order by f.expirationDate asc, f.id asc
            """)
    List<FridgeItem> lockUsableStock(@Param("ingredientIds") Collection<Integer> ingredientIds,
                                     @Param("today") LocalDate today);
}
