package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.response.CreditCardMonthlyStatementResponse;
import com.bkbank.ledger.dto.response.CreditCardStatementSummaryResponse;
import com.bkbank.ledger.dto.response.LoanStatementItemResponse;
import com.bkbank.ledger.entity.CreditCardStatement;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.CreditCardStatementRepository;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditCardStatementService {

    private final LoanAccountRepository loanAccountRepository;
    private final TransactionRepository transactionRepository;
    private final CreditCardStatementRepository creditCardStatementRepository;

    @Transactional
    public CreditCardMonthlyStatementResponse generateMonthlyStatement(String accountNumber, LocalDate billingDate) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        LocalDate normalizedBillingDate = normalizeBillingDate(billingDate, account.getBillingDayOfMonth());
        if (!normalizedBillingDate.equals(billingDate)) {
            throw new IllegalArgumentException("billingDate does not match account billing day");
        }

        LocalDate previousBillingDate = normalizeBillingDate(billingDate.minusMonths(1), account.getBillingDayOfMonth());
        LocalDate periodStart = previousBillingDate.plusDays(1);
        LocalDate periodEnd = billingDate;
        LocalDate dueDate = billingDate.plusDays(safeInt(account.getPaymentDueDays(), 20));

        LocalDateTime from = periodStart.atStartOfDay();
        LocalDateTime to = periodEnd.atTime(LocalTime.MAX);

        double previousBalance = transactionRepository
                .findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                        accountNumber,
                        "LOAN",
                        from
                )
                .map(Transaction::getBalanceAfter)
                .orElse(0.0);

        List<Transaction> transactions = transactionRepository
                .findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                        accountNumber,
                        "LOAN",
                        from,
                        to
                );

        double totalCharges = 0.0;
        double totalPayments = 0.0;

        for (Transaction tx : transactions) {
            if (!"SUCCESS".equalsIgnoreCase(tx.getStatus())) {
                continue;
            }
            if ("CHARGE".equalsIgnoreCase(tx.getTransactionType())) {
                totalCharges += safe(tx.getAmount());
            } else if ("PAYMENT".equalsIgnoreCase(tx.getTransactionType())) {
                totalPayments += safe(tx.getAmount());
            }
        }

        double newBalance = roundMoney(previousBalance + totalCharges - totalPayments);
        double minimumDue = calculateMinimumDue(
                newBalance,
                safe(account.getMinimumPaymentRate()),
                safe(account.getMinimumPaymentFloor())
        );
        double availableCredit = roundMoney(safe(account.getPrincipal()) - newBalance);

        List<LoanStatementItemResponse> items = transactions.stream()
                .map(tx -> new LoanStatementItemResponse(
                        tx.getTransactionDate(),
                        tx.getPaymentId(),
                        tx.getTransactionType(),
                        tx.getMerchantId(),
                        tx.getMerchantName(),
                        tx.getAmount(),
                        tx.getBalanceAfter(),
                        tx.getStatus(),
                        tx.getResponseCode(),
                        tx.getResponseMessage()
                ))
                .toList();

        CreditCardStatement snapshot = creditCardStatementRepository
                .findByAccountNumberAndBillingDate(accountNumber, billingDate)
                .orElseGet(CreditCardStatement::new);
        snapshot.setAccountNumber(accountNumber);
        snapshot.setStatementPeriodStart(periodStart);
        snapshot.setStatementPeriodEnd(periodEnd);
        snapshot.setBillingDate(billingDate);
        snapshot.setDueDate(dueDate);
        snapshot.setPreviousBalance(roundMoney(previousBalance));
        snapshot.setTotalCharges(roundMoney(totalCharges));
        snapshot.setTotalPayments(roundMoney(totalPayments));
        snapshot.setMinimumDue(minimumDue);
        snapshot.setNewBalance(newBalance);
        snapshot.setAvailableCreditAtBilling(availableCredit);
        snapshot.setTransactionCount(items.size());
        snapshot.setStatementStatus("OPEN");
        snapshot.setPaidAmountAfterStatement(0.0);
        snapshot.setRemainingMinimumDue(minimumDue);
        snapshot.setRemainingBalance(newBalance);
        snapshot.setLastPaymentDate(null);
        creditCardStatementRepository.save(snapshot);

        return buildStatementDetailResponse(account, snapshot);
    }

    @Transactional
    public List<CreditCardStatementSummaryResponse> getStatementHistory(String accountNumber) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        return creditCardStatementRepository.findByAccountNumberOrderByBillingDateDesc(accountNumber).stream()
                .map(statement -> refreshStatementPaymentStatus(account, statement))
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public CreditCardMonthlyStatementResponse getStatementDetail(String accountNumber, LocalDate billingDate) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        CreditCardStatement snapshot = creditCardStatementRepository.findByAccountNumberAndBillingDate(accountNumber, billingDate)
                .orElseThrow(() -> new RuntimeException("Statement not found for billingDate: " + billingDate));

        snapshot = refreshStatementPaymentStatus(account, snapshot);
        return buildStatementDetailResponse(account, snapshot);
    }

    private LocalDate normalizeBillingDate(LocalDate referenceDate, Integer billingDayOfMonth) {
        int configuredDay = safeInt(billingDayOfMonth, 25);
        YearMonth yearMonth = YearMonth.from(referenceDate);
        int day = Math.min(configuredDay, yearMonth.lengthOfMonth());
        return yearMonth.atDay(day);
    }

    private CreditCardStatementSummaryResponse toSummaryResponse(CreditCardStatement statement) {
        return new CreditCardStatementSummaryResponse(
                statement.getId(),
                statement.getAccountNumber(),
                statement.getStatementPeriodStart(),
                statement.getStatementPeriodEnd(),
                statement.getBillingDate(),
                statement.getDueDate(),
                statement.getPreviousBalance(),
                statement.getTotalCharges(),
                statement.getTotalPayments(),
                statement.getMinimumDue(),
                statement.getNewBalance(),
                statement.getAvailableCreditAtBilling(),
                statement.getTransactionCount(),
                statement.getStatementStatus(),
                statement.getPaidAmountAfterStatement(),
                statement.getRemainingMinimumDue(),
                statement.getRemainingBalance(),
                statement.getLastPaymentDate(),
                statement.getCreatedAt()
        );
    }

    private CreditCardMonthlyStatementResponse buildStatementDetailResponse(LoanAccount account, CreditCardStatement snapshot) {
        LocalDateTime from = snapshot.getStatementPeriodStart().atStartOfDay();
        LocalDateTime to = snapshot.getStatementPeriodEnd().atTime(LocalTime.MAX);

        List<LoanStatementItemResponse> items = transactionRepository
                .findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                        account.getAccountNumber(),
                        "LOAN",
                        from,
                        to
                ).stream()
                .map(tx -> new LoanStatementItemResponse(
                        tx.getTransactionDate(),
                        tx.getPaymentId(),
                        tx.getTransactionType(),
                        tx.getMerchantId(),
                        tx.getMerchantName(),
                        tx.getAmount(),
                        tx.getBalanceAfter(),
                        tx.getStatus(),
                        tx.getResponseCode(),
                        tx.getResponseMessage()
                ))
                .toList();

        return new CreditCardMonthlyStatementResponse(
                snapshot.getId(),
                snapshot.getCreatedAt(),
                account.getAccountNumber(),
                account.getCurrency(),
                snapshot.getStatementPeriodStart(),
                snapshot.getStatementPeriodEnd(),
                snapshot.getStatementPeriodStart() + " to " + snapshot.getStatementPeriodEnd(),
                snapshot.getBillingDate(),
                snapshot.getDueDate(),
                safe(account.getPrincipal()),
                snapshot.getPreviousBalance(),
                snapshot.getTotalCharges(),
                snapshot.getTotalPayments(),
                snapshot.getMinimumDue(),
                snapshot.getNewBalance(),
                snapshot.getAvailableCreditAtBilling(),
                snapshot.getTransactionCount(),
                safeInt(account.getBillingDayOfMonth(), 25),
                safeInt(account.getPaymentDueDays(), 20),
                safe(account.getMinimumPaymentRate()),
                safe(account.getMinimumPaymentFloor()),
                snapshot.getStatementStatus(),
                snapshot.getPaidAmountAfterStatement(),
                snapshot.getRemainingMinimumDue(),
                snapshot.getRemainingBalance(),
                snapshot.getLastPaymentDate(),
                items
        );
    }

    private CreditCardStatement refreshStatementPaymentStatus(LoanAccount account, CreditCardStatement snapshot) {
        LocalDateTime afterBilling = snapshot.getBillingDate().atTime(LocalTime.MAX);
        List<Transaction> postStatementTransactions = transactionRepository
                .findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                        account.getAccountNumber(),
                        "LOAN",
                        afterBilling
                );

        double paidAmountAfterStatement = 0.0;
        LocalDateTime lastPaymentDate = null;

        for (Transaction tx : postStatementTransactions) {
            if (!"SUCCESS".equalsIgnoreCase(tx.getStatus())) {
                continue;
            }
            if ("PAYMENT".equalsIgnoreCase(tx.getTransactionType())) {
                paidAmountAfterStatement += safe(tx.getAmount());
                lastPaymentDate = tx.getTransactionDate();
            }
        }

        paidAmountAfterStatement = roundMoney(paidAmountAfterStatement);
        double remainingMinimumDue = roundMoney(Math.max(safe(snapshot.getMinimumDue()) - paidAmountAfterStatement, 0.0));
        double remainingBalance = roundMoney(Math.max(safe(snapshot.getNewBalance()) - paidAmountAfterStatement, 0.0));
        String statementStatus = determineStatementStatus(snapshot, paidAmountAfterStatement, remainingMinimumDue, remainingBalance);

        snapshot.setPaidAmountAfterStatement(paidAmountAfterStatement);
        snapshot.setRemainingMinimumDue(remainingMinimumDue);
        snapshot.setRemainingBalance(remainingBalance);
        snapshot.setStatementStatus(statementStatus);
        snapshot.setLastPaymentDate(lastPaymentDate);

        return creditCardStatementRepository.save(snapshot);
    }

    private String determineStatementStatus(CreditCardStatement snapshot,
                                            double paidAmountAfterStatement,
                                            double remainingMinimumDue,
                                            double remainingBalance) {
        if (remainingBalance <= 0) {
            return "PAID";
        }
        if (LocalDate.now().isAfter(snapshot.getDueDate()) && remainingMinimumDue > 0) {
            return "OVERDUE";
        }
        if (paidAmountAfterStatement > 0) {
            return "PARTIALLY_PAID";
        }
        return "OPEN";
    }

    private double calculateMinimumDue(double newBalance, double minimumPaymentRate, double minimumPaymentFloor) {
        if (newBalance <= 0) {
            return 0.0;
        }
        double percentageAmount = roundMoney(newBalance * minimumPaymentRate / 100.0);
        return roundMoney(Math.min(newBalance, Math.max(percentageAmount, minimumPaymentFloor)));
    }

    private double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private int safeInt(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
