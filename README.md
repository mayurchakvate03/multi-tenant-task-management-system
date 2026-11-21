# Multi-Tenant Task Management System

A backend-only Spring Boot task management API. All tenants share one H2 database and one schema. Tenant identity is taken from a signed JWT and is applied to every tenant-owned repository read and write.

## Requirements

- Java 17 or newer
- Maven 3.6.3 or newer
- Docker only if you want to build and run the container image

## Run locally

From this directory:

```powershell
mvn clean verify
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. The default database is an in-memory H2 database, so its contents reset when the process stops. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` to use a file-backed H2 database. Using a different database requires adding its JDBC driver and configuring that driver in `application.yml`.

The local JWT secret is for development only. Before deployment, set `JWT_SECRET` to a securely generated Base64-encoded key of at least 32 bytes. `JWT_EXPIRATION_MS` controls token lifetime, and `PORT` controls the HTTP port.

## API request flow

1. Register an organization with `POST /tenant/register`. This creates the tenant and its first `ADMIN` account in one transaction.
2. Sign in with `POST /auth/login`. Email is globally unique, so login does not need a tenant identifier.
3. Send the returned token as `Authorization: Bearer <accessToken>` on secured requests.
4. `JwtAuthenticationFilter`, a `OncePerRequestFilter`, validates the signature and expiration, reads `tenant_id`, `user_id`, and `role`, and stores them in the Spring Security principal for the request.
5. Role checks guard controller actions. Services obtain tenant identity from that principal. Repository methods for tasks and users include `tenantId` in their predicates, including lookups before updates and assignments.

No request DTO or route accepts a tenant identifier. Unknown JSON fields are rejected, including a client-supplied `tenant_id`. The tenant column on the tenant table is set to that tenant's own ID; user and task rows store the same value in `tenant_id`.

## API endpoints

| Method and path | Access | Purpose |
| --- | --- | --- |
| `POST /tenant/register` | Public | Create tenant and first admin |
| `POST /auth/login` | Public | Authenticate by email and password; return JWT |
| `POST /users` | `ADMIN` | Create a user in the caller's tenant |
| `POST /tasks` | `MANAGER` | Create a task in the caller's tenant; initial status is `OPEN` |
| `PUT /tasks/{id}/assign` | `MANAGER` | Assign a same-tenant user to a same-tenant task |
| `PUT /tasks/{id}/status` | `USER` | Update status only on a task assigned to the caller |
| `GET /tasks` | `ADMIN`, `MANAGER`, `USER` | List tenant tasks; users see only their assigned tasks |

Roles are `ADMIN`, `MANAGER`, and `USER`. Task statuses are `OPEN`, `IN_PROGRESS`, and `COMPLETED`. All request bodies are validated. Errors use a common JSON shape with a timestamp, HTTP status, message, optional field details, and request path.

## Example requests

Register a tenant and its admin:

```http
POST /tenant/register
Content-Type: application/json

{
  "tenantName": "Acme",
  "adminEmail": "admin@acme.example",
  "adminPassword": "ChangeThisPassword123!"
}
```

Log in:

```http
POST /auth/login
Content-Type: application/json

{
  "email": "admin@acme.example",
  "password": "ChangeThisPassword123!"
}
```

Create a manager as the admin:

```http
POST /users
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "email": "manager@acme.example",
  "password": "ChangeThisPassword123!",
  "role": "MANAGER"
}
```

Create a task as the manager:

```http
POST /tasks
Authorization: Bearer <managerAccessToken>
Content-Type: application/json

{
  "title": "Prepare release",
  "description": "Complete the release checklist"
}
```

Assign a task using the target user's ID returned from `POST /users`:

```http
PUT /tasks/1/assign
Authorization: Bearer <managerAccessToken>
Content-Type: application/json

{
  "assignedTo": 2
}
```

The assignment PDF requests an example JWT payload. This is the decoded payload shape; signatures and timestamps vary for each token:

```json
{
  "sub": "manager@acme.example",
  "tenant_id": "63c56cc6-d242-4d42-beb9-9ab21243a763",
  "user_id": 2,
  "role": "MANAGER",
  "iat": 1763733600,
  "exp": 1763737200
}
```

## Tests and coverage

The integration suite exercises registration, login, JWT claims, validation, role restrictions, task assignment, status ownership, tenant isolation, and global errors.

```powershell
mvn clean verify
```

JaCoCo writes an HTML report to `target/site/jacoco/index.html` and fails `verify` if line coverage is below 80% for the project bundle.

The verified build in this workspace passed all 6 integration tests with 97.17% line coverage.

## SonarQube

The project includes the SonarScanner for Maven plugin and `sonar-project.properties`. Start a local SonarQube Server using its official container image or an existing server, then set `SONAR_TOKEN` and run:

```powershell
$env:SONAR_TOKEN = "<your-token>"
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=$env:SONAR_TOKEN
```

Coverage XML is generated before the Sonar goal and is configured at `target/site/jacoco/jacoco.xml`.

## Container and deployment

Build the application first, then build and run the image:

```powershell
mvn clean package -DskipTests
docker build -t multi-tenant-task-management-system .
docker run --rm -p 8080:8080 -e JWT_SECRET="<base64-secret>" multi-tenant-task-management-system
```

The container can be deployed to a Docker-compatible hosting service. No external deployment is configured in this project. The default in-memory database is intended for local evaluation and loses data on restart; choose a persistent database and configure credentials before using a hosted environment.

## Assumptions and design decisions

- One H2 database and one schema are used for all tenants, as required. The `tenant_id` column is present on every table.
- Email addresses are globally unique so that login can identify one account without accepting a tenant ID from the request.
- Tenant IDs are generated by the server during registration and cannot be selected or overridden by API clients.
- `ADMIN` can create users and view all tenant tasks. `MANAGER` can create, assign, and view tenant tasks. `USER` can view and update only tasks assigned to that user.
- Task assignment uses a user ID in the body; the service verifies that both task and user belong to the authenticated tenant before persisting the assignment.
- Task status changes are limited to the three documented enum values. New tasks start as `OPEN` and are unassigned until a manager assigns them.
- Passwords are stored with BCrypt. API responses never include stored password hashes.
- The H2 database is in-memory by default for a self-contained assignment run. A persistent production deployment needs a durable database and a securely managed JWT secret.

## Assignment submission notes

The assignment document also requests a GitHub upload and a five-minute project walkthrough video. This workspace contains the complete local project; no repository remote or personal video is configured here. Push this folder to the interview GitHub repository and record the walkthrough before submission.
