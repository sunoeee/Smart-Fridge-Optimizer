-- V3: schema fixes found while building the backend.
--
-- 1. A fridge item with no ingredient is meaningless, but V1 allowed NULL.
ALTER TABLE fridge_items ALTER COLUMN ingredient_id SET NOT NULL;

-- 2. Quantities can't go negative (consuming more than you have is an error).
ALTER TABLE fridge_items ADD CONSTRAINT chk_fridge_quantity_nonneg CHECK (quantity >= 0);
ALTER TABLE recipe_ingredients ADD CONSTRAINT chk_recipe_quantity_pos CHECK (quantity_required > 0);

-- 3. Recipes had quantities with no unit ("0.50" of cheese - cups? grams?),
--    so the backend could not compare them with what is in the fridge.
--    The units below come from the comments in the original seed script.
ALTER TABLE recipe_ingredients ADD COLUMN unit VARCHAR(20);

UPDATE recipe_ingredients ri
SET unit = v.unit
FROM (VALUES
    ('Eggs', 'pieces'), ('Cheddar Cheese', 'cups'), ('Spinach', 'cups'),
    ('Garlic', 'cloves'), ('Olive Oil', 'tbsp'), ('Chicken Breast', 'pieces')
) AS v(ingredient_name, unit)
JOIN ingredients i ON i.name = v.ingredient_name
WHERE ri.ingredient_id = i.id AND ri.unit IS NULL;

-- 4. Index for "what do I have of ingredient X", used when cooking a recipe.
CREATE INDEX idx_fridge_ingredient ON fridge_items(ingredient_id);
