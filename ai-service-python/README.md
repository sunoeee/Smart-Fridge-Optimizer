# AI Service (Python / FastAPI)

In progress; this folder is reserved for a small FastAPI service that turns messy text input or receipt images
into structured data that the Java backend can save to the database.

**Planned endpoint:**

```
POST /parse
{"text": "2 lbs chicken breast, 1 dozen eggs"}

-> [{"ingredient": "Chicken Breast", "quantity": 2, "unit": "lbs"},
   {"ingredient": "Eggs", "quantity": 12, "unit": "pieces"}]
```

**Design rules:**
- Stateless: the Python AI service never touches the database. The Java backend validates and stores
  the results to the database.
