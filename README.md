# Identra — Identity & Access Management Service

Identra is a backend Identity and Access Management (IAM) service built using Java and Spring Boot. It provides secure user authentication, JWT-based authorization, refresh token management, and role-based access control.

The project demonstrates backend security practices, REST API development, database migrations, automated testing, and containerization.

## Features

- User registration and login
- Password hashing using BCrypt
- JWT-based authentication
- Access token expiration
- Refresh token rotation
- Refresh token revocation on logout
- Role-Based Access Control (RBAC)
- Failed login attempt tracking
- Temporary account locking after repeated failed logins
- Login rate limiting
- Centralized exception handling
- PostgreSQL database integration
- Flyway database migrations
- Swagger/OpenAPI documentation
- Docker and Docker Compose support
- Automated tests using JUnit

## Tech Stack

- **Language:** Java
- **Framework:** Spring Boot
- **Security:** Spring Security, JWT, BCrypt
- **Database:** PostgreSQL
- **Persistence:** Spring Data JPA, Hibernate
- **Database Migrations:** Flyway
- **API Documentation:** Swagger / OpenAPI
- **Testing:** JUnit, Mockito
- **Containerization:** Docker, Docker Compose
- **Build Tool:** Maven

## Architecture

The application follows a layered backend architecture:

- **Controller:** Handles incoming HTTP requests.
- **Service:** Implements authentication and business logic.
- **Repository:** Handles database operations.
- **Entity:** Represents persistent database models.
- **DTO:** Defines request and response objects.
- **Config:** Contains security filters and application configuration.
- **Exception:** Handles application errors centrally.

## Getting Started

### Prerequisites

Install the following tools:

- Java 25
- Docker Desktop
- Git

Docker Compose is included with Docker Desktop.

### Clone the Repository

```bash
git clone https://github.com/Anvesha621/identra.git
cd identra
```

### Configure Environment Variables

Before starting the application, configure these environment variables:

| Variable | Description |
|---|---|
| `DB_PASSWORD` | Password for the PostgreSQL database |
| `JWT_SECRET` | Base64-encoded secret used to sign JWTs |

Use a strong, randomly generated JWT secret. Never commit actual passwords or secret values to GitHub.

The Docker Compose configuration uses these variables to configure PostgreSQL and the application.

### Run with Docker Compose

Build and start the application and PostgreSQL database:

```bash
docker compose up -d --build
```

Check the running containers:

```bash
docker compose ps
```

View application logs:

```bash
docker compose logs -f app
```

Stop the containers:

```bash
docker compose down
```

The PostgreSQL data is stored in a named Docker volume so it persists when containers are stopped and recreated.

**Note:** Avoid `docker compose down -v` unless you intentionally want to delete the Docker database volume.

## API Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user | Public |
| POST | `/api/auth/login` | Authenticate a user | Public |
| POST | `/api/auth/refresh` | Refresh access tokens | Public |
| POST | `/api/auth/logout` | Log out and revoke a refresh token | Public endpoint; token required by the implementation |
| GET | `/api/profile` | Access a protected resource | Authenticated |
| GET | `/api/admin` | Access an administrator-only resource | ADMIN role |

The exact request and response formats are documented in Swagger UI.

## API Documentation

After starting the application, open:

http://localhost:8080/swagger-ui/index.html

Swagger UI allows you to explore endpoints and test requests.

For protected endpoints, use the **Authorize** button to provide a valid access token.

## Security

Identra includes several authentication and authorization controls:

- Passwords are hashed using BCrypt.
- Access tokens are signed using a configurable JWT secret.
- Protected endpoints require authentication.
- Administrator endpoints enforce role-based authorization.
- Repeated failed login attempts trigger temporary account locking.
- Login requests are rate-limited.
- Refresh tokens support rotation and revocation.

## Database Migrations

Flyway manages database schema migrations.

Migration scripts are located at:

```text
src/main/resources/db/migration/
```

The migrations are applied automatically when the application starts.

## Testing

Run the automated test suite using Maven:

```bash
./mvnw clean test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean test
```

The current test suite contains 24 tests, all of which passed during the latest local run.

## Future Improvements

Potential future enhancements include:

- CI/CD integration with GitHub Actions
- Additional integration and security tests
- Production deployment
- Enhanced monitoring and observability

## Author

**Anvesha Pandey**

GitHub: [@Anvesha621](https://github.com/Anvesha621)