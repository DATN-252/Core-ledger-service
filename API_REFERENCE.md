# 📚 Core Ledger Service - API Reference

**Base URL**: `http://localhost:8083`  
**Version**: 2.0.0 (with Client Entity Support)  
**Format**: JSON

---

## 📋 Table of Contents

1. [Client Management APIs](#client-management-apis) ⭐ NEW
2. [Savings Accounts APIs](#savings-accounts-apis) (Debit Cards)
3. [Loan Accounts APIs](#loan-accounts-apis) (Credit Cards)
4. [Error Responses](#error-responses)
5. [Schema Management](#schema-management)

---

## Client Management APIs

**Version 2.0 Update**: Thay vì dùng `clientName` string, giờ có Client entity riêng với đầy đủ thông tin KYC.

### 1. Create Client

**Chức năng**: Tạo client mới với thông tin đầy đủ hoặc tối thiểu.

**Endpoint**: `POST /clients`

**Request Body (Minimal)**:
```json
{
  "clientId": "CLI_001",
  "fullName": "Nguyen Van A",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "email": "nguyenvana@example.com",
  "phoneNumber": "+84912345678",
  "address": "123 Test Street, Hanoi",
  "idNumber": "001234567890",
  "idType": "NATIONAL_ID"
}
```

**Request Body (Full)**:
```json
{
  "clientId": "CLI_002",
  "fullName": "Tran Thi B",
  "dateOfBirth": "1992-05-15",
  "gender": "FEMALE",
  "email": "tranthib@example.com",
  "phoneNumber": "+84987654321",
  "address": "456 Full Street",
  "city": "Ho Chi Minh",
  "country": "Vietnam",
  "idNumber": "002345678901",
  "idType": "NATIONAL_ID",
  "idIssueDate": "2018-01-01",
  "idExpiryDate": "2033-01-01",
  "occupation": "Software Engineer",
  "employerName": "FPT Software",
  "employerAddress": "FPT Tower, Hanoi",
  "employmentType": "FULL_TIME",
  "monthlyIncome": 30000000.0,
  "yearsAtCurrentJob": 4
}
```

**Required Fields**:
- `clientId`, `fullName`, `dateOfBirth`, `gender`
- `email`, `phoneNumber`, `address`
- `idNumber`, `idType`

**Optional Fields**:
- `city`, `country`
- `idIssueDate`, `idExpiryDate`
- `occupation`, `employerName`, `employerAddress`, `employmentType`, `monthlyIncome`, `yearsAtCurrentJob`

**Response**:
```json
{
  "clientId": "CLI_001",
  "fullName": "Nguyen Van A",
  "status": "ACTIVE",
  "message": "Client created successfully"
}
```

---

### 2. Get Client Details

**Endpoint**: `GET /clients/{clientId}`

**Response**:
```json
{
  "clientId": "CLI_002",
  "fullName": "Tran Thi B",
  "dateOfBirth": "1992-05-15",
  "gender": "FEMALE",
  "email": "tranthib@example.com",
  "phoneNumber": "+84987654321",
  "address": "456 Full Street",
  "city": "Ho Chi Minh",
  "country": "Vietnam",
  "idNumber": "002345678901",
  "idType": "NATIONAL_ID",
  "idIssueDate": "2018-01-01",
  "idExpiryDate": "2033-01-01",
  "idExpired": false,
  "idExpiringSoon": false,
  "occupation": "Software Engineer",
  "employerName": "FPT Software",
  "employerAddress": "FPT Tower, Hanoi",
  "employmentType": "FULL_TIME",
  "monthlyIncome": 30000000.0,
  "yearsAtCurrentJob": 4,
  "suggestedCreditLimit": 150000000.0,
  "status": "ACTIVE",
  "totalSavingsAccounts": 2,
  "totalLoanAccounts": 1,
  "createdAt": "2026-02-12T10:00:00",
  "updatedAt": "2026-02-12T10:30:00"
}
```

**Calculated Fields**:
- `idExpired`: true nếu ID đã hết hạn
- `idExpiringSoon`: true nếu ID hết hạn trong 6 tháng tới
- `suggestedCreditLimit`: Tính dựa trên monthlyIncome × factor (FULL_TIME=5x, PART_TIME=3x, etc.)

---

### 3. Update Client

**Endpoint**: `PUT /clients/{clientId}`

**Request Body** (all fields optional):
```json
{
  "occupation": "Senior Engineer",
  "monthlyIncome": 35000000.0,
  "employmentType": "FULL_TIME",
  "city": "Hanoi"
}
```

**Response**:
```json
{
  "clientId": "CLI_001",
  "message": "Client updated successfully"
}
```

---

### 4. Search Clients

**Endpoint**: `GET /clients/search?name={name}`

**Example**: `GET /clients/search?name=Nguyen`

**Response**:
```json
{
  "clients": [
    {
      "clientId": "CLI_001",
      "fullName": "Nguyen Van A",
      "email": "nguyenvana@example.com",
      "phoneNumber": "+84912345678",
      "status": "ACTIVE",
      "totalAccounts": 3
    }
  ],
  "total": 1,
  "searchTerm": "Nguyen"
}
```

---

### 5. Get All Active Clients

**Endpoint**: `GET /clients`

**Response**:
```json
{
  "clients": [ /* array of client summaries */ ],
  "total": 15
}
```

---

### 6. Get Client Accounts

**Endpoint**: `GET /clients/{clientId}/accounts`

**Response**:
```json
{
  "clientId": "CLI_001",
  "clientName": "Nguyen Van A",
  "savingsAccounts": [
    {
      "accountNumber": "SAV_001",
      "balance": 10000.0,
      "status": "ACTIVE"
    }
  ],
  "loanAccounts": [
    {
      "accountNumber": "LOAN_001",
      "principal": 50000.0,
      "principalOutstanding": 5000.0,
      "status": "ACTIVE"
    }
  ],
  "totalSavingsAccounts": 1,
  "totalLoanAccounts": 1
}
```

---

### 7. Get Client Savings Accounts

**Endpoint**: `GET /clients/{clientId}/savings`

---

### 8. Get Client Loan Accounts

**Endpoint**: `GET /clients/{clientId}/loans`

---

### 9. Delete Client (Soft Delete)

**Endpoint**: `DELETE /clients/{clientId}`

**Response**:
```json
{
  "clientId": "CLI_001",
  "message": "Client deactivated successfully"
}
```

**Note**: Soft delete - client status → INACTIVE, không xóa khỏi database.

---

## Savings Accounts APIs

⚠️ **Version 2.0 Changes**: Account creation now requires `clientId` instead of `clientName`.

### 1. Get Account Details

**Endpoint**: `GET /savingsaccounts/{accountId}`

**Response**:
```json
{
  "id": 1,
  "accountNo": "ACC_001",
  "accountNumber": "ACC_001",
  "accountBalance": 10000.0,
  "currency": {
    "code": "USD"
  },
  "status": {
    "value": "ACTIVE"
  },
  "clientName": "Nguyen Van A"
}
```

**Note**: `clientName` vẫn có trong response để backward compatibility.

---

### 2. Withdraw Money

**Endpoint**: `POST /savingsaccounts/{accountId}/transactions?command=withdrawal`

**Request Body**:
```json
{
  "locale": "en",
  "dateFormat": "dd MMMM yyyy",
  "transactionDate": "12 February 2026",
  "transactionAmount": 100.0
}
```

**Response**:
```json
{
  "resourceId": 1,
  "changes": {
    "accountBalance": 9900.0
  }
}
```

**Business Logic**:
- ✅ Account phải ACTIVE
- ✅ Balance >= amount
- ✅ Save transaction log

---

### 3. Deposit Money

**Endpoint**: `POST /savingsaccounts/{accountId}/transactions?command=deposit`

*(Same format as Withdraw)*

---

### 4. Create Savings Account (Admin) ⚠️ UPDATED

**Endpoint**: `POST /savingsaccounts`

**Old Request (v1.0)**:
```json
{
  "accountNumber": "ACC_001",
  "balance": 10000.0,
  "clientName": "John Doe"  ❌ DEPRECATED
}
```

**New Request (v2.0)**:
```json
{
  "accountNumber": "ACC_001",
  "balance": 10000.0,
  "clientId": "CLI_001"  ✅ REQUIRED
}
```

**Response**:
```json
{
  "resourceId": 1,
  "accountNumber": "ACC_001",
  "clientId": "CLI_001",
  "clientName": "Nguyen Van A"
}
```

**Lưu ý**: Clients phải được tạo trước khi tạo accounts.

---

## Loan Accounts APIs

⚠️ **Version 2.0 Changes**: Loan creation now requires `clientId`.

### 5. Get Loan Details

**Endpoint**: `GET /loans/{loanId}`

**Response**:
```json
{
  "id": 1,
  "accountNumber": "LOAN_001",
  "principal": 50000.0,
  "principalOutstanding": 1000.0,
  "currency": {
    "code": "USD"
  },
  "status": {
    "value": "ACTIVE"
  },
  "clientName": "Jane Smith"
}
```

---

### 6. Add Charge to Loan

**Endpoint**: `POST /loans/{loanId}/charges`

**Request Body**:
```json
{
  "amount": 1000.0,
  "dueDate": "12 February 2026",
  "locale": "en",
  "dateFormat": "dd MMMM yyyy"
}
```

**Response**:
```json
{
  "resourceId": 1,
  "changes": {
    "principalOutstanding": 2000.0
  }
}
```

---

### 7. Make Payment to Loan

**Endpoint**: `POST /loans/{loanId}/payments`

---

### 8. Create Loan Account (Admin) ⚠️ UPDATED

**Endpoint**: `POST /loans`

**New Request (v2.0)**:
```json
{
  "accountNumber": "LOAN_001",
  "principal": 50000.0,
  "clientId": "CLI_001"  ✅ REQUIRED (was clientName)
}
```

**Response**:
```json
{
  "resourceId": 1,
  "accountNumber": "LOAN_001",
  "principal": 50000.0,
  "clientId": "CLI_001",
  "clientName": "Nguyen Van A"
}
```

---

## Error Responses

```json
{
  "error": "Error message description"
}
```

**Common Errors**:
- `Client not found: CLI_XXX` → 400 Bad Request
- `Client ID already exists` → 400 Bad Request
- `Email already exists` → 400 Bad Request
- `ID number already exists` → 400 Bad Request
- `Insufficient balance` → 400 Bad Request
- `Credit limit exceeded` → 400 Bad Request

---

## Schema Management

Schema hiện được quản lý trực tiếp bởi JPA/Hibernate.

- `spring.jpa.hibernate.ddl-auto=update`
- Khi entity thay đổi, Hibernate sẽ tự đồng bộ schema khi service khởi động
- Không cần chạy SQL migration thủ công trong flow local/dev hiện tại

**Account creation flow**:

```java
// 1. Create client first
POST /clients
{ "clientId": "CLI_001", "fullName": "John", ... }

// 2. Then create account with clientId
POST /savingsaccounts
{ "accountNumber": "ACC_001", "clientId": "CLI_001" }
```

---

## 📊 Updated Database Schema

### clients (NEW)
```sql
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(50) UNIQUE NOT NULL,
    -- Required fields
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address VARCHAR(500) NOT NULL,
    id_number VARCHAR(50) UNIQUE NOT NULL,
    id_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- Optional fields
    city VARCHAR(100),
    country VARCHAR(100),
    id_issue_date DATE,
    id_expiry_date DATE,
    occupation VARCHAR(255),
    employer_name VARCHAR(255),
    employer_address VARCHAR(500),
    employment_type VARCHAR(20),
    monthly_income DOUBLE PRECISION,
    years_at_current_job INTEGER,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

### savings_accounts (UPDATED)
```sql
CREATE TABLE savings_accounts (
  id BIGSERIAL PRIMARY KEY,
  account_number VARCHAR(255) UNIQUE NOT NULL,
  balance DOUBLE PRECISION NOT NULL,
  currency VARCHAR(10) DEFAULT 'USD',
  status VARCHAR(20) DEFAULT 'ACTIVE',
  client_id BIGINT NOT NULL,  -- NEW: FK to clients
  client_name VARCHAR(255),   -- DEPRECATED: for backward compat
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (client_id) REFERENCES clients(id)
);
```

### loan_accounts (UPDATED)
```sql
CREATE TABLE loan_accounts (
  id BIGSERIAL PRIMARY KEY,
  account_number VARCHAR(255) UNIQUE NOT NULL,
  principal DOUBLE PRECISION NOT NULL,
  principal_outstanding DOUBLE PRECISION DEFAULT 0,
  currency VARCHAR(10) DEFAULT 'USD',
  status VARCHAR(20) DEFAULT 'ACTIVE',
  client_id BIGINT NOT NULL,  -- NEW: FK to clients
  client_name VARCHAR(255),   -- DEPRECATED
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (client_id) REFERENCES clients(id)
);
```

---

## 🧪 Testing

### Test Client APIs
```powershell
.\test-client-api.ps1
```

### Test Account APIs (Updated)
```powershell
# 1. Create client
$client = @{
    clientId = "CLI_TEST_001"
    fullName = "Test User"
    dateOfBirth = "1990-01-01"
    gender = "MALE"
    email = "test@example.com"
    phoneNumber = "+84901234567"
    address = "Test Address"
    idNumber = "001234567890"
    idType = "NATIONAL_ID"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8083/clients" -Method Post -Body $client -ContentType "application/json"

# 2. Create savings account with clientId
$account = @{
    accountNumber = "SAV_TEST_001"
    balance = 10000.0
    clientId = "CLI_TEST_001"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8083/savingsaccounts" -Method Post -Body $account -ContentType "application/json"
```

---

## 📝 API Summary Table

| Method | Endpoint | Version | Description |
|--------|----------|---------|-------------|
| **Client Management** | | | |
| POST | `/clients` | 2.0 | Create client |
| GET | `/clients/{clientId}` | 2.0 | Get client details |
| PUT | `/clients/{clientId}` | 2.0 | Update client |
| DELETE | `/clients/{clientId}` | 2.0 | Soft delete client |
| GET | `/clients` | 2.0 | List all clients |
| GET | `/clients/search?name=X` | 2.0 | Search clients |
| GET | `/clients/{id}/accounts` | 2.0 | Get client accounts |
| GET | `/clients/{id}/savings` | 2.0 | Get savings accounts |
| GET | `/clients/{id}/loans` | 2.0 | Get loan accounts |
| **Savings Accounts** | | | |
| GET | `/savingsaccounts/{id}` | 1.0 | Get account |
| POST | `/savingsaccounts/{id}/transactions?command=withdrawal` | 1.0 | Withdraw |
| POST | `/savingsaccounts/{id}/transactions?command=deposit` | 1.0 | Deposit |
| POST | `/savingsaccounts` | 2.0 | Create account (needs clientId) |
| **Loan Accounts** | | | |
| GET | `/loans/{id}` | 1.0 | Get loan |
| POST | `/loans/{id}/charges` | 1.0 | Add charge |
| POST | `/loans/{id}/payments` | 1.0 | Make payment |
| POST | `/loans` | 2.0 | Create loan (needs clientId) |

**Total**: 18 endpoints (9 new Client APIs + 9 existing)
