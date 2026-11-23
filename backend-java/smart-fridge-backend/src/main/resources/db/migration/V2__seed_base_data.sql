-- V2: starter data (originally BaseData.sql).
--
-- Bugs fixed from the original script:
--   1. The VALUES list was wrapped in an extra pair of parentheses:
--        VALUES ( (...), (...) )
--      Postgres reads that as ONE row whose columns are themselves rows, so the
--      insert fails ("INSERT has more target columns than expressions").
--      Correct form is VALUES (...), (...);
--   2. The semicolon was INSIDE the closing parenthesis "...365);\n)", which ends
--      the statement early and leaves a dangling ")" that is a syntax error.
--   3. recipe_ingredients used hard-coded ids (1, 2, ...). That only works if the
--      SERIAL sequences happen to start at 1, which is not true after a failed or
--      repeated run. Looking ids up by name makes the script order-independent.
--
-- ON CONFLICT DO NOTHING makes the script safe to run more than once.

INSERT INTO ingredients (name, category, default_storage_type, default_shelf_life_days) VALUES
    ('Eggs',           'Dairy/Eggs', 'refrigerated', 21),
    ('Cheddar Cheese', 'Dairy/Eggs', 'refrigerated', 30),
    ('Spinach',        'Produce',    'refrigerated', 7),
    ('Chicken Breast', 'Meat',       'freezer',      90),
    ('Garlic',         'Produce',    'pantry',       60),
    ('Olive Oil',      'Pantry',     'pantry',       365)
ON CONFLICT (name) DO NOTHING;

INSERT INTO recipes (title, instructions) VALUES
    ('Spinach Omelette',
     'Whisk eggs. Sauté spinach and garlic in olive oil. Pour eggs over spinach, top with cheese, and fold.'),
    ('Garlic Chicken Breast',
     'Season chicken breast with garlic. Sear in olive oil over medium-high heat until cooked through.')
ON CONFLICT (title) DO NOTHING;

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity_required)
SELECT r.id, i.id, v.qty
FROM (VALUES
    ('Spinach Omelette',      'Eggs',           3.00), -- 3 eggs
    ('Spinach Omelette',      'Cheddar Cheese', 0.50), -- 0.5 cups
    ('Spinach Omelette',      'Spinach',        1.00), -- 1 cup
    ('Spinach Omelette',      'Garlic',         1.00), -- 1 clove
    ('Spinach Omelette',      'Olive Oil',      1.00), -- 1 tbsp
    ('Garlic Chicken Breast', 'Chicken Breast', 1.00), -- 1 breast
    ('Garlic Chicken Breast', 'Garlic',         2.00), -- 2 cloves
    ('Garlic Chicken Breast', 'Olive Oil',      1.00)  -- 1 tbsp
) AS v(recipe_title, ingredient_name, qty)
JOIN recipes r     ON r.title = v.recipe_title
JOIN ingredients i ON i.name  = v.ingredient_name
ON CONFLICT (recipe_id, ingredient_id) DO NOTHING;
