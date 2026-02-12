# Core Ledger Service

Simple Core Banking Ledger built with Spring Boot to replace complex Apache Fineract.

## Features

- ✅ **Savings Accounts** (Debit Cards) - Balance management
- ✅ **Loan Accounts** (Credit Cards) - Credit limit tracking
- ✅ **Transaction History** - Full audit trail
- ✅ **Fineract-Compatible API** - Drop-in replacement for CMS integration

## Tech Stack

- Java 17
- Spring Boot 3.2.2
- PostgreSQL 15
- JPA/Hibernate
- Lombok

## Quick Start

### 1. Start Database
```powershell
cd d:\Code\DoAn\DATN\core-ledger-service
docker-compose up -d
```

### 2. Run Service
```powershell
./gradlew bootRun
```

Service will start on **port 8083**.

### 3. Test APIs
```powershell
# Create test account
./test-ledger-api.ps1
```

---

## API Endpoints

### Savings Accounts (Debit Cards)

#### Get Account Balance
```http
GET /savingsaccounts/{accountId}
```

Response:
```json
{
  "id": 1,
  "accountNumber": "ACC001",
  "accountBalance": 10000.0,
  "currency": {"code": "USD"},
  "status": {"value": "ACTIVE"}
}
```

#### Withdraw
```http
POST /savingsaccounts/{accountId}/transactions?command=withdrawal
Content-Type: application/json

{
  "locale": "en",
  "dateFormat": "dd MMMM yyyy",
  "transactionDate": "12 February 2026",
  "transactionAmount": 100.0
}
```

#### Create Account (Admin)
```http
POST /savingsaccounts
Content-Type: application/json

{
  "accountNumber": "ACC001",
  "balance": 10000.0,
  "clientName": "John Doe"
}
```

---

### Loan Accounts (Credit Cards)

#### Get Loan Info
```http
GET /loans/{loanId}
```

Response:
```json
{
  "id": 1,
  "accountNumber": "LOAN001",
  "principal": 50000.0,
  "principalOutstanding": 1000.0,
  "currency": {"code": "USD"}
}
```

#### Add Charge (Credit Card Transaction)
```http
POST /loans/{loanId}/charges
Content-Type: application/json

{
  "amount": 100.0,
  "dueDate": "12 February 2026",
  "locale": "en",
  "dateFormat": "dd MMMM yyyy"
}
```

#### Create Loan Account (Admin)
```http
POST /loans
Content-Type: application/json

{
  "accountNumber": "LOAN001",
  "principal": 50000.0,
  "clientName": "Jane Smith"
}
```

---

## Integration with CMS Service

Update `d:\Code\DoAn\DATN\cms-service\src\main\resources\application.yml`:

```yaml
fineract:
  base-url: http://localhost:8083  # Changed from Fineract
  tenant-id: default
  username: admin
  password: admin
  use-mock: false
```

**No code changes needed in `FineractClient.java`** - API is compatible!

---

## Database Schema

### Tables
- `savings_accounts` - Debit card accounts
- `loan_accounts` - Credit card accounts  
- `transactions` - Transaction history

### Access Database
- **PostgreSQL**: `localhost:5434`
- **pgAdmin**: `http://localhost:5051`
  - Email: `admin@bkbank.com`
  - Password: `admin`

---

## Testing

### Unit Tests
```powershell
./gradlew test
```

### API Tests
```powershell
./test-ledger-api.ps1
```

### E2E Test
See: `test-e2e-flow.ps1`

---

## Project Structure

```
core-ledger-service/
├── entity/
│   ├── SavingsAccount.java
│   ├── LoanAccount.java
│   └── Transaction.java
├── repository/
│   ├── SavingsAccountRepository.java
│   ├── LoanAccountRepository.java
│   └── TransactionRepository.java
├── service/
│   ├── SavingsAccountService.java
│   └── LoanAccountService.java
└── controller/
    ├── SavingsAccountController.java
    └── LoanAccountController.java
```

---

## Comparison with Fineract

| Feature | Fineract | Core Ledger Service |
|---------|----------|---------------------|
| Files | 6500+ | ~20 |
| Setup Time | Hours | Minutes |
| Features | Full core banking | Only essentials |
| Complexity | High | Low |
| For Production | ✅ | For education/demo |

**Perfect for your thesis project!** 🎓
