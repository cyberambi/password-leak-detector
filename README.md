# Secure Password Leak Detector

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![React](https://img.shields.io/badge/React-19-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

A full-stack web app for checking whether a password has appeared in a known
data breach, scoring password strength, generating cryptographically secure
passwords, and storing an encrypted, per-user password history.

Built with Spring Boot (Java 17) on the backend, React (Vite) on the
frontend, MySQL for storage, JWT for auth, and a `docker-compose` stack that
brings the whole thing up with one command.

## Features

- **Breach check** - checks a password against the [Have I Been Pwned Pwned
  Passwords API](https://haveibeenpwned.com/API/v3#PwnedPasswords) using the
  k-anonymity model: only the first 5 hex characters of the password's SHA-1
  hash ever leave the server. The full password and full hash are never
  transmitted or logged.
- **Password strength analysis** - a custom entropy + pattern-penalty scorer
  (length, character variety, sequential runs like `abc`/`123`, keyboard
  walks like `qwerty`, repeated characters, common passwords, dictionary
  words) returning a 0-4 score and specific, actionable feedback.
- **Secure password generator** - configurable length and character sets,
  backed by `java.security.SecureRandom` (not `Math.random()`), guaranteeing
  at least one character from every selected set.
- **Encrypted password history** - logged-in users can save site credentials.
  Saved password values are encrypted at rest with AES-256-GCM using a key
  from environment config; login credentials are hashed one-way with BCrypt
  and are never recoverable. All history endpoints are strictly scoped to
  the authenticated user.
- **JWT auth with rotating refresh tokens** - short-lived (15 min) access
  tokens plus opaque, server-tracked refresh tokens (7 days) that rotate on
  every use. Reusing a already-rotated refresh token is treated as evidence
  of theft and revokes the entire token family.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway |
| Frontend | React 19, Vite, Axios, React Router |
| Database | MySQL 8 |
| Auth | JWT (access token) + opaque rotating refresh token (httpOnly cookie) |
| Containerization | Docker, docker-compose |

## Architecture notes

- **Token storage**: the access token lives only in memory on the frontend
  (never `localStorage`/`sessionStorage`), so it isn't reachable by an XSS
  payload reading browser storage. The refresh token lives in an `HttpOnly`,
  `SameSite=Strict` cookie the JS never touches. Both the docker-compose
  deployment (nginx reverse-proxies `/api/*` to the backend) and local dev
  (Vite dev-server proxy) keep frontend and backend same-origin, so no CORS
  configuration is needed and the cookie behaves consistently everywhere.
- **Refresh rotation + reuse detection**: every refresh token is single-use;
  presenting an already-used one revokes its entire rotation family, cutting
  off a stolen-token replay attack rather than just the one token.
- **Encryption**: password-history values use AES-256-GCM (authenticated
  encryption, random 96-bit IV per entry) so tampered ciphertext fails to
  decrypt instead of silently corrupting. The list endpoint never returns
  decrypted passwords in bulk - each entry is decrypted on demand via its own
  `GET /passwords/history/{id}` call.
- **Public vs. authenticated endpoints**: breach-check, strength-analysis and
  generate are public (no login needed to use the core tool); saving history
  requires authentication.

## Getting started

### Quickest path: Docker Compose

```bash
cp .env.example .env
# then edit .env: set real MySQL creds, JWT_SECRET (>=32 chars),
# and AES_KEY (base64, decodes to exactly 32 bytes)

docker compose up --build
```

- Frontend: http://localhost
- Backend API: http://localhost:8080/api/v1
- MySQL data persists in the `mysql_data` named volume across restarts.

Generate secrets with:

```bash
openssl rand -hex 32          # JWT_SECRET
openssl rand -base64 32       # AES_KEY
```

### Deploying to Render (free)

Render's free tier has no managed MySQL, and its static-site hosting can't
reverse-proxy to a separate service the way nginx does above - splitting
frontend and backend into two Render services would put them on different
subdomains, which breaks the `SameSite=Strict` refresh cookie entirely
(cross-subdomain cookies are blocked by the browser). So the Render deploy
uses a different shape than local/docker-compose:

- **One** Render web service, built from `Dockerfile.render`, which embeds
  the built React app into the Spring Boot jar's static resources - the API
  and the frontend are served from the same origin, so no CORS or cookie
  issues.
- Render's free **Postgres** instead of MySQL, activated via the `postgres`
  Spring profile (`SPRING_PROFILES_ACTIVE=postgres`), which only swaps the
  Flyway migration path (`db/migration/postgresql` vs `db/migration/mysql`) -
  entity code is identical either way.

To deploy:

1. Push this repo to GitHub (already done if you're reading this on GitHub).
2. In the [Render dashboard](https://dashboard.render.com), choose **New >
   Blueprint** and point it at this repo. Render reads `render.yaml` and
   provisions the free Postgres database and the free web service, wiring
   `DATABASE_URL`, `JWT_SECRET`, and `AES_KEY` automatically (the latter two
   via Render's `generateValue`, which produces a random base64-encoded
   256-bit value each - long enough for `JWT_SECRET`, and exactly the right
   size for `AES_KEY`).
3. First deploy takes a few minutes (Docker build + first Postgres
   provisioning). The free web service spins down after 15 minutes of
   inactivity and takes ~30s to wake up on the next request.

This was verified locally end-to-end (a real `postgres:16-alpine` container,
the actual `Dockerfile.render` image, `SPRING_PROFILES_ACTIVE=postgres`,
`DATABASE_URL` in Render's `postgresql://user:pass@host:port/db` format)
before being handed off - register, login, breach-check, history CRUD, the
SPA shell, and client-side route refreshes (`/history`, `/login`, etc.) all
confirmed working from the single origin.

### Local development (without Docker)

Backend (needs JDK 17; MySQL running locally or exposed from the compose
stack):

```bash
cd backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` to `localhost:8080`, so the frontend and
backend behave as same-origin in local dev too.

## API reference

All endpoints are under `/api/v1`.

| Method & path | Auth | Description |
|---|---|---|
| `POST /auth/register` | - | Create an account |
| `POST /auth/login` | - | Log in; returns an access token, sets the refresh cookie |
| `POST /auth/refresh` | cookie | Rotate the refresh token, issue a new access token |
| `POST /auth/logout` | cookie | Revoke the current refresh token family |
| `POST /passwords/check-breach` | - | k-anonymity HIBP breach check |
| `POST /passwords/analyze-strength` | - | Strength score (0-4) + feedback |
| `POST /passwords/generate` | - | Generate a secure password |
| `GET /passwords/history` | JWT | List saved entries (metadata only, no plaintext) |
| `GET /passwords/history/{id}` | JWT | Fetch one entry, including the decrypted password |
| `POST /passwords/history` | JWT | Save a new entry |
| `PUT /passwords/history/{id}` | JWT | Update an entry |
| `DELETE /passwords/history/{id}` | JWT | Delete an entry |

Error responses share a consistent JSON envelope (`timestamp`, `status`,
`error`, `message`, `path`, and `validationErrors` for 400s).

## Testing

```bash
cd backend
./mvnw test          # unit tests (strength scorer, encryption, generator, JWT)
                      # + integration tests (auth flow, history CRUD) against H2

cd frontend
npm run build         # production build
npm run lint          # oxlint
```

## Project structure

```
password-leak-detector/
├── backend/              # Spring Boot API (Maven)
├── frontend/             # React app (Vite)
├── docker-compose.yml    # local/dev: backend, frontend, mysql (two-container + nginx proxy)
├── Dockerfile.render     # Render deploy: frontend bundled into the backend jar (single origin)
├── render.yaml           # Render Blueprint - free web service + free Postgres
├── .env.example          # copy to .env and fill in real secrets
└── CLAUDE.md             # project conventions for AI-assisted development
```

## License

MIT - see [LICENSE](LICENSE).
