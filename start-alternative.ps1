# Alternative Start Method - Using IntelliJ IDEA

Write-Host "🚀 Core Ledger Service - Alternative Start Guide" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "If Gradle build fails, use IntelliJ IDEA instead:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Open IntelliJ IDEA" -ForegroundColor White
Write-Host "2. Open Project → Select: d:\Code\DoAn\DATN\core-ledger-service" -ForegroundColor White
Write-Host "3. Wait for Gradle sync to complete" -ForegroundColor White
Write-Host "4. Right-click on LedgerServiceApplication.java" -ForegroundColor White
Write-Host "5. Select 'Run LедgerServiceApplication'" -ForegroundColor White
Write-Host ""
Write-Host "Or use Maven instead of Gradle:" -ForegroundColor Yellow
Write-Host ""

# Create pom.xml for Maven alternative
$pomContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.5</version>
    </parent>
    
    <groupId>com.bkbank</groupId>
    <artifactId>core-ledger-service</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
"@

$pomContent | Out-File -FilePath "pom.xml" -Encoding UTF8

Write-Host "✅ Created pom.xml (Maven build file)" -ForegroundColor Green
Write-Host ""
Write-Host "To run with Maven:" -ForegroundColor White
Write-Host "   mvn spring-boot:run" -ForegroundColor Cyan
Write-Host ""
Write-Host "Maven will auto-download dependencies and start the service." -ForegroundColor Gray
