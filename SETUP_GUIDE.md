# 🚀 Setup Guide - Core Ledger Service

## Step 1: Start Database

```powershell
cd d:\Code\DoAn\DATN\core-ledger-service
docker-compose up -d
```

**Nếu gặp lỗi "cannot find the file specified"**:
- Mở Docker Desktop
- Vào Settings → Resources → File Sharing
- Add đường dẫn: `d:\Code\DoAn\DATN\core-ledger-service`
- Apply & Restart
- Chạy lại `docker-compose up -d`

Verify database đã chạy:
```powershell
docker ps
# Should see: ledger-postgres running on port 5434
```

---

## Step 2: Build & Run Service

```powershell
cd d:\Code\DoAn\DATN\core-ledger-service
./gradlew bootRun
```

Wait for log message:
```
Started LedgerServiceApplication in X.XXX seconds
```

Service running on: **http://localhost:8083**

---

## Step 3: Test APIs

```powershell
# Run test script
./test-ledger-api.ps1
```

Expected output:
```
✅ Created savings account: TEST_ACC_001
✅ Account Balance: 10000.0
✅ Withdrawal successful
✅ Created loan account: TEST_LOAN_001
✅ Charge added successfully
```

---

## Step 4: Update CMS Service

Edit: `d:\Code\DoAn\DATN\cms-service\src\main\resources\application.yml`

```yaml
fineract:
  base-url: http://localhost:8083  # Changed from https://localhost:8443
  tenant-id: default
  username: admin
  password: admin
  use-mock: false                   # Changed from true
```

**No code changes needed!** CMS will now use Core Ledger instead of Fineract.

---

## Step 5: Create Test Accounts for CMS

```powershell
# Create savings account for debit card
curl -X POST http://localhost:8083/savingsaccounts `
  -H "Content-Type: application/json" `
  -d '{"accountNumber":"FINERACT_ACC_001","balance":10000.0,"clientName":"John Doe"}'

# Create loan account for credit card
curl -X POST http://localhost:8083/loans `
  -H "Content-Type: application/json" `
  -d '{"accountNumber":"FINERACT_LOAN_001","principal":50000.0,"clientName":"Jane Smith"}'
```

---

## Step 6: Test E2E Flow

### 6.1 Start All Services
```powershell
# Terminal 1: Core Ledger
cd d:\Code\DoAn\DATN\core-ledger-service
./gradlew bootRun

# Terminal 2: CMS
cd d:\Code\DoAn\DATN\cms-service
./gradlew bootRun

# Terminal 3: jPOS (if testing full flow)
cd d:\Code\DoAn\DATN\jPOS
./gradlew run
```

### 6.2 Issue Card in CMS
```powershell
curl -X POST "http://localhost:8082/api/cards/issue" `
  -d "pan=1234567890123456&cvv=123&expirationDate=2028-12-31&accountId=FINERACT_ACC_001&cardholderName=John Doe"
```

### 6.3 Test Transaction
```powershell
curl -X POST http://localhost:8082/api/transaction `
  -H "Content-Type: application/json" `
  -d '{"cardNumber":"1234567890123456","amount":100.0}'
```

Expected response:
```json
{
  "approved": true,
  "responseCode": "00",
  "message": "Approved"
}
```

### 6.4 Verify Balance Decreased
```powershell
curl http://localhost:8083/savingsaccounts/FINERACT_ACC_001
# Should show balance = 9900.0
```

---

## Troubleshooting

### Port Already in Use
```powershell
# Check what's using port 8083
netstat -ano | findstr :8083

# Kill process
Stop-Process -Id <PID> -Force
```

### Database Connection Failed
```powershell
# Restart database
docker-compose down
docker-compose up -d

# Check logs
docker logs ledger-postgres
```

### Gradle Build Failed
```powershell
# Clean and rebuild
./gradlew clean build
```

---

## Access Database

**pgAdmin**: http://localhost:5051
- Email: `admin@bkbank.com`
- Password: `admin`

**Add Server in pgAdmin**:
- Host: `host.docker.internal`
- Port: `5434`
- Database: `ledgerdb`
- Username: `ledgeruser`
- Password: `ledgerpassword`

---

## Next Steps

✅ Core Ledger Service is running
✅ CMS is integrated
⬜ Test full E2E flow with jPOS
⬜ Create Fraud Detection Service
⬜ Create Federated Learning Service

See: `d:\Code\DoAn\DATN\core-ledger-service\README.md` for API documentation
