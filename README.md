# Splitwise Backend

A RESTful expense splitting backend built with Java 21 and Spring Boot.
Supports group creation, expense tracking, debt calculation, and
settlement recording with JWT authentication.

---

## Tech Stack

| Layer          | Technology                        |
|----------------|-----------------------------------|
| Language       | Java 21                           |
| Framework      | Spring Boot 3.3.x                |
| Database       | PostgreSQL                        |
| Security       | Spring Security + JWT (JJWT)      |
| ORM            | Spring Data JPA + Hibernate       |
| Validation     | Jakarta Validation                |
| Boilerplate    | Lombok                            |
| API Docs       | SpringDoc OpenAPI (Swagger UI)    |
| Build Tool     | Maven                             |

---

## Project Structure

```text
src/main/java/com/splitwise/
├── config/
│   └── SecurityConfig.java
├── controllers/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── GroupController.java
│   ├── ExpenseController.java
│   ├── SettlementController.java
│   ├── BalanceController.java
│   └── GlobalExceptionHandler.java
├── dto/
│   ├── request/
│   │   ├── AuthRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── GroupRequest.java
│   │   ├── ExpenseRequest.java
│   │   ├── ExpenseSplitRequest.java
│   │   └── SettlementRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── GroupResponse.java
│       ├── ExpenseResponse.java
│       ├── ExpenseSplitResponse.java
│       └── SettlementResponse.java
├── filters/
│   └── JwtAuthFilter.java
├── models/
│   ├── User.java
│   ├── Group.java
│   ├── GroupMember.java
│   ├── Expense.java
│   ├── ExpenseSplit.java
│   ├── Settlement.java
│   └── enums/
│       ├── Role.java
│       └── SplitType.java
├── repositories/
│   ├── UserRepository.java
│   ├── GroupRepository.java
│   ├── GroupMemberRepository.java
│   ├── ExpenseRepository.java
│   ├── ExpenseSplitRepository.java
│   └── SettlementRepository.java
├── services/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── GroupService.java
│   ├── ExpenseService.java
│   ├── SettlementService.java
│   └── BalanceService.java
└── utils/
    └── JwtUtil.java
```

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 15+

### Database Setup

Create the database in PostgreSQL before running:
```sql
CREATE DATABASE splitwise_db;
```

### Configuration

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/splitwise_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

jwt.secret=c3BsaXR3aXNlLXNlY3JldC1rZXktZm9yLWp3dC1hdXRoZW50aWNhdGlvbi0yMDI0
jwt.expiration=86400000

server.port=8080
```

### Run the Project
```bash
mvn spring-boot:run
```

Server starts at: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## Authentication

All routes except `/api/auth/**` require a Bearer token.

Include this header on every protected request:
Authorization: Bearer <token>

---

## API Reference

Base URL: `http://localhost:8080`

---

### Auth

#### Register
POST /api/auth/register
Request body:
```json
{
  "name": "Split Wise",
  "email": "splitwise@gmail.com",
  "password": "123456",
  "avatarUrl": null
}
```
Response `201 Created`:
```json
{
  "token": "<jwt-token>",
  "userId": 1,
  "name": "Split Wise",
  "email": "splitwise@gmail.com",
  "role": "MEMBER"
}
```

#### Login
POST /api/auth/login
Request body:
```json
{
  "email": "splitwise@gmail.com",
  "password": "123456"
}
```
Response `200 OK`:
```json
{
  "token": "<jwt-token>",
  "userId": 1,
  "name": "Split Wise",
  "email": "splitwise@gmail.com",
  "role": "MEMBER"
}
```

---

### Users

#### Get user by ID
GET /api/v1/users/{id}
Response `200 OK`:
```json
{
  "id": 1,
  "name": "Split Wise",
  "email": "splitwise@gmail.com",
  "avatarUrl": null
}
```

#### Get user by email
GET /api/v1/users/email/{email}

#### Create group for user
POST /api/v1/users/groups?creatorUserId={userId}
Request body:
```json
{
  "name": "Goa Trip",
  "description": "Trip expenses"
}
```
Response `201 Created`:
```json
{
  "id": 1,
  "name": "Goa Trip",
  "description": "Trip expenses",
  "createdBy": {
    "id": 1,
    "name": "Split Wise",
    "email": "splitwise@gmail.com",
    "avatarUrl": null
  },
  "createdAt": "2026-04-06T19:50:31.645791"
}
```

#### Get all groups for user
GET /api/v1/users/groups?userId={userId}

---

### Groups

#### Get group by ID
GET /api/v1/groups/{groupId}

#### Get groups by user
GET /api/v1/groups/user/{userId}

#### Add member to group
POST /api/v1/groups/{groupId}/members?userId={userId}

---

### Expenses

#### Add expense
POST /api/v1/expenses
Request body:
```json
{
  "groupId": 1,
  "paidByUserId": 1,
  "description": "Dinner bill",
  "amount": 1000,
  "splitType": "EXACT",
  "splits": [
    { "userId": 1, "owedAmount": 500 },
    { "userId": 2, "owedAmount": 500 }
  ]
}
```
Response `201 Created`:
```json
{
  "id": 1,
  "groupId": 1,
  "paidBy": { "id": 1, "name": "Split Wise", "email": "splitwise@gmail.com", "avatarUrl": null },
  "description": "Dinner bill",
  "amount": 1000,
  "splitType": "EXACT",
  "splits": [
    { "id": 1, "user": { "id": 1, "name": "Split Wise", "email": "splitwise@gmail.com", "avatarUrl": null }, "owedAmount": 500, "settled": false },
    { "id": 2, "user": { "id": 2, "name": "Harish S", "email": "harishs@gmail.com", "avatarUrl": null }, "owedAmount": 500, "settled": false }
  ],
  "createdAt": "2026-04-06T21:42:06.256"
}
```

#### Get expense by ID
GET /api/v1/expenses/{expenseId}

#### Get all expenses in a group
GET /api/v1/expenses/group/{groupId}

---

### Balances

#### Get net balances for a group
GET /api/v1/balances/group/{groupId}
Response `200 OK`:
```json
{
  "1": 500.00,
  "2": -500.00
}
```
Positive = user is owed money. Negative = user owes money.

#### Get minimum settlement suggestions
GET /api/v1/balances/group/{groupId}/settlements
Returns minimum list of payments to clear all debts in the group.
This is read-only and does not record any settlement.

---

### Settlements

#### Record a settlement
POST /api/v1/settlements
Request body:
```json
{
  "groupId": 1,
  "payerId": 2,
  "receiverId": 1,
  "amount": 500
}
```
Response `201 Created`:
```json
{
  "id": 1,
  "groupId": 1,
  "payer": { "id": 2, "name": "Harish S", "email": "harishs@gmail.com", "avatarUrl": null },
  "receiver": { "id": 1, "name": "Split Wise", "email": "splitwise@gmail.com", "avatarUrl": null },
  "amount": 500,
  "settledAt": "2026-04-06T21:45:17.760"
}
```

#### Get all settlements in a group
GET /api/v1/settlements/group/{groupId}

---

## Validation Rules

- Expense amount must be greater than zero
- Expense splits list must have at least one item
- Each split owedAmount must be greater than zero
- Sum of all split amounts must exactly equal total expense amount
- Expense payer and all split users must be members of the group
- Settlement amount must be greater than zero
- Settlement payer and receiver cannot be the same user
- Settlement payer and receiver must be members of the group
- Settlement amount must match the payer's total pending unsettled balance

---

## Split Types

| Type        | Description                              |
|-------------|------------------------------------------|
| EQUAL       | Total divided equally among all members  |
| EXACT       | Each member owes a specific fixed amount |
| PERCENTAGE  | Each member owes a percentage of total   |
| SHARES      | Split by ratio of shares per member      |

---

## Postman Test Order

Use `Authorization: Bearer <token>` on all requests below:

POST   /api/auth/register
POST   /api/auth/login
POST   /api/v1/users/groups?creatorUserId=1
POST   /api/v1/groups/1/members?userId=2
POST   /api/v1/expenses
GET    /api/v1/expenses/1
GET    /api/v1/expenses/group/1
GET    /api/v1/balances/group/1
GET    /api/v1/balances/group/1/settlements
POST   /api/v1/settlements
GET    /api/v1/settlements/group/1
GET    /api/v1/balances/group/1
GET    /api/v1/balances/group/1/settlements

---

## Negative Test Cases

| Request                          | Expected Result        |
|----------------------------------|------------------------|
| Split sum not equal to amount    | 400 validation error   |
| Non-member user in splits        | 400 business error     |
| payerId equals receiverId        | 400 business error     |
| Settlement amount does not match | 400 business error     |
| Missing Authorization header     | 403 Forbidden          |
| Invalid or expired JWT token     | 403 Forbidden          |

---

## Future Scope

- Google ADK chatbot integration for natural language expense entry
- MongoDB for chat history and activity feed
- Firebase push notifications
- Receipt scanning with AI
- Multi-currency support
- Frontend (React / Flutter)

---
