# Expense Management — Frontend

React 19 + TypeScript + Vite + Tailwind CSS v4 frontend for your Spring Boot
Expense Management backend. Built incrementally, one roadmap section at a time.

## Setup

```bash
npm install
cp .env.example .env   # set VITE_API_BASE_URL to your backend, e.g. http://localhost:8080/api/v1
npm run dev
```

Your Spring Boot app must be running (default `localhost:8080`) with CORS
allowing `http://localhost:5173`. **Note:** `SecurityConfig.java` doesn't
currently configure CORS — you'll need to add a `CorsConfigurationSource`
bean (or `@CrossOrigin`) allowing the Vite dev origin, or browser requests
from this frontend will be blocked.

## Progress

### ✅ Step 1 — Authentication (done)
- `POST /api/v1/auth/login` → `LoginForm`, `AuthContext.login`
- `POST /api/v1/users` (registration) → `RegisterForm`, `AuthContext.register`
- JWT stored in `localStorage`, attached to every request via an Axios
  interceptor (`src/api/axiosClient.ts`)
- `ProtectedRoute` guards authenticated pages; 401 responses auto-clear the
  session and redirect to `/login`

### ✅ Step 2 — Dashboard (done)
- Requires the `GET /api/v1/groups` backend patch (see `backend-patch-list-groups.zip`)
  — apply that first or this page has nothing to show.
- `useMyGroups` fetches the user's groups; `useDashboardData` then fetches
  balances/expenses/settlements **per group in parallel** (`useQueries`) and
  aggregates client-side, since there's no cross-group aggregate endpoint yet.
- Cards: Total You Owe, Total You Are Owed, Net Balance, Total Expenses (all
  grouped by currency, since a user's balances can span currencies), Recent
  Expenses (last 5), Recent Settlements (last 5), Groups Summary grid.
- Loading skeletons and empty states included. Group cards aren't clickable
  yet — that lands in Step 3 (Group Management) with Group Details.

### ✅ Step 3 — Group Management (done)
- `GroupsPage` (`/groups`) lists all your groups via `GET /api/v1/groups`, with
  loading skeletons and an empty state.
- `CreateGroupDialog` — creates a group via `POST /api/v1/groups`. Members
  are added by email (`MemberEmailPicker` calls `GET /api/v1/users/lookup`),
  not by pasting UUIDs.
- Dashboard and Groups page group cards now link to `/groups/:id` — that
  route (Group Details) doesn't exist yet, so clicking a group currently
  404s. That's Step 4, next.
- Added `Dialog`, `Toast`, `Avatar` primitives and a `ThemeContext` (dark
  mode toggle in the navbar) to `components/ui` — needed for this step and
  reused going forward.

### ✅ Step 4 — Group Details (done)
- `GroupDetailsPage` (`/groups/:groupId`) with 5 tabs, matching the roadmap:
  - **Overview** — description, created date, member count, Edit Group dialog (`PUT /groups/{id}`)
  - **Expenses** — list via `GET /groups/{id}/expenses`; "Add expense" links to `/groups/:id/expenses/new`, which 404s until Step 5
  - **Balances** — "who owes whom" via `GET /groups/{id}/balances`, with a working **Settle up** button that opens `RecordSettlementDialog` (`POST /settlements`)
  - **Settlements** — history via `GET /groups/{id}/settlements`
  - **Members** — list + Add member (email lookup) + Remove member, via the member-management endpoints from the backend patch
- Custom `Tabs` and `Dialog`/`ConfirmDialog` primitives (no external dep, since Radix wasn't installable offline here).

### ✅ Step 5 — Expense Management (done)
- `CreateExpensePage` (`/groups/:groupId/expenses/new`) — `ExpenseForm` with
  `SplitTypeSelector` (Equal/Exact/Percentage), `ParticipantSelector`,
  `AmountInput`, `CurrencySelector`, all wired to `POST /api/v1/expenses`.
  Client-side validation enforces exact amounts sum to the total and
  percentages sum to 100 before submitting (server also validates).
- `ExpenseDetailsPage` (`/expenses/:expenseId`) — full split breakdown via
  `GET /api/v1/expenses/{id}`.
- Expense cards (Dashboard + Group Details → Expenses tab) are now clickable
  and link to Expense Details.
- **Not implemented:** editing/deleting an expense. There's no `PUT`/`DELETE`
  endpoint for expenses, and building one safely means reversing previously
  applied balance deltas before reapplying new ones — nontrivial financial
  logic I didn't want to bolt on without your sign-off. Let me know if you
  want that built out.

### ✅ Step 6 — Profile (done)
- `ProfilePage` (`/profile`) — full name, email, mobile, role, member-since
  date, via `GET /api/v1/users/me`.
- **Not implemented:** Change Password. No backend endpoint for it exists
  (`UserController` only has create/me/lookup). Let me know if you want that
  added.

## Roadmap status

Everything in **Priority 1** (Auth, Dashboard, Group Management, Expense
Management, Balance Visualization, Settlement Management) and most of
**Priority 2** (Responsive UI, Dark Mode, Profile) is done.

Left on the checklist, both optional/nice-to-have:
- **Charts & Analytics** — the roadmap lists these as "Optional Charts" under
  Balance Visualization (pie/bar/monthly-trend via Recharts, already in the
  dependency list but unused so far).
- **Minor UI enhancements** — floating action button, toast-based
  notifications beyond the ones already added for form feedback.

Say the word if you'd like either of those built out; otherwise this covers
the full functional roadmap.

## ⚠️ Backend API gaps found while reviewing your code

Your controllers currently expose a smaller surface than the full roadmap
assumes. These will block some planned screens until added:

| Roadmap feature | Needs an endpoint that doesn't exist yet |
|---|---|
| ~~"My Groups" list, Dashboard groups summary~~ | ✅ added — see `backend-patch-list-groups.zip` |
| Group Details — Overview/Members tabs | `GET /api/v1/groups/{groupId}` and a members list |
| Add/remove member, invite member | member-management endpoints on `GroupController` |
| Profile page | `GET /api/v1/users/{id}` or `/me` |
| Edit/Delete group or expense | `PUT`/`DELETE` endpoints |

Everything else (login, register, create group, create expense, get expense
detail, group balances/expenses/settlements, record a settlement) matches
what's already implemented, and the frontend calls those exactly as your
DTOs define them.

## Tech stack

React 19 · TypeScript · Vite · Tailwind CSS v4 · React Router · Axios ·
React Hook Form · Zod · TanStack Query · hand-built shadcn-style UI
primitives (no CLI network access was available in this environment, so
`button`/`input`/`label`/`card`/`alert` were written to match shadcn's API).

## Project structure

```
src/
├── api/              # axios client + one file per backend resource
├── components/
│   ├── auth/         # LoginForm, RegisterForm, ProtectedRoute, LogoutButton
│   ├── dashboard/     # BalanceCard, ExpenseCard, SettlementCard, GroupCard
│   ├── layout/        # Navbar
│   └── ui/            # shadcn-style primitives
├── contexts/          # AuthContext
├── hooks/             # useAuth, useMyGroups, useDashboardData
├── layouts/           # AppLayout (navbar + outlet)
├── pages/
│   ├── auth/          # LoginPage, RegisterPage
│   └── dashboard/      # DashboardPage
├── routes/             # AppRoutes
├── types/              # TS types mirroring backend DTOs
├── utils/              # cn(), authStorage, format (currency/date)
├── App.tsx
└── main.tsx
```
