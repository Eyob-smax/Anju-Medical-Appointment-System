# Anju Medical Appointment System

A robust medical appointment and facility management backend system built with Spring Boot, Docker, and Nacos.

## Prerequisites

- Docker & Docker Compose

## Run The Application

From the project root, start all required services:

```bash
docker compose up -d
```

Docker Compose loads credentials from `.env` automatically.

### Exposed Ports

- **API Interface**: `8080`
- **MySQL**: `3306`
- **Nacos Configuration Server**: `8848`

## Run Tests In Docker

Run the full unit + API test suite in Docker:

```bash
./run_tests.sh
```

Before running Maven tests, the `test-runner` container performs an API readiness check against the backend service. If the API is not reachable and stable, the test run exits with failure. The script automatically tears down test containers and volumes on completion.
