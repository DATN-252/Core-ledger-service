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

    @Override
    @Transactional
    public void run(String... args) {
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

        seedSavingsAccount(clientOne, "S0011", 12_350.0);
        seedSavingsAccount(clientTwo, "S0022", 8_400.0);

        seedLoanAccount(clientOne, "C0013", 10_000.0, 4_410.0, 25);
        seedLoanAccount(clientTwo, "C0024", 15_000.0, 2_200.0, 15);

        seedSavingsTransactions();
        seedLoanTransactionsAndStatement();

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
        if (loanAccountRepository.existsByAccountNumber(accountNumber)) {
            return;
        }

        LoanAccount account = new LoanAccount();
        account.setAccountNumber(accountNumber);
        account.setPrincipal(principal);
        account.setPrincipalOutstanding(outstanding);
        account.setCurrency("USD");
        account.setBillingDayOfMonth(billingDay);
        account.setPaymentDueDays(20);
        account.setMinimumPaymentRate(5.0);
        account.setMinimumPaymentFloor(10.0);
        account.setStatus(AccountStatus.ACTIVE);
        account.setClient(client);
        account.setBranch(client.getHomeBranch());
        loanAccountRepository.save(account);
    }

    private void seedSavingsTransactions() {
        createTransactionIfMissing(
                "DEMO-SAV-DEPOSIT-001",
                buildTransaction(
                        Transaction.createDeposit("S0011", 15_000.0, "USD", 15_000.0),
                        "DEMO-SAV-DEPOSIT-001",
                        LocalDateTime.now().minusDays(20).withHour(9).withMinute(0),
                        "M001",
                        "BKBank Branch Hanoi",
                        "Branch",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-SAV-WITHDRAW-001",
                buildTransaction(
                        Transaction.createWithdrawal("S0011", 2_650.0, "USD", 12_350.0, "M002", "AEON Mall", "Hanoi", null, null),
                        "DEMO-SAV-WITHDRAW-001",
                        LocalDateTime.now().minusDays(5).withHour(15).withMinute(20),
                        "M002",
                        "AEON Mall",
                        "POS",
                        "00",
                        "Approved"
                )
        );
    }

    private void seedLoanTransactionsAndStatement() {
        LocalDate latestBillingDate = latestBillingDate(25);
        LocalDate previousBillingDate = latestBillingDate.minusMonths(1).withDayOfMonth(25);
        LocalDate statementStart = previousBillingDate.plusDays(1);
        LocalDate dueDate = latestBillingDate.plusDays(20);

        createTransactionIfMissing(
                "DEMO-LOAN-CHARGE-001",
                buildTransaction(
                        Transaction.createCharge("C0013", 2_400.0, "USD", 2_400.0, "M100", "Apple Store", "Online", null, null),
                        "DEMO-LOAN-CHARGE-001",
                        statementStart.atTime(10, 15),
                        "M100",
                        "Apple Store",
                        "ECOM",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-CHARGE-002",
                buildTransaction(
                        Transaction.createCharge("C0013", 890.0, "USD", 3_290.0, "M101", "Amazon", "Online", null, null),
                        "DEMO-LOAN-CHARGE-002",
                        statementStart.plusDays(6).atTime(13, 45),
                        "M101",
                        "Amazon",
                        "ECOM",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-PAYMENT-001",
                buildTransaction(
                        Transaction.createPayment("C0013", 600.0, "USD", 2_690.0),
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
                        Transaction.createFailedCharge("C0013", 900.0, "USD", 2_690.0, "M102", "Steam", "Online", null, null, "Credit limit exceeded"),
                        "DEMO-LOAN-FAILED-001",
                        statementStart.plusDays(17).atTime(21, 10),
                        "M102",
                        "Steam",
                        "ECOM",
                        "51",
                        "Credit limit exceeded"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-CHARGE-003",
                buildTransaction(
                        Transaction.createCharge("C0013", 1_820.0, "USD", 4_510.0, "M103", "Booking.com", "Online", null, null),
                        "DEMO-LOAN-CHARGE-003",
                        statementStart.plusDays(24).atTime(18, 0),
                        "M103",
                        "Booking.com",
                        "ECOM",
                        "00",
                        "Approved"
                )
        );
        createTransactionIfMissing(
                "DEMO-LOAN-PAYMENT-AFTER-001",
                buildTransaction(
                        Transaction.createPayment("C0013", 100.0, "USD", 4_410.0),
                        "DEMO-LOAN-PAYMENT-AFTER-001",
                        latestBillingDate.plusDays(5).atTime(8, 45),
                        null,
                        null,
                        "MOBILE",
                        "00",
                        "Approved"
                )
        );

        if (creditCardStatementRepository.findByAccountNumberAndBillingDate("C0013", latestBillingDate).isPresent()) {
            return;
        }

        CreditCardStatement statement = new CreditCardStatement();
        statement.setAccountNumber("C0013");
        statement.setStatementPeriodStart(statementStart);
        statement.setStatementPeriodEnd(latestBillingDate);
        statement.setBillingDate(latestBillingDate);
        statement.setDueDate(dueDate);
        statement.setPreviousBalance(0.0);
        statement.setTotalCharges(5_110.0);
        statement.setTotalPayments(600.0);
        statement.setMinimumDue(225.5);
        statement.setNewBalance(4_510.0);
        statement.setAvailableCreditAtBilling(5_490.0);
        statement.setTransactionCount(5);
        statement.setStatementStatus(LocalDate.now().isAfter(dueDate) ? "OVERDUE" : "PARTIALLY_PAID");
        statement.setPaidAmountAfterStatement(100.0);
        statement.setRemainingMinimumDue(125.5);
        statement.setRemainingBalance(4_410.0);
        statement.setLastPaymentDate(latestBillingDate.plusDays(5).atTime(8, 45));
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
        return transaction;
    }

    private void createTransactionIfMissing(String paymentId, Transaction transaction) {
        if (transactionRepository.findByPaymentId(paymentId).isPresent()) {
            return;
        }
        transactionRepository.save(transaction);
    }

    private LocalDate latestBillingDate(int billingDay) {
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() >= billingDay) {
            return today.withDayOfMonth(billingDay);
        }
        return today.minusMonths(1).withDayOfMonth(billingDay);
    }
}
