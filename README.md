# Anju Medical Appointment System

A robust medical appointment and facility management backend system built with Spring Boot, Docker, and Nacos.

## Prerequisites
- Docker & Docker Compose
- Java 17+ (for local development outside of Docker)
- Maven

## Quick Start (Automated Startup)

The system is fully containerized and designed to start without any human intervention beyond the initial Docker command. The database initializes itself, the configurations are managed, and the application connects directly to Nacos and MySQL seamlessly.

Run the following exact command from the root directory to spin up all services:

```bash
docker compose up -d
```

Docker Compose automatically loads credentials from `.env` in the project root.

### Exposed Ports
- **API Interface**: `8080`
- **MySQL**: `3306`
- **Nacos Configuration Server**: `8848`

## Verifying the Setup

You can execute the following local `curl` test command to verify that the core API is running and routing properly:

```bash
curl -v http://localhost:8080/
```
*(Note: Change the endpoint to an existing REST mapping e.g., `http://localhost:8080/appointment` or `http://localhost:8080/property` to see full localized response structures matching standard `Result<T>` output).*

All data parsing, configuration linking, and Docker bootstrapping are fully resolved. **No manual DB scripts or setup parameters need to be toggled.** Everything starts automatically.

## Swagger API Documentation

After the backend is running, open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Authentication in Swagger:
- Most endpoints require HTTP Basic auth.
- Use an existing account (for example, register via `POST /auth/register`) and then click **Authorize** in Swagger UI.
- For endpoints requiring secondary password, include header `X-Secondary-Password`.

## Frontend Demo (For Submission Screenshots)

A minimal presentation UI is included for full-stack style submission evidence.

- Demo page: `frontend/index.html`
- Style: `frontend/styles.css`
- API check script: `frontend/app.js`

Open `frontend/index.html` in a browser and keep backend running on `http://localhost:8080`.

## Run Tests In Docker (Non-Interactive)

All test suites are executed in containers (app + API tests + unit tests) with no interactive prompts.

```bash
sh run_tests.sh
```

You can also run tests directly with Docker Compose commands (no wrapper script):

```bash
docker compose --profile test up --build --abort-on-container-exit --exit-code-from test-runner test-runner
```

Optional cleanup after test run:

```bash
docker compose --profile test down -v
```

Windows CMD shortcut:

```cmd
run_tests_docker.cmd
```

What this script does automatically:
- Brings up the full stack with Docker Compose using the `test` profile.
- Builds and starts `mysql`, `nacos`, and `anju-backend`.
- Runs unit tests and API functional tests inside the `test-runner` container.
- Exits with code `0` on success and non-zero on failure.
- Tears down containers and volumes after test completion.
