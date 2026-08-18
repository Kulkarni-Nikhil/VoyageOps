# VoyageOps

Backend-first travel planning platform built as a resume project. Users manage
trips collaboratively, track budgets and expenses, and pull live weather and
points-of-interest for their destinations — served with Redis caching, method-level
resilience, scheduled jobs, and containerized infrastructure.

## Tech Stack

| Concern            | Choice                                                                 |
| ------------------ | ---------------------------------------------------------------------- |
| Language           | Java 25                                                                 |
| Framework          | Spring Boot 4.1 (Spring Framework 7)                                    |
| Persistence        | Spring Data JPA / Hibernate, PostgreSQL 17, Flyway migrations           |
| Cache              | Redis 7 (JSON serialization with per-cache TTLs)                        |
| Resilience         | Spring Resilience (`@Retryable`, `@ConcurrencyLimit`)                   |
| HTTP client        | `RestClient` (Spring Boot `restclient` starter)                         |
| API docs           | Springdoc OpenAPI 3 / Swagger UI                                        |
| Build / run        | Maven, Docker Compose                                                   |
| Tests              | JUnit 5, Mockito, AssertJ                                               |

## Architecture

- **Package-by-feature**: each feature (`trip`, `user`, `expense`, `weather`,
  `places`) owns its `domain / application / infrastructure / web` layers.
- **Hexagonal integrations**: external providers sit behind ports
  (`Geocoder`, `WeatherProvider`, `PlacesProvider`) with adapter
  implementations (Open-Meteo, OpenTripMap), so providers can be swapped.
- **Caching with auth safety**: cached readers are deliberately free of
  authorization logic and are only reachable through facades that enforce trip
  membership *before* any cache lookup — a cache hit can never bypass access
  control.
- **Resilience**: every adapter method is retried on transient failures
  (4xx excluded) and concurrency-limited, backed by short HTTP client timeouts.
- **Forecast horizon**: Open-Meteo only forecasts ~15 days ahead; requests for
  farther-out trips return the available days plus a `notice` describing the
  limitation instead of failing.

```
Client ──> Web controllers ──> Application services ──> Ports ──> Adapters
                                    │                    │           │
                                    │                    └──> Open-Meteo (weather/geocoding)
                                    │                    └──> OpenTripMap (places)
                                    ▼
                            Redis cache layer
                              (per-cache TTL)
```

## Project Structure

```
VoyageOps/
├── app/                      Spring Boot application
│   ├── Dockerfile            Multi-stage build (Maven → JRE)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/voyageops/app/
│       │   ├── config/       Cache, resilience, scheduling configuration
│       │   ├── common/       Exceptions, ProblemDetail handling, web helpers
│       │   ├── trip/         Trips, destinations, activities
│       │   ├── user/         Users, members, roles, invitations
│       │   ├── expense/      Expenses, budget summary
│       │   ├── weather/      Weather forecast + geocoding (hexagonal)
│       │   ├── places/       Nearby points of interest (hexagonal)
│       │   └── jobs/         Scheduled maintenance & prewarm jobs
│       ├── main/resources/db/migration/   Flyway schema
│       └── test/             Unit tests per feature
└── compose.yaml              PostgreSQL, Redis, app
```

## Quickstart (Docker Compose)

```bash
docker compose up -d --build
```

This starts PostgreSQL, Redis, and the API. The app is then reachable at
[http://localhost:8080](http://localhost:8080):

- Health: `http://localhost:8080/actuator/health`
- API docs: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Optional — enable places discovery (OpenTripMap requires a free key):

```bash
INTEGRATION_OPENTRIPMAP_API_KEY=your-key docker compose up -d --build
```

## Local Development

Requirements: JDK 25, Docker (for PostgreSQL + Redis).

```bash
# 1. Start infrastructure only
docker compose up -d postgres redis

# 2. Run the application
cd app
./mvnw spring-boot:run
```

PostgreSQL is exposed on port `5433` (to avoid clashing with a local install),
Redis on `6379`.

## Configuration

All values have sensible defaults and are overridable via environment
variables.

| Variable                         | Default                                          | Description                              |
| -------------------------------- | ------------------------------------------------ | ---------------------------------------- |
| `DB_URL`                         | `jdbc:postgresql://localhost:5433/voyageops`     | JDBC URL                                 |
| `DB_USERNAME` / `DB_PASSWORD`    | `voyageops` / `voyageops`                        | Database credentials                     |
| `REDIS_HOST` / `REDIS_PORT`      | `localhost` / `6379`                             | Redis connection                         |
| `SERVER_PORT`                    | `8080`                                           | HTTP port                                |
| `INTEGRATION_OPENTRIPMAP_API_KEY`| *(empty)*                                        | OpenTripMap key; without it the places endpoint returns `503` |

## Authentication

A real identity provider is out of scope. The API uses an `X-User-Id` request
header (a UUID) as the current-user seam:

- missing header → `401 Unauthorized`
- user is not a member of the trip → `403 Access Denied`
- unknown user UUID → `404`

## API Overview

### Trips
| Method | Path | Description |
| ------ | ---- | ----------- |
| GET | `/trips` | List trips (paginated) |
| POST | `/trips` | Create a trip (owner is seeded as OWNER member) |
| GET | `/trips/{id}` | Trip detail (cached) |
| PUT | `/trips/{id}` | Update trip / transition status (evicts caches) |
| DELETE | `/trips/{id}` | Delete trip |

### Destinations & activities
| Method | Path | Description |
| ------ | ---- | ----------- |
| GET/POST | `/trips/{tripId}/destinations` | List / add destinations |
| PUT/DELETE | `/destinations/{id}` | Update / delete destination (evicts weather + places) |
| GET/POST | `/trips/{tripId}/activities` | List / add activities |
| PUT/DELETE | `/activities/{id}` | Update / delete activity |

### Collaboration
| Method | Path | Description |
| ------ | ---- | ----------- |
| GET/POST | `/trips/{tripId}/members` | List / add members |
| PATCH | `/trips/{tripId}/members/{userId}` | Change role (owner-only) |
| DELETE | `/trips/{tripId}/members/{userId}` | Remove member (owner-only) |
| POST | `/trips/{tripId}/invitations` | Invite by email (48h token) |
| POST | `/invitations/{token}/accept` | Accept invitation |
| GET | `/users` | List users (cached) |

### Expenses
| Method | Path | Description |
| ------ | ---- | ----------- |
| GET/POST | `/trips/{tripId}/expenses` | List (paginated) / add |
| GET | `/trips/{tripId}/expenses/summary` | Budget summary (spent, remaining, count) |
| GET/PUT/DELETE | `/trips/{tripId}/expenses/{expenseId}` | Detail / update / delete |

### Integrations
| Method | Path | Description |
| ------ | ---- | ----------- |
| GET | `/trips/{tripId}/weather` | Daily forecast for the trip window (cached 3h) |
| GET | `/trips/{tripId}/places?radiusMeters=5000` | Nearby points of interest (cached 24h, requires API key) |

## Caching

| Cache | TTL | Evicted on |
| ----- | --- | ---------- |
| `trip` | 10 min | trip update/delete |
| `trip-destinations` | 1 h | destination write |
| `trip-activities` | 1 h | activity write |
| `expense-summary` | 5 min | expense write |
| `users` | 30 min | user write |
| `weather` | 3 h | trip/destination write |
| `places` | 24 h | trip/destination write |

A scheduled job prewarms the `weather` cache hourly for trips starting within
the next seven days, so common requests rarely hit the upstream API cold.

## Testing

```bash
cd app
./mvnw test
```

The suite uses JUnit 5 + Mockito + AssertJ (no Spring context / containers),
covering domain policies, service behavior, cache/eviction contracts, and
scheduled jobs.

## Roadmap (Phase 2, not yet implemented)

- Spring AI assistant for itinerary suggestions
- OAuth2 / Spring Security identity
- Testcontainers integration tests + CI (GitHub Actions)
