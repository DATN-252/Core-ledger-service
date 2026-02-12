# 🚀 How to Run Core Ledger Service

## ⚠️ If Gradle Build Fails

Nếu gặp lỗi `Failed to notify dependency resolution listener`, có 3 cách để chạy service:

---

## ✅ Method 1: IntelliJ IDEA (Khuyến nghị - Dễ nhất)

### Bước 1: Mở Project
1. Mở **IntelliJ IDEA**
2. Click **File** → **Open**
3. Chọn thư mục: `d:\Code\DoAn\DATN\core-ledger-service`
4. Click **OK**

### Bước 2: Wait for Sync
- IntelliJ sẽ tự động detect Gradle project
- Đợi Gradle sync hoàn tất (xem progress bar góc dưới)
- Nếu có popup "Trust Project", click **Trust Project**

### Bước 3: Run Service
1. Mở file: `src/main/java/com/bkbank/ledger/LedgerServiceApplication.java`
2. Click chuột phải vào file
3. Chọn **Run 'LedgerServiceApplication'**

✅ Service sẽ chạy trên **http://localhost:8083**

---

## ✅ Method 2: Maven (Alternative)

Nếu Gradle không work, dùng Maven:

### Bước 1: Generate pom.xml
```powershell
cd d:\Code\DoAn\DATN\core-ledger-service
.\start-alternative.ps1
```

### Bước 2: Run with Maven
```powershell
mvn spring-boot:run
```

---

## ✅ Method 3: Fix Gradle (For Advanced Users)

### Option A: Clean Gradle Cache
```powershell
cd d:\Code\DoAn\DATN\core-ledger-service

# Delete .gradle cache
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue

# Rebuild
.\gradlew.bat clean build --refresh-dependencies
.\gradlew.bat bootRun
```

### Option B: Use Different Java Version
Nếu bạn có nhiều Java versions:
```powershell
# Check Java version
java -version

# Should be Java 17 or higher
# If not, install Java 17 from: https://adoptium.net/
```

---

## 🧪 After Service Starts

### Test API
```powershell
# Test if service is running
curl http://localhost:8083/actuator/health

# Run full API test
.\test-ledger-api.ps1
```

### Check Logs
Xem log trong IntelliJ console hoặc terminal để verify:
```
Started LedgerServiceApplication in X.XXX seconds (JVM running for X.XXX)
```

---

## 📝 Summary

**Easiest method**: Use IntelliJ IDEA (Method 1)
- No command line needed
- Auto-handles dependencies
- Built-in debugging

**For command line**: Use Maven (Method 2)
- More stable than Gradle
- Good alternative if Gradle has issues

**For troubleshooting**: See Method 3
- Clean cache
- Check Java version

---

## 🆘 Still Having Issues?

1. **Check Database**: Make sure PostgreSQL is running
   ```powershell
   docker ps | findstr ledger-postgres
   ```

2. **Check Port**: Make sure port 8083 is not in use
   ```powershell
   netstat-ano | findstr :8083
   ```

3. **Check Java**: Verify Java 17+ is installed
   ```powershell
   java -version
   ```

4. **Restart Everything**:
   ```powershell
   # Stop database
   docker-compose down
   
   # Start fresh
   docker-compose up -d
   
   # Run service with IntelliJ IDEA
   ```
