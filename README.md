# Smart Fridge Optimizer (Meal Planner)

helping college students without a college meal plan eat well :)
- tracks what food is in your kitchen
- estimates when each item expires
- suggests recipes that use food before it goes bad

## Repo layout

| Folder | What it is | Status |
|---|---|---|
| [`backend/`](backend/) | Java / Spring Boot REST API on PostgreSQL | Working |
| [`ai-service-python/`](ai-service-python/) | Python FastAPI service that parses free text and receipts into fridge items | In-Progress |

## Architecture

```
client ──HTTP──▶ Spring Boot backend ──▶ PostgreSQL
                       │
                       └──HTTP──▶ Python text/receipt parser
```

- the Java backend manages the database
- the Python service will be stateless: the backend
sends it raw text, gets structured JSON back (ingredient, quantity, unit), then validates
and saves the result itself.

## CURRENT FEATURES

- ingredient catalog, fridge inventory, and recipes over a REST API (uses 12 endpoints)
- can estimate expiration dates when you don't enter a date, based on shelf life and storage
- recipe suggestions ranked by ingredient coverage and how soon ingredients expire
- "Cook a recipe": all-or-nothing, uses the oldest ingredients first, returns a shopping list
  (HTTP 409) if you don't have enough ingredients for the recipe
- PostgreSQL schema is versioned using Flyway
- there are unit tests plus integration tests against a
  real Postgres (Testcontainers, Docker)

Also see [`backend/README.md`](backend/README.md) 

## PLANNED:

- Python text/receipt parser, called from the backend
- Web frontend
- Unit conversion (such as grams <-> cups), user accounts, weekly meal planning
