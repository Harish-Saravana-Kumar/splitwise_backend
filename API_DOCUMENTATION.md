# Splitwise Backend API Documentation

Base URL: `http://localhost:8080`

## Authentication

Use JWT for all protected APIs.

Header for protected requests:
`Authorization: Bearer <token>`

Only `/api/auth/**` is public. All other routes require Bearer token.

### 1. Login

### Endpoint
`POST /api/auth/login`

### Full URL
`http://localhost:8080/api/auth/login`

### Request Body
```json
{
  "email": "splitwise@gmail.com",
  "password": "123456"
}
```

### Success Response (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJzcGxpdHdpc2VAZ21haWwuY29tIiwiaWF0IjoxNzc1NDgzNjc4LCJleHAiOjE3NzU1NzAwNzh9.wn6lH2w-vN-5p35Y68No2575AmXrIHWUhLypqgjCiQ2tAx_Mbg4-sd78a00YasuM",
  "userId": 1,
  "name": "Split Wise",
  "email": "splitwise@gmail.com",
  "role": "MEMBER"
}
```

### 2. Register

### Endpoint
`POST /api/auth/register`

### Full URL
`http://localhost:8080/api/auth/register`

### Request Body
```json
{
  "name": "Split Wise",
  "email": "splitwise@gmail.com",
  "password": "123456",
  "avatarUrl": null
}
```

### Success Response (201 Created)
```json
{
  "token": "<jwt-token>",
  "userId": 1,
  "name": "Split Wise",
  "email": "splitwise@gmail.com",
  "role": "MEMBER"
}
```

## 3. UserController

### 3.1 Get User By ID

### Endpoint
`GET /api/v1/users/{id}`

### Full URL
`http://localhost:8080/api/v1/users/1`

### Success Response (200 OK)
```json
{
  "id": 1,
  "name": "Split Wise",
  "email": "splitwise@gmail.com",
  "avatarUrl": null
}
```

### 3.2 Get User By Email

### Endpoint
`GET /api/v1/users/email/{email}`

### Full URL
`http://localhost:8080/api/v1/users/email/splitwise@gmail.com`

### Success Response (200 OK)
```json
{
  "id": 1,
  "name": "Split Wise",
  "email": "splitwise@gmail.com",
  "avatarUrl": null
}
```

### 3.3 Create Group For User

### Endpoint
`POST /api/v1/users/groups?creatorUserId={userId}`

### Full URL
`http://localhost:8080/api/v1/users/groups?creatorUserId=1`

### Request Body
```json
{
  "name": "turf payalugah",
  "description": "this is a turf group description, "
}
```

### Success Response (201 Created)
```json
{
  "id": 1,
  "name": "turf payalugah",
  "description": "this is a turf group description, ",
  "createdBy": {
    "id": 1,
    "name": "Split Wise",
    "email": "splitwise@gmail.com",
    "avatarUrl": null
  },
  "createdAt": "2026-04-06T19:50:31.6457907"
}
```

### 3.4 Get Groups By User

### Endpoint
`GET /api/v1/users/groups?userId={userId}`

### Full URL
`http://localhost:8080/api/v1/users/groups?userId=1`

### Success Response (200 OK)
```json
[
  {
    "id": 1,
    "name": "turf payalugah",
    "description": "this is a turf group description, ",
    "createdBy": {
      "id": 1,
      "name": "Split Wise",
      "email": "splitwise@gmail.com",
      "avatarUrl": null
    },
    "createdAt": "2026-04-06T19:50:31.645791"
  }
]
```

## 4. GroupController

### 4.1 Get Group By ID

### Endpoint
`GET /api/v1/groups/{groupId}`

### Full URL
`http://localhost:8080/api/v1/groups/1`

### Success Response (200 OK)
```json
{
  "id": 1,
  "name": "turf payalugah",
  "description": "this is a turf group description, ",
  "createdBy": {
    "id": 1,
    "name": "Split Wise",
    "email": "splitwise@gmail.com",
    "avatarUrl": null
  },
  "createdAt": "2026-04-06T19:50:31.645791"
}
```

### 4.2 Get Groups By User

### Endpoint
`GET /api/v1/groups/user/{userId}`

### Full URL
`http://localhost:8080/api/v1/groups/user/1`

### Success Response (200 OK)
```json
[
  {
    "id": 1,
    "name": "turf payalugah",
    "description": "this is a turf group description, ",
    "createdBy": {
      "id": 1,
      "name": "Split Wise",
      "email": "splitwise@gmail.com",
      "avatarUrl": null
    },
    "createdAt": "2026-04-06T19:50:31.645791"
  }
]
```

### 4.3 Add Member To Group

### Endpoint
`POST /api/v1/groups/{groupId}/members?userId={userId}`

### Full URL
`http://localhost:8080/api/v1/groups/1/members?userId=2`

### Success Response (200 OK)
```json
{
  "id": 1,
  "name": "turf payalugah",
  "description": "this is a turf group description, ",
  "createdBy": {
    "id": 1,
    "name": "Split Wise",
    "email": "splitwise@gmail.com",
    "avatarUrl": null
  },
  "createdAt": "2026-04-06T19:50:31.645791"
}
```

## 5. ExpenseController

### 5.1 Add Expense

### Endpoint
`POST /api/v1/expenses`

### Full URL
`http://localhost:8080/api/v1/expenses`

### Request Body
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

### Success Response (201 Created)
```json
{
  "id": 1,
  "groupId": 1,
  "paidBy": {
    "id": 1,
    "name": "Split Wise",
    "email": "splitwise@gmail.com",
    "avatarUrl": null
  },
  "description": "Dinner bill",
  "amount": 1000,
  "splitType": "EXACT",
  "splits": [
    {
      "id": 1,
      "user": {
        "id": 1,
        "name": "Split Wise",
        "email": "splitwise@gmail.com",
        "avatarUrl": null
      },
      "owedAmount": 500,
      "settled": false
    },
    {
      "id": 2,
      "user": {
        "id": 2,
        "name": "Harish S",
        "email": "harishs@gmail.com",
        "avatarUrl": null
      },
      "owedAmount": 500,
      "settled": false
    }
  ],
  "createdAt": "2026-04-06T21:42:06.2566298"
}
```

### 5.2 Get Expense By ID

### Endpoint
`GET /api/v1/expenses/{expenseId}`

### Full URL
`http://localhost:8080/api/v1/expenses/1`

### Success Response (200 OK)
```json
{
  "id": 1,
  "groupId": 1,
  "paidBy": {
    "id": 1,
    "name": "Split Wise",
    "email": "splitwise@gmail.com",
    "avatarUrl": null
  },
  "description": "Dinner bill",
  "amount": 1000.00,
  "splitType": "EXACT",
  "splits": [
    {
      "id": 1,
      "user": {
        "id": 1,
        "name": "Split Wise",
        "email": "splitwise@gmail.com",
        "avatarUrl": null
      },
      "owedAmount": 500.00,
      "settled": false
    },
    {
      "id": 2,
      "user": {
        "id": 2,
        "name": "Harish S",
        "email": "harishs@gmail.com",
        "avatarUrl": null
      },
      "owedAmount": 500.00,
      "settled": false
    }
  ],
  "createdAt": "2026-04-06T21:42:06.25663"
}
```

### 5.3 Get Expenses By Group

### Endpoint
`GET /api/v1/expenses/group/{groupId}`

### Full URL
`http://localhost:8080/api/v1/expenses/group/1`

### Success Response (200 OK)
```json
[
  {
    "id": 1,
    "groupId": 1,
    "paidBy": {
      "id": 1,
      "name": "Split Wise",
      "email": "splitwise@gmail.com",
      "avatarUrl": null
    },
    "description": "Dinner bill",
    "amount": 1000.00,
    "splitType": "EXACT",
    "splits": [
      {
        "id": 1,
        "user": {
          "id": 1,
          "name": "Split Wise",
          "email": "splitwise@gmail.com",
          "avatarUrl": null
        },
        "owedAmount": 500.00,
        "settled": false
      },
      {
        "id": 2,
        "user": {
          "id": 2,
          "name": "Harish S",
          "email": "harishs@gmail.com",
          "avatarUrl": null
        },
        "owedAmount": 500.00,
        "settled": false
      }
    ],
    "createdAt": "2026-04-06T21:42:06.25663"
  }
]
```

## 6. SettlementController

### 6.1 Settle Up

### Endpoint
`POST /api/v1/settlements`

### Full URL
`http://localhost:8080/api/v1/settlements`

### Request Body
```json
{
  "groupId": 1,
  "payerId": 2,
  "receiverId": 1,
  "amount": 500
}
```

### Success Response (201 Created)
```json
{
  "id": 1,
  "groupId": 1,
  "payer": {
    "id": 2,
    "name": "Harish S",
    "email": "harishs@gmail.com",
    "avatarUrl": null
  },
  "receiver": {
    "id": 1,
    "name": "Split Wise",
    "email": "splitwise@gmail.com",
    "avatarUrl": null
  },
  "amount": 500,
  "settledAt": "2026-04-06T21:45:17.7602488"
}
```

### 6.2 Get Settlements By Group

### Endpoint
`GET /api/v1/settlements/group/{groupId}`

### Full URL
`http://localhost:8080/api/v1/settlements/group/1`

### Success Response (200 OK)
```json
[
  {
    "id": 1,
    "groupId": 1,
    "payer": {
      "id": 2,
      "name": "Harish S",
      "email": "harishs@gmail.com",
      "avatarUrl": null
    },
    "receiver": {
      "id": 1,
      "name": "Split Wise",
      "email": "splitwise@gmail.com",
      "avatarUrl": null
    },
    "amount": 500.00,
    "settledAt": "2026-04-06T21:45:17.760249"
  }
]
```

## 7. BalanceController

### 7.1 Get Group Balances

### Endpoint
`GET /api/v1/balances/group/{groupId}`

### Full URL
`http://localhost:8080/api/v1/balances/group/1`

### Success Response (200 OK)
```json
{
  "1": 0.00
}
```

### 7.2 Get Minimum Settlement Suggestions

### Endpoint
`GET /api/v1/balances/group/{groupId}/settlements`

### Full URL
`http://localhost:8080/api/v1/balances/group/1/settlements`

### Success Response (200 OK)
```json
[]
```

## Logic Notes

- `splitType` is enum based and supports: `EQUAL`, `EXACT`, `PERCENTAGE`, `SHARES`.
- In current implementation, settlement marks all unsettled splits for `payerId` in that group as settled.
- Balance APIs operate on unsettled splits only.
- JSON number formatting can vary (`500` vs `500.00`) based on serializer/BigDecimal rendering.

## Validation Rules (Latest)

- Expense amount must be greater than zero.
- Expense `splits` list must contain at least one item.
- Each split `owedAmount` must be greater than zero.
- Sum of all split amounts must exactly match the total expense amount.
- Expense payer and all split users must be members of the group.
- Settlement amount must be greater than zero.
- Settlement payer and receiver cannot be the same user.
- Settlement payer and receiver must be members of the group.
- Settlement amount must exactly match the payer's pending unsettled total in the group.

## Postman Retest Checklist

Use Bearer token for all endpoints below:
`Authorization: Bearer <token>`

1. `POST http://localhost:8080/api/auth/login`
2. `POST http://localhost:8080/api/v1/users/groups?creatorUserId=1`
3. `POST http://localhost:8080/api/v1/groups/1/members?userId=2`
4. `POST http://localhost:8080/api/v1/expenses`
5. `GET http://localhost:8080/api/v1/expenses/1`
6. `GET http://localhost:8080/api/v1/expenses/group/1`
7. `GET http://localhost:8080/api/v1/balances/group/1`
8. `GET http://localhost:8080/api/v1/balances/group/1/settlements`
9. `POST http://localhost:8080/api/v1/settlements`
10. `GET http://localhost:8080/api/v1/settlements/group/1`
11. `GET http://localhost:8080/api/v1/balances/group/1`
12. `GET http://localhost:8080/api/v1/balances/group/1/settlements`

## Negative Tests (Expected Failures)

- `POST /api/v1/expenses` with split sum not equal to amount -> validation/business error.
- `POST /api/v1/expenses` with non-member split user -> business error.
- `POST /api/v1/settlements` with `payerId == receiverId` -> business error.
- `POST /api/v1/settlements` with partial/extra amount vs pending balance -> business error.
