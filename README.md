# OptiGridZA

OptiGridZA is an energy optimisation platform for South African businesses. The repository contains a React frontend, Spring Boot microservices, and a Python ML project.

## Architecture

- `frontend/`: Vite + React application.
- `backend/api-gateway/`: public API gateway on port 8080; validates JWTs and routes requests.
- `backend/eureka-service/`: service registry on port 8761.
- `backend/auth-service/`: registration, login, JWT issuance, and user management.
- `backend/company-service/`: company management.
- `backend/etl-service/`: energy and load-shedding data ingestion.
- `backend/prediction-service/`: prediction and model inference.
- `backend/simulation-service/`: battery and energy simulations.
- `backend/notification-service/`: alerts and tickets.
- `optigridza-ml/`: model development and Python tests.

## Run The Backend

Requirements: Docker Compose. From `backend/`, copy `.env.example` to `.env` for local-only test credentials. The example seeds `admin@test.local` / `TestAdmin123!` as the first system administrator. Change these values and generate a fresh base64 32-byte JWT secret (`openssl rand -base64 32`) before sharing or deploying the stack. Set `ESKOM_API_KEY` only when enabling live Eskom data fetching. Set `COOKIE_SECURE=true` when serving over HTTPS.

```sh
cd backend
cp .env.example .env
# The checked-in example values are for local development only.
docker compose up --build
```

The gateway is available at `http://localhost:8080`; Eureka is at `http://localhost:8761`. Compose reads `backend/.env`; do not commit it. Rotate the previously exposed database password, JWT signing secret, and Eskom API key before using real data. The database volume persists between runs. Stop the stack with `docker compose down` (add `-v` only when you intend to delete database data).

## Run The Frontend

```sh
cd frontend
npm install
npm run dev
```

The development server uses `http://localhost:5173` and defaults to the local API gateway. Override the gateway URL with `VITE_API_URL` if needed. Authentication uses an HttpOnly cookie; the production gateway origin must be configured with `CORS_ALLOWED_ORIGIN` and `COOKIE_SECURE=true`.

## API Overview

All application APIs are rooted at `/api/v1` and should be called through the gateway.

| Area | Routes | Access |
| --- | --- | --- |
| Authentication | `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me` | Register/login are public; remaining routes require a valid JWT cookie or bearer token |
| User company assignment | `PATCH /auth/users/{email}/company` | `SYSTEM_ADMIN` only |
| Companies | `/companies/**` | Authenticated |
| ETL | `/etl/**` | Authenticated |
| Predictions | `/prediction/**` | Authenticated |
| Simulations | `/simulation/**` | Authenticated |
| Notifications and tickets | `/notifications/**`, `/tickets/**` | Authenticated |

Public registration assigns the standard `ENERGY_MANAGER` role. For local development, the optional bootstrap admin is created once when `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` are set. Remove those variables after creating the admin account; administrative roles cannot be self-assigned.

## Tests

Run Java tests from an individual service directory with `./mvnw test` (Windows: `mvnw.cmd test`). The Python ML tests use a checked-in fixture and run with `python tests/test_data.py` from `optigridza-ml/`.
