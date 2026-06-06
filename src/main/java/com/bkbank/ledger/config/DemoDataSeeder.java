package com.bkbank.ledger.config;

import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.Branch;
import com.bkbank.ledger.entity.CreditCardStatement;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.entity.User;
import com.bkbank.ledger.entity.enums.AccountStatus;
import com.bkbank.ledger.entity.enums.ClientStatus;
import com.bkbank.ledger.entity.enums.EmploymentType;
import com.bkbank.ledger.entity.enums.Gender;
import com.bkbank.ledger.entity.enums.IdType;
import com.bkbank.ledger.entity.enums.UserRole;
import com.bkbank.ledger.repository.ClientRepository;
import com.bkbank.ledger.repository.BranchRepository;
import com.bkbank.ledger.repository.CreditCardStatementRepository;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.SavingsAccountRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import com.bkbank.ledger.repository.UserRepository;
import com.bkbank.ledger.service.BranchService;
import com.bkbank.ledger.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private final ClientRepository clientRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final TransactionRepository transactionRepository;
    private final CreditCardStatementRepository creditCardStatementRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchService branchService;
    private final MerchantService merchantService;

    @Override
    @Transactional
    public void run(String... args) {
        branchService.ensureDefaultBranches();
        merchantService.createDemoMerchantsIfNotExist();

        Client clientOne = seedClient(
                "CLI_001",
                "Nguyen Van A",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "nguyenvana@gmail.com",
                "0333444555",
                "123 A Street, Hanoi",
                "001122334455",
                "Software Engineer",
                "FPT Software",
                30_000_000.0,
                "BR001"
        );
        Client clientTwo = seedClient(
                "CLI_002",
                "Tran Thi B",
                LocalDate.of(1992, 5, 12),
                Gender.FEMALE,
                "tranthib@gmail.com",
                "0988777666",
                "45 Nguyen Hue, Ho Chi Minh City",
                "998877665544",
                "Product Manager",
                "VNG",
                42_000_000.0,
                "BR002"
        );

        seedCustomerUser(clientOne, "0333444555", "customer123");
        seedCustomerUser(clientTwo, "0988777666", "customer123");

        seedSavingsAccount(clientOne, "1000000011", 12_350.0);
        seedSavingsAccount(clientTwo, "1000000022", 8_400.0);

        seedLoanAccount(clientOne, "2000000013", 10_000.0, 4_410.0, 25);
        seedLoanAccount(clientTwo, "2000000024", 15_000.0, 2_200.0, 15);
        seedLoanAccount(clientOne, "2000000035", 12_000.0, 0.0, 10);
        seedLoanAccount(clientTwo, "2000000046", 8_000.0, 2_203.38, 5);

        seedSavingsTransactions();
        seedLoanStatementScenarios();

        log.info("Demo ledger data seeded successfully");
    }

    private Client seedClient(String clientId,
                              String fullName,
                              LocalDate dateOfBirth,
                              Gender gender,
                              String email,
                              String phoneNumber,
                              String address,
                              String idNumber,
                              String occupation,
                              String employerName,
                              Double monthlyIncome,
                              String homeBranchId) {
        return clientRepository.findByClientId(clientId).orElseGet(() -> {
            Branch homeBranch = branchRepository.findByBranchId(homeBranchId).orElse(null);
            Client client = new Client();
            client.setClientId(clientId);
            client.setFullName(fullName);
            client.setDateOfBirth(dateOfBirth);
            client.setGender(gender);
            client.setEmail(email);
            client.setPhoneNumber(phoneNumber);
            client.setAddress(address);
            client.setIdNumber(idNumber);
            client.setIdType(IdType.NATIONAL_ID);
            client.setStatus(ClientStatus.ACTIVE);
            client.setCity("Vietnam");
            client.setCountry("Vietnam");
            client.setHomeBranch(homeBranch);
            client.setIdIssueDate(LocalDate.of(2018, 1, 1));
            client.setIdExpiryDate(LocalDate.of(2033, 1, 1));
            client.setOccupation(occupation);
            client.setEmployerName(employerName);
            client.setEmployerAddress(address);
            client.setEmploymentType(EmploymentType.FULL_TIME);
            client.setMonthlyIncome(monthlyIncome);
            client.setYearsAtCurrentJob(4);
            return clientRepository.save(client);
        });
    }

    private void seedCustomerUser(Client client, String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(client.getFullName())
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .client(client)
                .build();
        userRepository.save(user);
    }

    private void seedSavingsAccount(Client client, String accountNumber, Double balance) {
        if (savingsAccountRepository.existsByAccountNumber(accountNumber)) {
            return;
        }

        SavingsAccount account = new SavingsAccount();
        account.setAccountNumber(accountNumber);
        account.setBalance(balance);
        account.setCurrency("USD");
        account.setStatus(AccountStatus.ACTIVE);
        account.setClient(client);
        account.setBranch(client.getHomeBranch());
        savingsAccountRepository.save(account);
    }

    private void seedLoanAccount(Client client, String accountNumber, Double principal, Double outstanding, Integer billingDay) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber).orElseGet(LoanAccount::new);
        account.setAccountNumber(accountNumber);
        account.setPrincipal(principal);
        account.setPrincipalOutstanding(outstanding);
        account.setCurrency("USD");
        account.setBillingDayOfMonth(billingDay);
        account.setPaymentDueDays(20);
        account.setMinimumPaymentRate(5.0);
        account.setMinimumPaymentFloor(10.0);
        account.setStatementInterestRateAnnual(30.0);
        account.setStatementLateFeeFixed(15.0);
        account.setStatus(AccountStatus.ACTIVE);
        account.setClient(client);
        account.setBranch(client.getHomeBranch());
        loanAccountRepository.save(account);
    }

    private void seedSavingsTransactions() {
        createTransactionIfMissing(
                "DEMO-SAV-DEPOSIT-001",
                buildTransaction(
                        Transaction.createDeposit("1000000011", 15_000.0, "USD", 15_000.0),
                        "DEMO-SAV-DEPOSIT-001",
                        LocalDateTime.now().minusDays(20).withHour(9).withMinute(0),
                        null,
                        null,
                        "BRANCH",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-SAV-WITHDRAW-001",
                buildTransaction(
                        Transaction.createWithdrawal(
                                "1000000011",
                                2_650.0,
                                "USD",
                                12_350.0,
                                "SP0002",
                                "Siêu thị GO",
                                "38 Nguyen Van Linh, Tan Phong, District 7",
                                10.8231,
                                106.6297
                        ),
                        "DEMO-SAV-WITHDRAW-001",
                        LocalDateTime.now().minusDays(5).withHour(15).withMinute(20),
                        "SP0002",
                        "Siêu thị GO",
                        "POS",
                        "00",
                        "Approved"
                )
        );
    }

    private void seedLoanStatementScenarios() {
        seedPartiallyPaidScenario();
        seedOpenScenario();
        seedPaidScenario();
        seedOverdueScenario();
    }

    private void seedPartiallyPaidScenario() {
        LocalDate billingDate = latestBillingDate(25);
        LocalDate previousBillingDate = billingDate.minusMonths(1).withDayOfMonth(25);
        LocalDate statementStart = previousBillingDate.plusDays(1);
        LocalDate dueDate = billingDate.plusDays(20);
        LocalDateTime postStatementPaymentDate = billingDate.plusDays(2).atTime(8, 45);

        createTransactionIfMissing(
                "DEMO-LOAN-CHARGE-001",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000013",
                                2_400.0,
                                "USD",
                                2_400.0,
                                "SP0001",
                                "Điện lực EVN",
                                "11 Cua Bac, Truc Bach, Ba Dinh",
                                21.0285,
                                105.8542
                        ),
                        "DEMO-LOAN-CHARGE-001",
                        statementStart.atTime(10, 15),
                        "SP0001",
                        "Điện lực EVN",
                        "ECOM",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-CHARGE-002",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000013",
                                890.0,
                                "USD",
                                3_290.0,
                                "SPD001",
                                "Metro Fresh",
                                "120 Nguyen Hue, Ben Nghe, District 1",
                                10.7768,
                                106.7002
                        ),
                        "DEMO-LOAN-CHARGE-002",
                        statementStart.plusDays(6).atTime(13, 45),
                        "SPD001",
                        "Metro Fresh",
                        "POS",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-PAYMENT-001",
                buildTransaction(
                        Transaction.createPayment("2000000013", 600.0, "USD", 2_690.0),
                        "DEMO-LOAN-PAYMENT-001",
                        statementStart.plusDays(12).atTime(9, 30),
                        null,
                        null,
                        "MOBILE",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-FAILED-001",
                buildTransaction(
                        Transaction.createFailedCharge(
                                "2000000013",
                                900.0,
                                "USD",
                                2_690.0,
                                "SPD005",
                                "River Bookstore",
                                "14 Hoa Binh, Tan An, Ninh Kieu",
                                10.0358,
                                105.7806,
                                "Credit limit exceeded"
                        ),
                        "DEMO-LOAN-FAILED-001",
                        statementStart.plusDays(17).atTime(21, 10),
                        "SPD005",
                        "River Bookstore",
                        "ECOM",
                        "51",
                        "Credit limit exceeded"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-CHARGE-003",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000013",
                                1_820.0,
                                "USD",
                                4_510.0,
                                "SPD003",
                                "Blue Cinema",
                                "25 Tran Phu, Hai Chau 1, Hai Chau",
                                16.0678,
                                108.2213
                        ),
                        "DEMO-LOAN-CHARGE-003",
                        statementStart.plusDays(24).atTime(18, 0),
                        "SPD003",
                        "Blue Cinema",
                        "POS",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-PAYMENT-AFTER-001",
                buildTransaction(
                        Transaction.createPayment("2000000013", 100.0, "USD", 4_410.0),
                        "DEMO-LOAN-PAYMENT-AFTER-001",
                        postStatementPaymentDate,
                        null,
                        null,
                        "MOBILE",
                        "00",
                        "Approved"
                )
        );

        saveOrUpdateStatement(
                "2000000013",
                statementStart,
                billingDate,
                dueDate,
                0.0,
                5_110.0,
                600.0,
                225.5,
                4_510.0,
                30.0,
                0.0,
                null,
                15.0,
                0.0,
                null,
                5_490.0,
                5,
                "PARTIALLY_PAID",
                100.0,
                125.5,
                4_410.0,
                postStatementPaymentDate
        );
    }

    private void seedOpenScenario() {
        LocalDate billingDate = latestBillingDate(15);
        LocalDate previousBillingDate = billingDate.minusMonths(1).withDayOfMonth(15);
        LocalDate statementStart = previousBillingDate.plusDays(1);
        LocalDate dueDate = billingDate.plusDays(20);

        createTransactionIfMissing(
                "DEMO-LOAN-OPEN-CHARGE-001",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000024",
                                1_500.0,
                                "USD",
                                1_500.0,
                                "SPD007",
                                "Green Pharmacy",
                                "88 Hai Ba Trung, District 1",
                                10.7829,
                                106.7009
                        ),
                        "DEMO-LOAN-OPEN-CHARGE-001",
                        statementStart.plusDays(2).atTime(11, 5),
                        "SPD007",
                        "Green Pharmacy",
                        "POS",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-OPEN-CHARGE-002",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000024",
                                700.0,
                                "USD",
                                2_200.0,
                                "SPD008",
                                "Cloud Electronics",
                                "12 Truong Dinh, District 3",
                                10.7796,
                                106.6924
                        ),
                        "DEMO-LOAN-OPEN-CHARGE-002",
                        statementStart.plusDays(11).atTime(16, 20),
                        "SPD008",
                        "Cloud Electronics",
                        "ECOM",
                        "00",
                        "Approved"
                )
        );

        saveOrUpdateStatement(
                "2000000024",
                statementStart,
                billingDate,
                dueDate,
                0.0,
                2_200.0,
                0.0,
                110.0,
                2_200.0,
                30.0,
                0.0,
                null,
                15.0,
                0.0,
                null,
                12_800.0,
                2,
                "OPEN",
                0.0,
                110.0,
                2_200.0,
                null
        );
    }

    private void seedPaidScenario() {
        LocalDate billingDate = latestBillingDate(10);
        LocalDate previousBillingDate = billingDate.minusMonths(1).withDayOfMonth(10);
        LocalDate statementStart = previousBillingDate.plusDays(1);
        LocalDate dueDate = billingDate.plusDays(20);
        LocalDateTime paidAt = billingDate.plusDays(2).atTime(14, 10);

        createTransactionIfMissing(
                "DEMO-LOAN-PAID-CHARGE-001",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000035",
                                1_200.0,
                                "USD",
                                1_200.0,
                                "SPD009",
                                "Fresh Mart",
                                "99 Nguyen Trai, Thanh Xuan",
                                21.0037,
                                105.8119
                        ),
                        "DEMO-LOAN-PAID-CHARGE-001",
                        statementStart.plusDays(1).atTime(10, 0),
                        "SPD009",
                        "Fresh Mart",
                        "POS",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-PAID-CHARGE-002",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000035",
                                600.0,
                                "USD",
                                1_800.0,
                                "SPD010",
                                "Travel Air",
                                "1 Bach Dang, Hai Chau",
                                16.0544,
                                108.2022
                        ),
                        "DEMO-LOAN-PAID-CHARGE-002",
                        statementStart.plusDays(9).atTime(19, 15),
                        "SPD010",
                        "Travel Air",
                        "ECOM",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-PAID-PAYMENT-001",
                buildTransaction(
                        Transaction.createPayment("2000000035", 1_800.0, "USD", 0.0),
                        "DEMO-LOAN-PAID-PAYMENT-001",
                        paidAt,
                        null,
                        null,
                        "MOBILE",
                        "00",
                        "Approved"
                )
        );

        saveOrUpdateStatement(
                "2000000035",
                statementStart,
                billingDate,
                dueDate,
                0.0,
                1_800.0,
                0.0,
                90.0,
                1_800.0,
                30.0,
                0.0,
                null,
                15.0,
                0.0,
                null,
                10_200.0,
                2,
                "PAID",
                1_800.0,
                0.0,
                0.0,
                paidAt
        );
    }

    private void seedOverdueScenario() {
        LocalDate billingDate = latestBillingDate(5);
        LocalDate previousBillingDate = billingDate.minusMonths(1).withDayOfMonth(5);
        LocalDate previousStatementStart = previousBillingDate.minusMonths(1).withDayOfMonth(5).plusDays(1);
        LocalDate previousDueDate = previousBillingDate.plusDays(20);
        LocalDate statementStart = previousBillingDate.plusDays(1);
        LocalDate dueDate = billingDate.plusDays(20);
        LocalDateTime previousInterestAppliedAt = previousDueDate.plusDays(1).atTime(9, 0);
        LocalDateTime previousLateFeeAppliedAt = previousDueDate.plusDays(1).atTime(9, 15);
        LocalDateTime interestAppliedAt = dueDate.plusDays(1).atTime(9, 0);
        LocalDateTime lateFeeAppliedAt = dueDate.plusDays(1).atTime(9, 15);

        createTransactionIfMissing(
                "DEMO-LOAN-OVERDUE-PREV-CHARGE-001",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000046",
                                450.0,
                                "USD",
                                450.0,
                                "SPD013",
                                "Sun Market",
                                "22 Hung Vuong, Hue",
                                16.4634,
                                107.5903
                        ),
                        "DEMO-LOAN-OVERDUE-PREV-CHARGE-001",
                        previousStatementStart.plusDays(4).atTime(10, 20),
                        "SPD013",
                        "Sun Market",
                        "POS",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-OVERDUE-PREV-CHARGE-002",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000046",
                                350.0,
                                "USD",
                                800.0,
                                "SPD014",
                                "City Books",
                                "30 Le Loi, Hue",
                                16.4702,
                                107.5848
                        ),
                        "DEMO-LOAN-OVERDUE-PREV-CHARGE-002",
                        previousStatementStart.plusDays(20).atTime(18, 5),
                        "SPD014",
                        "City Books",
                        "ECOM",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-OVERDUE-PREV-INTEREST-001",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000046",
                                20.0,
                                "USD",
                                820.0,
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        "DEMO-LOAN-OVERDUE-PREV-INTEREST-001",
                        previousInterestAppliedAt,
                        null,
                        null,
                        "STATEMENT_INTEREST",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-OVERDUE-PREV-LATEFEE-001",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000046",
                                15.0,
                                "USD",
                                835.0,
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        "DEMO-LOAN-OVERDUE-PREV-LATEFEE-001",
                        previousLateFeeAppliedAt,
                        null,
                        null,
                        "STATEMENT_LATE_FEE",
                        "00",
                        "Approved"
                )
        );

        createTransactionIfMissing(
                "DEMO-LOAN-OVERDUE-CHARGE-001",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000046",
                                800.0,
                                "USD",
                                1_600.0,
                                "SPD011",
                                "Medicare Plus",
                                "7 Le Loi, Hue",
                                16.4637,
                                107.5909
                        ),
                        "DEMO-LOAN-OVERDUE-CHARGE-001",
                        statementStart.plusDays(2).atTime(9, 45),
                        "SPD011",
                        "Medicare Plus",
                        "POS",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-OVERDUE-CHARGE-002",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000046",
                                500.0,
                                "USD",
                                2_100.0,
                                "SPD012",
                                "Ocean Store",
                                "15 Vo Nguyen Giap, Son Tra",
                                16.0777,
                                108.2433
                        ),
                        "DEMO-LOAN-OVERDUE-CHARGE-002",
                        statementStart.plusDays(18).atTime(20, 10),
                        "SPD012",
                        "Ocean Store",
                        "ECOM",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-OVERDUE-INTEREST-001",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000046",
                                53.38,
                                "USD",
                                2_188.38,
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        "DEMO-LOAN-OVERDUE-INTEREST-001",
                        interestAppliedAt,
                        null,
                        null,
                        "STATEMENT_INTEREST",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-OVERDUE-LATEFEE-001",
                buildTransaction(
                        Transaction.createCharge(
                                "2000000046",
                                15.0,
                                "USD",
                                2_203.38,
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        "DEMO-LOAN-OVERDUE-LATEFEE-001",
                        lateFeeAppliedAt,
                        null,
                        null,
                        "STATEMENT_LATE_FEE",
                        "00",
                        "Approved"
                )
        );

        transactionRepository.findByPaymentId("DEMO-LOAN-OVERDUE-PREV-INTEREST-001").ifPresent(tx -> {
            tx.setTransactionType("INTEREST");
            tx.setDescription("Statement interest");
            tx.setExternalReference(previousBillingDate.toString());
            transactionRepository.save(tx);
        });
        transactionRepository.findByPaymentId("DEMO-LOAN-OVERDUE-PREV-LATEFEE-001").ifPresent(tx -> {
            tx.setTransactionType("LATE_FEE");
            tx.setDescription("Statement late fee");
            tx.setExternalReference(previousBillingDate.toString());
            transactionRepository.save(tx);
        });
        transactionRepository.findByPaymentId("DEMO-LOAN-OVERDUE-INTEREST-001").ifPresent(tx -> {
            tx.setTransactionType("INTEREST");
            tx.setDescription("Statement interest");
            tx.setExternalReference(billingDate.toString());
            transactionRepository.save(tx);
        });
        transactionRepository.findByPaymentId("DEMO-LOAN-OVERDUE-LATEFEE-001").ifPresent(tx -> {
            tx.setTransactionType("LATE_FEE");
            tx.setDescription("Statement late fee");
            tx.setExternalReference(billingDate.toString());
            transactionRepository.save(tx);
        });

        saveOrUpdateStatement(
                "2000000046",
                previousStatementStart,
                previousBillingDate,
                previousDueDate,
                0.0,
                800.0,
                0.0,
                40.0,
                800.0,
                30.0,
                20.0,
                previousInterestAppliedAt,
                15.0,
                15.0,
                previousLateFeeAppliedAt,
                7_200.0,
                2,
                "OVERDUE",
                0.0,
                40.0,
                835.0,
                null
        );

        saveOrUpdateStatement(
                "2000000046",
                statementStart,
                billingDate,
                dueDate,
                800.0,
                1_335.0,
                0.0,
                106.75,
                2_135.0,
                30.0,
                53.38,
                interestAppliedAt,
                15.0,
                15.0,
                lateFeeAppliedAt,
                5_865.0,
                4,
                "OVERDUE",
                0.0,
                106.75,
                2_203.38,
                null
        );
    }

    private void saveOrUpdateStatement(String accountNumber,
                                       LocalDate statementStart,
                                       LocalDate billingDate,
                                       LocalDate dueDate,
                                       Double previousBalance,
                                       Double totalCharges,
                                       Double totalPayments,
                                       Double minimumDue,
                                       Double newBalance,
                                       Double interestRateAnnual,
                                       Double interestCharged,
                                       LocalDateTime interestAppliedAt,
                                       Double lateFeeFixed,
                                       Double lateFeeCharged,
                                       LocalDateTime lateFeeAppliedAt,
                                       Double availableCreditAtBilling,
                                       Integer transactionCount,
                                       String statementStatus,
                                       Double paidAmountAfterStatement,
                                       Double remainingMinimumDue,
                                       Double remainingBalance,
                                       LocalDateTime lastPaymentDate) {
        CreditCardStatement statement = creditCardStatementRepository
                .findByAccountNumberAndBillingDate(accountNumber, billingDate)
                .orElseGet(CreditCardStatement::new);
        statement.setAccountNumber(accountNumber);
        statement.setStatementPeriodStart(statementStart);
        statement.setStatementPeriodEnd(billingDate);
        statement.setBillingDate(billingDate);
        statement.setDueDate(dueDate);
        statement.setPreviousBalance(previousBalance);
        statement.setTotalCharges(totalCharges);
        statement.setTotalPayments(totalPayments);
        statement.setMinimumDue(minimumDue);
        statement.setNewBalance(newBalance);
        statement.setInterestRateAnnual(interestRateAnnual);
        statement.setInterestCharged(interestCharged);
        statement.setInterestAppliedAt(interestAppliedAt);
        statement.setLateFeeFixed(lateFeeFixed);
        statement.setLateFeeCharged(lateFeeCharged);
        statement.setLateFeeAppliedAt(lateFeeAppliedAt);
        statement.setAvailableCreditAtBilling(availableCreditAtBilling);
        statement.setTransactionCount(transactionCount);
        statement.setStatementStatus(statementStatus);
        statement.setPaidAmountAfterStatement(paidAmountAfterStatement);
        statement.setRemainingMinimumDue(remainingMinimumDue);
        statement.setRemainingBalance(remainingBalance);
        statement.setLastPaymentDate(lastPaymentDate);
        creditCardStatementRepository.save(statement);
    }

    private Transaction buildTransaction(Transaction transaction,
                                         String paymentId,
                                         LocalDateTime transactionDate,
                                         String merchantId,
                                         String merchantName,
                                         String channel,
                                         String responseCode,
                                         String responseMessage) {
        transaction.setPaymentId(paymentId);
        transaction.setTransactionDate(transactionDate);
        transaction.setMerchantId(merchantId);
        transaction.setMerchantName(merchantName);
        transaction.setChannel(channel);
        transaction.setResponseCode(responseCode);
        transaction.setResponseMessage(responseMessage);
        assignBranch(transaction);
        return transaction;
    }

    private void assignBranch(Transaction transaction) {
        if ("SAVINGS".equalsIgnoreCase(transaction.getAccountType())) {
            savingsAccountRepository.findByAccountNumber(transaction.getAccountNumber())
                    .ifPresent(account -> transaction.assignBranch(account.getBranchId(), account.getBranchName()));
            return;
        }
        if ("LOAN".equalsIgnoreCase(transaction.getAccountType())) {
            loanAccountRepository.findByAccountNumber(transaction.getAccountNumber())
                    .ifPresent(account -> transaction.assignBranch(account.getBranchId(), account.getBranchName()));
        }
    }

    private void createTransactionIfMissing(String paymentId, Transaction transaction) {
        transactionRepository.findByPaymentId(paymentId).ifPresentOrElse(existing -> {
            existing.setAccountNumber(transaction.getAccountNumber());
            existing.setAccountType(transaction.getAccountType());
            existing.setTransactionType(transaction.getTransactionType());
            existing.setAmount(transaction.getAmount());
            existing.setCurrency(transaction.getCurrency());
            existing.setIdempotencyKey(transaction.getIdempotencyKey());
            existing.setOriginalTransactionId(transaction.getOriginalTransactionId());
            existing.setChannel(transaction.getChannel());
            existing.setTransactionDate(transaction.getTransactionDate());
            existing.setDescription(transaction.getDescription());
            existing.setBalanceAfter(transaction.getBalanceAfter());
            existing.setMerchantId(transaction.getMerchantId());
            existing.setMerchantName(transaction.getMerchantName());
            existing.setBranchId(transaction.getBranchId());
            existing.setBranchName(transaction.getBranchName());
            existing.setLocation(transaction.getLocation());
            existing.setLatitude(transaction.getLatitude());
            existing.setLongitude(transaction.getLongitude());
            existing.setCardNetwork(transaction.getCardNetwork());
            existing.setAuthCode(transaction.getAuthCode());
            existing.setStan(transaction.getStan());
            existing.setRrn(transaction.getRrn());
            existing.setExternalReference(transaction.getExternalReference());
            existing.setResponseCode(transaction.getResponseCode());
            existing.setResponseMessage(transaction.getResponseMessage());
            existing.setStatus(transaction.getStatus());
            transactionRepository.save(existing);
            if (transaction.getTransactionDate() != null) {
                transactionRepository.updateTransactionDateByPaymentId(paymentId, transaction.getTransactionDate());
            }
        }, () -> {
            transactionRepository.save(transaction);
            if (transaction.getTransactionDate() != null) {
                transactionRepository.updateTransactionDateByPaymentId(paymentId, transaction.getTransactionDate());
            }
        });
    }

    private LocalDate latestBillingDate(int billingDay) {
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() >= billingDay) {
            return today.withDayOfMonth(billingDay);
        }
        return today.minusMonths(1).withDayOfMonth(billingDay);
    }
}
