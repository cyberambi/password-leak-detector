# Secure Password Leak Detector

Full-stack app: checks passwords against known breaches (HIBP k-anonymity API),
analyzes password strength, generates secure passwords, and stores encrypted
password history for authenticated users.

## Tech stack
- Backend: Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA
- Frontend: React.js (Vite), Axios, React Router
- DB: MySQL 8
- Auth: JWT (access + refresh tokens)
- Containerization: Docker + docker-compose

## Project structure
- `backend/` — Spring Boot app (Maven)
- `frontend/` — React app (Vite)
- `docker-compose.yml` — wires backend, frontend, mysql

## Commands
- Backend tests: `cd backend && ./mvnw test`
- Backend run (local, no docker): `cd backend && ./mvnw spring-boot:run`
- Frontend dev server: `cd frontend && npm run dev`
- Frontend build: `cd frontend && npm run build`
- Frontend lint: `cd frontend && npm run lint`
- Full stack: `docker-compose up --build`
- Run a single backend test class: `./mvnw test -Dtest=ClassName`

## Code style
- Backend: standard Java/Spring conventions, constructor injection (no
  `@Autowired` on fields), DTOs for request/response bodies — never expose
  JPA entities directly over the API.
- Frontend: functional components + hooks only, no class components.
  Axios calls live in `src/api/`, not inline in components.

## Security rules (non-negotiable — do not weaken these without asking)
- Never send a full password or full SHA-1 hash to the HIBP API — only the
  first 5 hex characters of the hash (k-anonymity).
- Login passwords are hashed one-way with BCrypt. Never store or log
  plaintext passwords.
- Saved password-history entries are encrypted at rest with AES-256 using a
  key from environment config — never hardcode the key, never commit it.
- JWT secret, DB credentials, and AES key are all read from environment
  variables (`application.yml` uses `${VAR_NAME}` placeholders), never
  committed to git.
- All `/passwords/history` endpoints must scope queries to the authenticated
  user's own ID — always verify this when touching that code path.

## Workflow
- Use plan mode for anything touching auth, encryption, or the DB schema.
  Small fixes (typos, single-line changes) can skip planning.
- After implementing a backend change, run `./mvnw test` before considering
  it done.
- After implementing a frontend change, run `npm run build` and `npm run lint`
  before considering it done.
- Prefer running a single test class over the whole suite while iterating;
  run the full suite before committing.
- Commit with descriptive messages after each working milestone (e.g. "add
  breach-check endpoint with k-anonymity lookup").

## Gotchas
- MySQL container needs a few seconds to become healthy — docker-compose
  healthcheck gates backend startup, don't remove it.
- Refresh-token rotation invalidates the old refresh token; if auth tests
  fail intermittently, check for reuse of a stale refresh token.
