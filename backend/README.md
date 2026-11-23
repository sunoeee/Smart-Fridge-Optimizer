# Smart Fridge Backend (Java / Spring Boot)

REST API that tracks what's in your kitchen, estimates when it expires, and suggests
recipes that use food before it goes bad.

**Stack:** Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate 7) · PostgreSQL 18 ·
Flyway · Bean Validation · JUnit 5 + Testcontainers

---

## Running it

1. Start Postgres (the `smart-fridge-db` Docker container).
2. Copy `.env.example` to `.env` and fill in the DB password. `.env` is git-ignored.
3. Run it from this folder (the `.env` path is relative to where you start the app):

```bash
./mvnw spring-boot:run
```

Health check: `GET http://localhost:8080/actuator/health`

Tests (needs Docker running; they start their own throwaway Postgres):

```bash
./mvnw test
```

---

## API

| Method | Path | What it does |
|---|---|---|
| GET | `/api/ingredients` | Ingredient catalog |
| GET | `/api/ingredients/{id}` | One ingredient |
| POST | `/api/ingredients` | Add a new kind of ingredient (name, storage, shelf life) |
| GET | `/api/fridge` | Everything you have, soonest-expiring first |
| GET | `/api/fridge?expiringWithinDays=3` | Only what you need to use soon (includes expired items) |
| POST | `/api/fridge` | Record groceries. Expiration is estimated if you leave it out |
| POST | `/api/fridge/{id}/consume` | Use part of a batch: `{"quantity": 2}`. 204 if used up |
| DELETE | `/api/fridge/{id}` | Throw it out |
| GET | `/api/recipes` | All recipes with ingredients |
| GET | `/api/recipes/{id}` | One recipe |
| POST | `/api/recipes` | Create a recipe |
| GET | `/api/recipes/suggestions?limit=5` | Ranked "what should I cook now" |
| POST | `/api/recipes/{id}/cook` | Cook it: subtracts ingredients, oldest batch first. 409 + shopping list if short |

Example: record groceries.

```bash
curl -X POST localhost:8080/api/fridge -H "Content-Type: application/json" -d "{\"ingredientName\":\"Eggs\",\"quantity\":12,\"unit\":\"pieces\"}"
```

Errors use the standard **RFC 9457 Problem Details** JSON format:
```json
{"status":409,"title":"Conflict","detail":"Not enough ingredients to cook 'Spinach Omelette'",
 "missing":[{"ingredientName":"Spinach","required":1.00,"available":0,"unit":"cups"}]}
```

---

## Code layout (layered architecture)

```
web/          Controllers: HTTP in, HTTP out. No business logic.
web/dto/      Request/response records: the public JSON contract.
service/      Business rules + transactions (FridgeService, RecipeService,
              ShelfLifeCalculator, RecipeScorer).
repository/   Spring Data JPA interfaces: database queries.
model/        @Entity classes mapped to the tables.
exception/    Custom exceptions + one global handler mapping them to HTTP codes.
config/       Clock bean, typed config properties.
resources/db/migration/   Flyway SQL migrations: the schema's source of truth.
```

A request flows **Controller → Service → Repository → Postgres**, and each layer only
talks to the one below it. That keeps HTTP details out of the business rules, so they can
be unit tested without a web server or a database.

---

## Design decisions (and why)

### Database / schema

**Flyway migrations instead of hand-run DBeaver scripts.**
The schema now lives in the repo as `V1__create_schema.sql`, `V2__seed_base_data.sql`,
`V3__units_and_constraints.sql`. Flyway runs any migrations that haven't run yet at startup
and records them in a `flyway_schema_history` table. So anyone (a teammate, CI, a
test) gets the identical schema from an empty database, and schema changes go through
code review like any other code. Your existing DB was built by hand, so
`baseline-on-migrate=true` + `baseline-version=2` told Flyway "this DB is already at V2";
it then applied only V3.

**`ddl-auto=validate`, not `update`.**
Hibernate *checks* that entities match the tables and refuses to start if they don't, but it
never changes the schema itself. `update` would let Hibernate silently alter tables,
which is unpredictable and can't be reviewed. One tool owns the schema (Flyway), and the
other only verifies it.

**V3 schema fixes (found while writing the backend):**
- `fridge_items.ingredient_id` was nullable, but a fridge item with no ingredient makes no sense. Now `NOT NULL`.
- `CHECK (quantity >= 0)` / `CHECK (quantity_required > 0)`: the database enforces the
  rule even if a future bug in the code doesn't.
- **`recipe_ingredients.unit` added.** "0.50 Cheddar" had no unit, so there was no way to
  tell if the 200 grams in your fridge covered "0.5 cups". Units were filled in from the comments in the
  seed script. The code only adds up quantities with matching units (`cup` = `cups` =
  `Cups`) and does not convert between units yet (see roadmap).
- Index on `fridge_items(ingredient_id)`, because "cook" looks up stock by ingredient.

**Secrets out of source control.** The DB password was hard-coded in
`application.properties`. It now comes from a git-ignored `.env`
(`spring.config.import=optional:file:.env[.properties]`), with `.env.example` committed as
a template. The folder was never committed, so the password never reached git history.

### JPA mapping

- **Postgres ENUM ↔ Java enum:** `@Enumerated(STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
  Without `NAMED_ENUM`, Hibernate sends a VARCHAR and Postgres rejects it ("column is of
  type storage_enum but expression is of type character varying"). The Java constants are
  lowercase because they must match the SQL labels exactly.
- **`BigDecimal` for quantities**, matching `DECIMAL(6,2)`. `double` can't represent 0.1
  exactly. Compare with `compareTo`, never `equals` (`1.0` vs `1.00` are not `equals`).
- **`recipe_ingredients` is its own entity (`RecipeIngredient`) with a composite key**
  (`@EmbeddedId` + `@MapsId`), not a `@ManyToMany`, because the join row carries data
  (quantity, unit).
- **`FetchType.LAZY` on every relationship**, plus `@EntityGraph` / `join fetch` in the
  queries that need the related data. This avoids the **N+1 query problem**: listing 50 recipes
  is 1 query, not 1 + 50 + more.
- **`open-in-view=false`**: the DB session closes when the service method returns. Services
  return DTOs, so controllers can't trigger surprise lazy-load queries.
- Lombok `@Getter/@Setter` only on entities, **not `@Data`**. `@Data` generates
  equals/hashCode over every field, which with JPA relationships can trigger lazy loads
  or infinite recursion.

### API & business logic

- **DTOs as Java records**, separate from entities. The API contract and DB layout can
  change independently, and clients can't send fields they shouldn't (like `id`).
- **Bean Validation** (`@NotNull`, `@Positive`, `@Digits(4,2)` to match `DECIMAL(6,2)`)
  rejects bad input with a 400 listing each bad field, *before* it reaches the database.
- **One `@RestControllerAdvice`** maps exceptions to status codes: 404 not found, 409
  conflict (duplicate / not enough stock), 400 bad input. Services just throw.
- **Expiration fallback (`ShelfLifeCalculator`)**: purchase date + default shelf life,
  adjusted when the user stores something unusually (frozen ×3; thawed meat ≤ 2 days;
  fridge food left in pantry ÷4). The numbers are a heuristic, so the rule is isolated in
  one class and can be swapped for real per-storage data later.
- **Suggestions (`RecipeScorer`)**: `score = 100 × coverage + 25 × urgency`, where
  coverage is the fraction of ingredients you have enough of and urgency is the sum of
  `1/(daysLeft+1)` for ingredients expiring within 3 days. Coverage dominates, and urgency
  pushes recipes that rescue expiring food to the top. It's a pure function with no
  database access, so it's unit-tested directly and could later be replaced by the
  Python AI service.
- **Cooking is atomic and FEFO** ("first expired, first out"):
  1. *All-or-nothing:* check every ingredient first, then subtract, inside one
     `@Transactional` method. A failure never leaves half the ingredients subtracted.
  2. *Oldest batch first,* so food is used before it expires.
  3. *Row locking:* the stock query uses `PESSIMISTIC_WRITE` (`SELECT … FOR UPDATE`), so
     two simultaneous "cook" requests can't both spend the same eggs (a race condition).
- **Injected `Clock`** instead of `LocalDate.now()`. Tests freeze "today", so expiration
  logic is deterministic.
- **Constructor injection** (no `@Autowired` fields): dependencies are `final`, explicit,
  and the class can be created with `new` in a unit test.

### Testing

- **Unit tests** (`ShelfLifeCalculatorTest`, `RecipeScorerTest`): plain JUnit, no Spring,
  run in milliseconds.
- **Integration tests** (`FridgeApiIntegrationTest`): the full stack over HTTP (MockMvc)
  against a **real Postgres 18 started by Testcontainers in Docker**. Not H2: an
  in-memory DB doesn't have Postgres ENUM types, so it would hide exactly the bugs that
  matter. Every run also re-applies V1–V3 to an empty DB, which proves the migrations work.

---

## Bugs fixed

| Bug | Cause | Fix |
|---|---|---|
| `BaseData.sql` insert fails | `VALUES ( (...), (...) )`: the extra outer parentheses make Postgres read it as ONE row whose values are rows | `VALUES (...), (...);` |
| Same script: syntax error | `;` placed *inside* the closing paren `...365);\n)` ends the statement early, leaving a stray `)` | Removed the wrapper paren; one `;` at the end |
| Seed links break on re-run | `recipe_ingredients` used hard-coded ids (1, 2…), but SERIAL ids aren't guaranteed to start at 1 after failed/repeated runs | Look ids up by name with a JOIN; `ON CONFLICT DO NOTHING` makes it re-runnable |
| Recipe quantities meaningless | No unit column on `recipe_ingredients` | V3 adds `unit`, backfilled |
| Fridge rows could have no ingredient | `ingredient_id` nullable | V3: `NOT NULL` |
| Password in source | Hard-coded in `application.properties` | Moved to git-ignored `.env` |
| Validation errors unhelpful | Default 400 said only "Invalid request content" | Handler now returns `errors: {field: message}` |

---

## Roadmap: what the backend should do next

1. **Unit conversion**: a conversion table (g↔kg, tsp↔tbsp↔cup) plus per-ingredient
   density for weight↔volume, so "200 g cheddar" can satisfy "0.5 cups".
2. **Users / households**: add `user_id` to `fridge_items`, plus authentication (Spring
   Security + JWT). Right now there's one shared kitchen.
3. **Meal plan for the week**: pick N recipes that together use the most expiring
   food. That's a small optimization problem (greedy first, then maybe ILP), and a natural place for the
   Python AI service.
4. **Integrate the Python AI service**: the Java backend stays the system of record and
   calls Python (REST) for parsing receipts / free text ("2 lbs chicken") into fridge
   items and for smarter ranking. `RecipeScorer` is already isolated for this.
5. **Expiry notifications**: a `@Scheduled` daily job that finds items expiring tomorrow.
6. **Waste tracking**: record *why* items left the fridge (cooked vs. thrown out) to
   show food-waste stats over time.
7. **API docs**: springdoc-openapi for an auto-generated Swagger UI.
8. **Per-storage shelf-life data**: an `ingredient_shelf_life(ingredient_id, storage_type, days)` table to replace the heuristic multipliers.
9. **CI**: a GitHub Actions workflow running `./mvnw test` on every push.
