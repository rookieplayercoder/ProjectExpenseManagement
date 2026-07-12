# Merge notes

This project folder was assembled from three uploads plus prior work in this
conversation. Documenting exactly what happened so nothing is a surprise.

## What was combined

1. **`ProjectExpenseManagement-src_with_docker.zip`** — used as the base.
   This is a more advanced backend state than what this conversation had
   built directly: it includes JWT auth (`security/`, `AuthService`,
   `AuthController`), rate limiting (`ratelimit/`), request logging
   (`logging/`), Docker support (`Dockerfile`, `docker-compose.yml`), and
   several integration tests (`AuthIntegrationTest`,
   `RateLimitingIntegrationTest`, `IdempotencyIntegrationTest`,
   `MalformedRequestIntegrationTest`) beyond what we wrote together here.
   It appears to come from further work on this project outside this
   conversation thread.

2. **`backend-patch-list-groups.zip`** — applied on top. Added
   `GET /api/v1/groups` (list the authenticated caller's groups), plus the
   supporting `GroupSummaryResponse` DTO and repository/service methods.
   Verified via diff that this patch was built against the exact base above
   (identical constructors/imports) - applied cleanly, no conflicts.

3. **`expense-management-frontend-step6-profile.zip`** — added as
   `frontend/`, sitting alongside the backend at the project root (not
   containerized in `docker-compose.yml` - run separately via `npm run
   dev`).

## Change made during the merge

- **`SecurityConfig.java`** — added a `CorsConfigurationSource` bean
  allowing `http://localhost:5173` (the Vite dev origin) and
  `.cors(Customizer.withDefaults())` in the filter chain. This was flagged
  as a known gap by both the patch's README and the frontend's own README -
  without it, every browser request from the frontend would be blocked by
  CORS before reaching Spring Security at all.

## Known gaps (from the frontend's own README, not verified further by me)

The frontend was built against a larger planned API surface than currently
exists. These will 404 in the running app until built:

| Frontend feature | Missing backend endpoint |
|---|---|
| Group Details — Overview/Members tabs | `GET /api/v1/groups/{groupId}` (single group), members list |
| Add/remove/invite group member | member-management endpoints on `GroupController` |
| Profile page | `GET /api/v1/users/me` |
| Member picker (add by email) | `GET /api/v1/users/lookup` |
| Edit/delete group or expense | `PUT`/`DELETE` endpoints (none exist yet for either) |
| Change password | no endpoint exists |

Everything else the frontend calls (login, register, create group, create
expense, expense detail, group balances/expenses/settlements, record a
settlement, list groups) matches what's implemented.

## Minor housekeeping not addressed

- `src/test/.../expense/ExpenseEqualSplitIntegrationTest.java` overlaps with
  `src/test/.../integration/ExpenseCreationEqualSplitIntegrationTest.java` -
  same coverage, different test class, likely an earlier draft. Left in
  place since it compiles fine and doesn't conflict, but worth deleting one
  of them to avoid confusion.
