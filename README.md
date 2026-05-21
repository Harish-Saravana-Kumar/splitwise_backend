# Splitwise Backend

Public frontend: https://splitwise-frontend-pink.vercel.app

A Spring Boot backend for a Splitwise-style expense sharing app. It supports user authentication, group expense tracking, balance calculation, settlements, password reset, Google sign-in, and an AI assistant workflow for guided expense and settle-up actions.

## What It Does

- Email/password sign up and login with JWT authentication
- Google sign-in through Firebase Authentication
- Group creation and member management
- Expense creation with equal, exact, percentage, and shares splits
- Balance tracking for who owes whom inside a group
- Settlement recording between users in a group
- Password reset flow with email delivery
- Assistant-guided flows for creating expenses and settling up

## Tech Stack

- Java 25
- Spring Boot 4.0.5
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL on Neon
- JWT with JJWT
- Firebase Admin SDK
- Spring Mail
- Spring Boot Actuator
- Maven
- Docker

## Core API Reference

Base URL: `http://localhost:8080`

### Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`

### Users

- `GET /api/v1/users/{id}`
- `GET /api/v1/users/email/{email}`
- `POST /api/v1/users/groups?creatorUserId={userId}`
- `GET /api/v1/users/groups?userId={userId}`

### Groups

- `GET /api/v1/groups/{groupId}`
- `GET /api/v1/groups/user/{userId}`
- `POST /api/v1/groups/{groupId}/members?userId={userId}`
- `GET /api/v1/groups/{groupId}/members`
- `DELETE /api/v1/groups/{groupId}?requesterUserId={id}`

### Expenses

- `POST /api/v1/expenses`
- `GET /api/v1/expenses/{expenseId}`
- `GET /api/v1/expenses/group/{groupId}`
- `DELETE /api/v1/expenses/{expenseId}`

### Balances

- `GET /api/v1/balances/group/{groupId}`
- `GET /api/v1/balances/group/{groupId}/settlements`
- `GET /api/v1/balances/group/{groupId}/me`

### Settlements

- `POST /api/v1/settlements`
- `GET /api/v1/settlements/group/{groupId}`

### Dashboard

- `GET /api/v1/dashboard/me`

### Assistant

- `POST /api/v1/assistant/chat`

## API Rules

- All protected routes require `Authorization: Bearer <token>`.
- Only `/api/auth/**` is public.
- Settlement actions are restricted to the payer/collector.
- Expense creation and deletion are permission-based.
- Split types supported: `EQUAL`, `EXACT`, `PERCENTAGE`, `SHARES`.
- Group and membership checks are enforced before expense or settlement writes.

## Current Functionalities

### Authentication

- Register and login with JWT
- Google OAuth sign-in using Firebase ID tokens
- Password reset email flow

### Groups and Members

- Create groups
- Add and list members
- Delete groups with authorization checks

### Expenses

- Add expenses to a group
- Split amounts equally, exactly, by percentage, or by shares
- Validate split totals and member eligibility
- Delete expenses with permission checks

### Balances and Settlements

- View balances per group
- View settlement suggestions
- Settle dues between users
- Support collector-style settlement where the payer can collect multiple debts in one request
- Keep settlement history per group

### Assistant

- AI chat helper for finance actions
- Guided expense creation flow
- Guided settle-up confirmation flow
- Conversation history support

## Deployment

### Frontend

- Hosted on Vercel
- URL: https://splitwise-frontend-pink.vercel.app

### Backend

- Ready for Render deployment with Docker
- Uses environment variables for database, JWT, Firebase, mail, and assistant settings
- Health endpoint exposed at `/actuator/health`

## Environment Variables

Set these in Render or your local environment as needed:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGINS`
- `FIREBASE_SERVICE_ACCOUNT_PATH`
- `APP_PASSWORD_RESET_BASE_URL`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `APP_MAIL_FROM`
- `ASSISTANT_ENABLED`
- `ASSISTANT_API_KEY`
- `ASSISTANT_PROVIDER`
- `ASSISTANT_BASE_URL`

## Local Setup

### Prerequisites

- Java 25
- Maven 3.9+
- PostgreSQL 15+

### Run Locally

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

### Build JAR

```bash
mvn clean package -DskipTests
java -jar target/splitwise-backend-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
docker build -t splitwise-backend:latest .
docker run --env-file .env -p 8080:8080 splitwise-backend:latest
```

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health check: `http://localhost:8080/actuator/health`
- Detailed request/response examples: `API_DOCUMENTATION.md`

## Notes for Reviewers

- The project uses JWT for session authentication.
- Firebase is used only for Google sign-in token verification.
- Mail health checks are disabled to avoid container startup delays.
- The backend is designed to work with the public Vercel frontend and Render deployment.
