# ProjectExpenseManagement

Splitwise-style expense management app. Spring Boot 3 / Java 21 / PostgreSQL
backend with JWT auth, rate limiting, and Testcontainers integration tests,
plus a React/TypeScript/Vite frontend.

This is a merged snapshot: the backend (with Docker support) + a small
patch adding `GET /api/v1/groups` + the Step 6 frontend build, combined
into one project folder. See `merge-notes.md` for exactly what was
combined and what's still missing.

## Running it

### Backend

```bash
cp .env.example .env   # fill in DB_PASSWORD and JWT_SECRET
docker compose up --build
```

This starts Postgres + the Spring Boot app (Flyway runs migrations
automatically). API is at `http://localhost:8080/api/v1`.

Alternatively, without Docker: run a local Postgres, set `DB_URL` /
`DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET` as environment variables, then
`mvn spring-boot:run`.

### Frontend

```bash
cd frontend
npm install
cp .env.example .env   # VITE_API_BASE_URL=http://localhost:8080/api/v1
npm run dev
```

Opens at `http://localhost:5173`. The backend must already be running, and
CORS for `http://localhost:5173` is enabled in `SecurityConfig` (added as
part of this merge).

### Tests

```bash
mvn test
```

Uses Testcontainers - requires Docker running locally, no manual DB setup needed.
