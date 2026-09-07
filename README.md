# Resource Booking API

A Spring Boot REST API for booking shared resources (rooms, equipment, etc.)
with JWT authentication and role-based access control.

## Stack
- Spring Boot, Spring Data JPA, Spring Security (JWT, stateless)
- MySQL (H2 in-memory for tests)
- Bean Validation (`jakarta.validation`)

## Running locally

1. Create a MySQL database:
   ```sql
   CREATE DATABASE resource_booking;
   ```
2. Set environment variables (or edit `application.properties` directly for local dev):
   ```bash
   export DB_URL=jdbc:mysql://localhost:3306/resource_booking
   export DB_USERNAME=root
   export DB_PASSWORD=root
   export JWT_SECRET="a-long-random-production-secret"   # required for real deployments
   ```
3. Run:
   ```bash
   ./mvnw spring-boot:run
   ```

The app seeds two accounts on first startup (`DataSeeder`):

| Username | Password | Role  |
|----------|----------|-------|
| admin    | admin123 | ADMIN |
| user     | user123  | USER  |

## Running tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database (`src/test/resources/application-test.properties`)
so no MySQL instance is required.

## Authentication

`POST /auth/login`
```json
{ "username": "user", "password": "user123" }
```
Returns `{ "token": "...", "username": "..." }`. Send the token on every
subsequent request as `Authorization: Bearer <token>`.

## Authorization rules

- `ADMIN` — full CRUD on resources, can view/update/delete **any** reservation.
- `USER` — read-only on resources, can create reservations and can only
  view/update/delete their **own** reservations. Attempting to access another
  user's reservation returns `403 Forbidden`. A user can only modify/cancel a
  reservation while it's still `PENDING`.

## Reservation pricing & booking rules

- Price is **always computed server-side** as `resource.price × duration in hours`
  (the resource's `price` field is treated as an hourly rate). Clients cannot
  set or influence price.
- A reservation is rejected with `409 Conflict` if:
  - the resource is marked unavailable, or
  - the requested time window overlaps an existing non-cancelled reservation
    for that resource.
- New reservations always start as `PENDING`. Only an `ADMIN` can change status
  (e.g. to `CONFIRMED`/`CANCELLED`) via `PUT /reservations/{id}`.

## Endpoints

### Resources (`/resources`)
| Method | Path | Access |
|---|---|---|
| GET | `/resources` | USER, ADMIN |
| GET | `/resources/{id}` | USER, ADMIN |
| POST | `/resources` | ADMIN |
| PUT | `/resources/{id}` | ADMIN |
| DELETE | `/resources/{id}` | ADMIN |

### Reservations (`/reservations`)
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/reservations?page=&size=&status=&minPrice=&maxPrice=&sortBy=&direction=` | USER (own only), ADMIN (all) | Filters combine correctly (status AND price range together) |
| GET | `/reservations/{id}` | Owner or ADMIN | 403 if not owner/admin, 404 if missing |
| POST | `/reservations` | USER, ADMIN | Body: `{ "resourceId", "startTime", "endTime" }` |
| PUT | `/reservations/{id}` | Owner (if PENDING) or ADMIN | Body: `{ "resourceId", "startTime", "endTime", "status" }` — `status` is ignored unless caller is ADMIN |
| DELETE | `/reservations/{id}` | Owner or ADMIN | |

## Error format

All errors return a consistent JSON body:
```json
{
  "timestamp": "2026-09-05T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Reservation not found with id: 42"
}
```
Validation errors (`400`) additionally include a `fieldErrors` map naming
each invalid field.

| Status | When |
|---|---|
| 400 | Malformed input / failed `@Valid` validation / bad time range |
| 401 | Bad credentials at login |
| 403 | Authenticated but not authorized (wrong role, or not the reservation owner) |
| 404 | Resource/reservation not found |
| 409 | Resource unavailable or double-booking conflict |

## Postman

Import `postman_collection.json` from the project root. It includes requests
for login, resource CRUD, and reservation CRUD with a collection variable
(`token`) that's set automatically from the login response.

## Security notes

- Passwords are BCrypt-hashed and never serialized in API responses (`AppUser.password` is `@JsonIgnore`).
- JWT signing secret is read from the `JWT_SECRET` environment variable — do
  not use the default dev value in any real deployment.

   Final submission - Resource Booking System
