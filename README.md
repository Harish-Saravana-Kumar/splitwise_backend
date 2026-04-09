# Splitwise Backend

A RESTful expense splitting backend built with Java and Spring Boot.
Supports group creation, expense tracking, debt calculation,
settlement recording, and an assistant workflow for guided expense/settle-up actions.

---

## Tech Stack

| Layer          | Technology                        |
|----------------|-----------------------------------|
| Language       | Java 25                           |
| Framework      | Spring Boot 4.0.5                |
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
├── assistant/
│   ├── config/
│   │   └── AssistantProperties.java
│   ├── context/
│   │   └── AssistantUserContext.java
│   ├── controller/
│   │   └── AssistantController.java
│   ├── dto/
│   │   ├── AssistantChatRequest.java
│   │   └── AssistantChatResponse.java
│   ├── model/
│   │   ├── AssistantActionType.java
│   │   ├── AssistantChatMessage.java
│   │   ├── AssistantConversation.java
│   │   ├── AssistantMessageRole.java
│   │   └── PendingAssistantAction.java
│   ├── repository/
│   │   ├── AssistantChatMessageRepository.java
│   │   └── AssistantConversationRepository.java
│   ├── service/
│   │   ├── AssistantAgentService.java
│   │   └── AssistantPendingActionService.java
│   └── tools/
│       └── SplitwiseAssistantTools.java
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
│       ├── SettlementResponse.java
│       └── SettlementBalanceResponse.java
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

- Java 25
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

# Assistant (Groq/OpenAI-compatible)
assistant.enabled=true
assistant.provider=groq
assistant.model=llama-3.1-8b-instant
assistant.api-key=${GROQ_API_KEY:}
assistant.base-url=https://api.groq.com/openai/v1

langchain4j.open-ai.chat-model.base-url=https://api.groq.com/openai/v1
langchain4j.open-ai.chat-model.api-key=${GROQ_API_KEY:}
langchain4j.open-ai.chat-model.model-name=llama-3.1-8b-instant
```

Set your Groq key in environment variables (recommended, do not hardcode):

```powershell
$env:GROQ_API_KEY="<your_groq_api_key>"
```

### Assistant Setup Checklist

1. Keep assistant keys in environment variables, not in tracked files.
2. Ensure these values exist in `src/main/resources/application.properties`:
  - `assistant.enabled=true`
  - `assistant.provider=groq`
  - `assistant.base-url=https://api.groq.com/openai/v1`
  - `assistant.api-key=${GROQ_API_KEY:}`
3. Start backend and verify assistant endpoint:
  - `POST /api/v1/assistant/chat` (with Bearer token)
4. Use guided actions for write operations:
  - create expense -> confirm token
  - settle up -> confirm token

### Run the Project
```bash
mvn spring-boot:run
```

Server starts at: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## Reviewed Updates (Post Frontend Build)

This section summarizes the major logic and behavior updates currently implemented after frontend integration and review.

### Backend Logic Updates

- Group delete authorization is enforced: only the group creator can delete a group.
- Group deletion performs cleanup of related data (group members, expenses, expense splits, settlements).
- Group members API added: `GET /api/v1/groups/{groupId}/members`.
- Group delete API added: `DELETE /api/v1/groups/{groupId}?requesterUserId={id}`.
- Expense creation is now tied to authenticated user context (requester email from auth token).
- Only group members can add expenses.
- Payer must be a member of the target group.
- Split logic enhanced:
  - `EQUAL` auto-splits across all group members.
  - `PERCENTAGE` validates total is exactly `100` and allocates amounts with rounding-safe distribution.
  - `SHARES` validates positive shares and allocates amounts by share weights.
  - Duplicate split users are rejected.
  - Non-member split users are rejected.
- Expense deletion authorization added: only expense creator or group creator can delete an expense.
- Expense delete API added: `DELETE /api/v1/expenses/{expenseId}`.
- Settlement authorization tightened: only payer or receiver can perform settle-up.
- Settlement amount validation ensures amount matches payer pending balance in that group.
- Dashboard API added: `GET /api/v1/dashboard/me` with:
  - total paid,
  - total owes,
  - group-wise net summary,
  - person-to-person net balances.
- Assistant API added: `POST /api/v1/assistant/chat` with:
  - normal assistant chat,
  - guided expense form flow,
  - guided settle-up flow,
  - explicit confirmation-token execution,
  - previous-question recall from persisted chat history.

### Frontend + Behavior Updates

- Auth flow now defaults to dashboard after login/register.
- Header navigation updated to include Dashboard and Groups.
- Profile popover added in header with user details and logout.
- Group detail page updates:
  - creator-only delete group action,
  - member management modal,
  - members side panel via options menu.
- Expense list updates:
  - permission-based expense delete,
  - improved split status labels (`paid by`, `owes`, `settled`).
- Add Expense modal updates:
  - better alignment and responsive behavior,
  - explicit handling for `EQUAL`, `EXACT`, `PERCENTAGE`, and `SHARES` input flows.
- Balances page updates:
  - user names shown instead of only IDs,
  - settlement suggestions parser handles both object and ID payload shapes,
  - filter dropdown with default `Balance Debts`,
  - `Settlement Suggestions` and `Expense-Wise Member Splits` views,
  - settle button enabled only for payer/receiver.
- Dashboard page includes onboarding guidance for first-time users (empty-data state).
- Shared button hover effects were refined and centralized for core action buttons.

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

1. `POST /api/auth/register`
2. `POST /api/auth/login`
3. `POST /api/v1/users/groups?creatorUserId=1`
4. `POST /api/v1/groups/1/members?userId=2`
5. `POST /api/v1/expenses`
6. `GET /api/v1/expenses/1`
7. `GET /api/v1/expenses/group/1`
8. `GET /api/v1/balances/group/1`
9. `GET /api/v1/balances/group/1/settlements`
10. `POST /api/v1/settlements`
11. `GET /api/v1/settlements/group/1`
12. `GET /api/v1/balances/group/1`
13. `GET /api/v1/balances/group/1/settlements`

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

---

## Move to GitHub

The backend and frontend are separate Git repositories and should be pushed independently.

### 1. Push Backend Repository

```powershell
Set-Location h:\job\projects\splitwise-backend
git status
git add -A
git commit -m "feat: backend updates"
git push origin main
```

### 2. Push Frontend Repository

```powershell
Set-Location h:\job\projects\splitwise-backend\splitwise-frontend
git status
git add -A
git commit -m "feat: frontend updates"
git push origin main
```

### 3. Verify Ignore Rules Before Push

Backend `.gitignore` should include:
- `.env`
- `.env.*`
- `src/main/resources/application-local.properties`
- `src/main/resources/application-secrets.properties`

Frontend `.gitignore` should include:
- `.env`
- `.env.*`

---
