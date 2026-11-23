-- V1: initial schema (originally MainTableCreation.sql, written in DBeaver).
-- The DROP statements were removed: a migration only ever moves the schema
-- forward, and Flyway records that it ran, so it never needs to clean up first.

-- Custom ENUM type for storage types to ensure data integrity
CREATE TYPE storage_enum AS ENUM ('refrigerated', 'pantry', 'freezer');

-- Main table of ingredients (the "catalog": what an ingredient IS)
CREATE TABLE ingredients (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50), -- e.g. 'Produce', 'Dairy', 'Meat', 'Prepared Meal'
    default_storage_type storage_enum NOT NULL,
    default_shelf_life_days INT NOT NULL -- used if expiration date not given
);

-- Represents all of what a user has in their kitchen (the "inventory")
CREATE TABLE fridge_items (
    id SERIAL PRIMARY KEY,
    ingredient_id INT REFERENCES ingredients(id) ON DELETE CASCADE,
    quantity DECIMAL(6,2) NOT NULL,
    unit VARCHAR(20), -- e.g. 'grams', 'pieces', 'items'
    storage_type storage_enum, -- defaults to ingredients.default_storage_type
    purchase_date DATE DEFAULT CURRENT_DATE,
    expiration_date DATE NOT NULL
);

-- Master list of recipes
CREATE TABLE recipes (
    id SERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL UNIQUE,
    instructions TEXT NOT NULL
);

-- Links recipes to the ingredients required for them (many-to-many join table)
CREATE TABLE recipe_ingredients (
    recipe_id INT REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id INT REFERENCES ingredients(id) ON DELETE CASCADE,
    quantity_required DECIMAL(6,2) NOT NULL,
    PRIMARY KEY (recipe_id, ingredient_id)
);

-- Indexes for the queries we run most
CREATE INDEX idx_fridge_expiration ON fridge_items(expiration_date);
CREATE INDEX idx_recipe_ingredients_lookup ON recipe_ingredients(ingredient_id);
