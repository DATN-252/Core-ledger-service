# 🔗 CMS Integration Guide

## Update CMS to Use Core Ledger Service

Bây giờ Core Ledger Service đã chạy, bạn cần update CMS để sử dụng nó thay vì Fineract.

### Step 1: Update application.yml

File: `d:\Code\DoAn\DATN\cms-service\src\main\resources\application.yml`

```yaml
fineract:
  base-url: http://localhost:8083  # ✅ Changed from Fineract URL
  tenant-id: default
  username: admin
  password: admin
  use-mock: false                   # ✅ Changed from true
```

**Không cần thay đổi code!** `FineractClient.java` vẫn work vì APIs compatible.

---

### Step 2: Create Test Accounts

Tạo accounts để test với CMS:

```powershell
# Debit card account
$body = '{"accountNumber":"FINERACT_ACC_001","balance":10000.0,"clientName":"John Doe"}'
Invoke-RestMethod -Uri "http://localhost:8083/savingsaccounts" -Method POST -Body $body -ContentType "application/json"

# Credit card account
$body = '{"accountNumber":"FINERACT_LOAN_001","principal":50000.0,"clientName":"Jane Smith"}'
Invoke-RestMethod -Uri "http://localhost:8083/loans" -Method POST -Body $body -ContentType "application/json"
```

**Hoặc chạy script tự động**:
```powershell
.\test-e2e-flow.ps1
```

---

### Step 3: Restart CMS Service

```powershell
cd d:\Code\DoAn\DATN\cms-service

# Stop current CMS if running (Ctrl+C)

# Start với config mới
.\gradlew.bat bootRun
```

---

### Step 4: Test Integration

#### Issue Debit Card
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/cards/issue" `
  -Method POST `
  -Body "pan=1234567890123456&cvv=123&expirationDate=2028-12-31&accountId=FINERACT_ACC_001&cardholderName=John Doe" `
  -ContentType "application/x-www-form-urlencoded"
```

#### Test Transaction
```powershell
$body = '{"cardNumber":"1234567890123456","amount":100.0}'
Invoke-RestMethod -Uri "http://localhost:8082/api/transaction" `
  -Method POST -Body $body -ContentType "application/json"
```

**Expected**: Transaction approved, Core Ledger balance giảm từ 10000 → 9900

#### Verify in Core Ledger
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/savingsaccounts/FINERACT_ACC_001"
```

**Expected output**:
```
accountBalance : 9900
status         : @{value=ACTIVE}
```

---

## 🎯 Benefits vs Fineract

| Feature | Apache Fineract | Core Ledger Service |
|---------|----------------|---------------------|
| Files | 6500+ | ~20 |
| Setup | Hours | Minutes |
| Build | Complex | Simple |
| Port | 8443 (HTTPS) | 8083 (HTTP) |
| For Thesis | ❌ Overkill | ✅ Perfect |

---

## Troubleshooting

### CMS Cannot Connect
```powershell
# Check if Core Ledger is running
netstat -ano | findstr :8083

# Check CMS config
cat d:\Code\DoAn\DATN\cms-service\src\main\resources\application.yml
```

### Account Not Found
Make sure accounts exist in Core Ledger:
```powershell
# List all savings accounts (via database)
docker exec -it ledger-postgres psql -U ledgeruser -d ledgerdb -c "SELECT * FROM savings_accounts;"
```

### Transaction Fails
Check logs in Core Ledger terminal for detailed error messages.

---

## Next: Full E2E Test

After CMS integration works, test the complete flow:

**ATM → jPOS → CMS → Core Ledger → Database**

See: E2E Testing Guide
