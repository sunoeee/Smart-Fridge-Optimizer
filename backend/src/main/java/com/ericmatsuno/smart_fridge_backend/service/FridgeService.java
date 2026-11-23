package com.ericmatsuno.smart_fridge_backend.service;

import com.ericmatsuno.smart_fridge_backend.exception.ConflictException;
import com.ericmatsuno.smart_fridge_backend.exception.NotFoundException;
import com.ericmatsuno.smart_fridge_backend.model.FridgeItem;
import com.ericmatsuno.smart_fridge_backend.model.Ingredient;
import com.ericmatsuno.smart_fridge_backend.model.StorageType;
import com.ericmatsuno.smart_fridge_backend.repository.FridgeItemRepository;
import com.ericmatsuno.smart_fridge_backend.repository.IngredientRepository;
import com.ericmatsuno.smart_fridge_backend.web.dto.FridgeDtos.AddFridgeItemRequest;
import com.ericmatsuno.smart_fridge_backend.web.dto.FridgeDtos.FridgeItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Business logic for what's in the kitchen: adding groceries, using them, throwing them out. */
@Service
@Transactional(readOnly = true)
public class FridgeService {

    private final FridgeItemRepository fridgeItems;
    private final IngredientRepository ingredients;
    private final ShelfLifeCalculator shelfLife;
    private final Clock clock;

    public FridgeService(FridgeItemRepository fridgeItems, IngredientRepository ingredients,
                         ShelfLifeCalculator shelfLife, Clock clock) {
        this.fridgeItems = fridgeItems;
        this.ingredients = ingredients;
        this.shelfLife = shelfLife;
        this.clock = clock;
    }

    /** Everything, soonest-to-expire first; or only what expires within N days (expired items included). */
    public List<FridgeItemResponse> list(Integer expiringWithinDays) {
        LocalDate today = LocalDate.now(clock);
        List<FridgeItem> items = (expiringWithinDays == null)
                ? fridgeItems.findAllByOrderByExpirationDateAsc()
                : fridgeItems.findByExpirationDateLessThanEqualOrderByExpirationDateAsc(today.plusDays(expiringWithinDays));
        return items.stream().map(f -> FridgeItemResponse.from(f, today)).toList();
    }

    @Transactional
    public FridgeItemResponse add(AddFridgeItemRequest req) {
        LocalDate today = LocalDate.now(clock);
        Ingredient ingredient = resolveIngredient(req);

        StorageType storage = req.storageType() != null ? req.storageType() : ingredient.getDefaultStorageType();
        LocalDate purchased = req.purchaseDate() != null ? req.purchaseDate() : today;
        LocalDate expires = req.expirationDate() != null
                ? req.expirationDate()
                : shelfLife.estimateExpiration(ingredient, storage, purchased);

        if (expires.isBefore(purchased)) {
            throw new IllegalArgumentException("expirationDate can't be before purchaseDate");
        }

        FridgeItem item = new FridgeItem();
        item.setIngredient(ingredient);
        item.setQuantity(req.quantity());
        item.setUnit(req.unit());
        item.setStorageType(storage);
        item.setPurchaseDate(purchased);
        item.setExpirationDate(expires);
        return FridgeItemResponse.from(fridgeItems.save(item), today);
    }

    /**
     * Use part of one batch. Returns the updated item, or empty if it was used up
     * (the row is deleted so "0 eggs" doesn't clutter the list).
     */
    @Transactional
    public Optional<FridgeItemResponse> consume(int id, BigDecimal amount) {
        FridgeItem item = find(id);
        BigDecimal remaining = item.getQuantity().subtract(amount);
        // compareTo, not equals: for BigDecimal, 1.0.equals(1.00) is false because the scale differs.
        if (remaining.signum() < 0) {
            throw new ConflictException("Only " + item.getQuantity() + " left of "
                    + item.getIngredient().getName() + ", can't use " + amount);
        }
        if (remaining.signum() == 0) {
            fridgeItems.delete(item);
            return Optional.empty();
        }
        item.setQuantity(remaining); // no save() needed: JPA writes changes to managed entities on commit
        return Optional.of(FridgeItemResponse.from(item, LocalDate.now(clock)));
    }

    @Transactional
    public void remove(int id) {
        fridgeItems.delete(find(id));
    }

    private FridgeItem find(int id) {
        return fridgeItems.findById(id)
                .orElseThrow(() -> new NotFoundException("Fridge item " + id + " not found"));
    }

    private Ingredient resolveIngredient(AddFridgeItemRequest req) {
        if (req.ingredientId() != null) {
            return ingredients.findById(req.ingredientId())
                    .orElseThrow(() -> new NotFoundException("Ingredient " + req.ingredientId() + " not found"));
        }
        String name = req.ingredientName().trim();
        return ingredients.findByNameIgnoreCase(name)
                .orElseThrow(() -> new NotFoundException(
                        "Unknown ingredient '" + name + "'. Add it with POST /api/ingredients first."));
    }
}
