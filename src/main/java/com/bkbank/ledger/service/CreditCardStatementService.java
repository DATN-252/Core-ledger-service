package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.request.StatementPaymentRequest;
import com.bkbank.ledger.dto.response.CreditCardMonthlyStatementResponse;
import com.bkbank.ledger.dto.response.CreditCardStatementSummaryResponse;
import com.bkbank.ledger.dto.response.LoanStatementItemResponse;
import com.bkbank.ledger.dto.response.StatementPaymentResponse;
import com.bkbank.ledger.entity.CreditCardStatement;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.PaymentAllocationLog;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.CreditCardStatementRepository;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.PaymentAllocationLogRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CreditCardStatementService {
    private final LoanAccountRepository loanAccountRepository;
    private final TransactionRepository transactionRepository;
    private final CreditCardStatementRepository creditCardStatementRepository;
    private final PaymentAllocationLogRepository paymentAllocationLogRepository;
    private final SavingsAccountService savingsAccountService;
    private final LoanAccountService loanAccountService;

    @Transactional
    public CreditCardMonthlyStatementResponse generateMonthlyStatement(String accountNumber, LocalDate billingDate) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        // validateBillingDateNotInFuture(billingDate);

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
            if ("CHARGE".equalsIgnoreCase(tx.getTransactionType())
                    || "INTEREST".equalsIgnoreCase(tx.getTransactionType())
                    || "LATE_FEE".equalsIgnoreCase(tx.getTransactionType())) {
                totalCharges += safe(tx.getAmount());
            } else if ("REFUND".equalsIgnoreCase(tx.getTransactionType())
                    || "REVERSAL".equalsIgnoreCase(tx.getTransactionType())) {
                totalCharges -= safe(tx.getAmount());
            } else if ("PAYMENT".equalsIgnoreCase(tx.getTransactionType())) {
                totalPayments += safe(tx.getAmount());
            }
        }

        double newBalance = roundMoney(previousBalance + totalCharges - totalPayments);
        double currentMinimumDue = calculateMinimumDue(
                newBalance,
                safe(account.getMinimumPaymentRate()),
                safe(account.getMinimumPaymentFloor())
        );
        double pastDueMinimum = calculatePastDueMinimumAtBilling(account, billingDate, null);
        double totalMinimumDueNow = roundMoney(currentMinimumDue + pastDueMinimum);
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
        snapshot.setMinimumDue(currentMinimumDue);
        snapshot.setCurrentMinimumDue(currentMinimumDue);
        snapshot.setPastDueMinimum(pastDueMinimum);
        snapshot.setTotalMinimumDueNow(totalMinimumDueNow);
        snapshot.setNewBalance(newBalance);
        snapshot.setInterestRateAnnual(resolveStatementInterestRate(account));
        snapshot.setInterestCharged(0.0);
        snapshot.setInterestAppliedAt(null);
        snapshot.setLateFeeRate(resolveStatementLateFeeRate(account));
        snapshot.setLateFeeFixed(resolveStatementLateFeeFloor(account));
        snapshot.setLateFeeCharged(0.0);
        snapshot.setLateFeeAppliedAt(null);
        snapshot.setAvailableCreditAtBilling(availableCredit);
        snapshot.setTransactionCount(items.size());
        snapshot.setStatementStatus("OPEN");
        snapshot.setPaidAmountAfterStatement(0.0);
        snapshot.setRemainingMinimumDue(totalMinimumDueNow);
        snapshot.setRemainingBalance(newBalance);
        snapshot.setLastPaymentDate(null);
        snapshot.setGracePeriodEligible(isGracePeriodEligible(snapshot));
        creditCardStatementRepository.save(snapshot);

        return buildStatementDetailResponse(account, snapshot);
    }

    @Transactional
    public List<CreditCardStatementSummaryResponse> getStatementHistory(String accountNumber) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        return creditCardStatementRepository.findByAccountNumberOrderByBillingDateDesc(accountNumber).stream()
                .map(statement -> syncSnapshotFromTransactions(account, statement))
                .map(statement -> refreshStatementPaymentStatus(account, statement))
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public CreditCardMonthlyStatementResponse getStatementDetail(String accountNumber, LocalDate billingDate) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        // validateBillingDateNotInFuture(billingDate);

        CreditCardStatement snapshot = creditCardStatementRepository.findByAccountNumberAndBillingDate(accountNumber, billingDate)
                .orElseThrow(() -> new RuntimeException("Statement not found for billingDate: " + billingDate));

        snapshot = syncSnapshotFromTransactions(account, snapshot);
        snapshot = refreshStatementPaymentStatus(account, snapshot);
        return buildStatementDetailResponse(account, snapshot);
    }

    @Transactional
    public CreditCardMonthlyStatementResponse getOrGenerateStatement(String accountNumber, LocalDate billingDate) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        validateBillingDateNotInFuture(billingDate);

        LocalDate normalizedBillingDate = normalizeBillingDate(billingDate, account.getBillingDayOfMonth());
        if (!normalizedBillingDate.equals(billingDate)) {
            throw new IllegalArgumentException("billingDate does not match account billing day");
        }

        if (creditCardStatementRepository.findByAccountNumberAndBillingDate(accountNumber, billingDate).isPresent()) {
            return getStatementDetail(accountNumber, billingDate);
        }

        return generateMonthlyStatement(accountNumber, billingDate);
    }

    @Transactional
    public CreditCardMonthlyStatementResponse getCurrentStatement(String accountNumber) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        LocalDate latestBillingDate = resolveLatestBillingDate(account, LocalDate.now());
        return getOrGenerateStatement(accountNumber, latestBillingDate);
    }

    @Transactional
    public StatementPaymentResponse payStatement(String accountNumber, LocalDate billingDate, StatementPaymentRequest request) {
        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        CreditCardStatement snapshot = creditCardStatementRepository.findByAccountNumberAndBillingDate(accountNumber, billingDate)
                .orElseThrow(() -> new RuntimeException("Statement not found for billingDate: " + billingDate));

        snapshot = syncSnapshotFromTransactions(account, snapshot);
        snapshot = refreshStatementPaymentStatus(account, snapshot);

        double remainingMinimumDueBefore = roundMoney(safe(snapshot.getRemainingMinimumDue()));
        double remainingBalanceBefore = roundMoney(safe(snapshot.getRemainingBalance()));
        String statementStatusBefore = snapshot.getStatementStatus();

        if (remainingBalanceBefore <= 0) {
            throw new IllegalArgumentException("Statement is already fully paid");
        }

        String paymentOption = normalizeUpper(request.getPaymentOption(), "CUSTOM");
        String paymentSource = normalizeUpper(request.getPaymentSource(), "INTERNAL_SAVINGS");
        double paymentAmount = resolvePaymentAmount(paymentOption, request.getAmount(), remainingMinimumDueBefore, remainingBalanceBefore);

        Double sourceAccountBalanceAfter = null;
        String sourceAccountNumber = blankToNull(request.getSourceAccountNumber());

        if ("INTERNAL_SAVINGS".equals(paymentSource)) {
            if (sourceAccountNumber == null) {
                throw new IllegalArgumentException("sourceAccountNumber is required for INTERNAL_SAVINGS payments");
            }
            SavingsAccount sourceAccount = savingsAccountService.getAccount(sourceAccountNumber);
            if (sourceAccount.getClient() == null
                    || account.getClient() == null
                    || !sourceAccount.getClient().getClientId().equals(account.getClient().getClientId())) {
                throw new IllegalArgumentException("Source account does not belong to the same client");
            }

            sourceAccountBalanceAfter = savingsAccountService.withdrawStatementPayment(
                    sourceAccountNumber,
                    paymentAmount,
                    accountNumber,
                    billingDate.toString(),
                    request.getNote()
            ).getBalance();
        } else if (!"CASH_COUNTER".equals(paymentSource)) {
            throw new IllegalArgumentException("Unsupported paymentSource: " + paymentSource);
        }

        Transaction paymentTx = loanAccountService.makeStatementPayment(
                accountNumber,
                paymentAmount,
                billingDate.toString(),
                paymentSource,
                sourceAccountNumber,
                request.getNote()
        );

        snapshot = creditCardStatementRepository.findByAccountNumberAndBillingDate(accountNumber, billingDate)
                .orElseThrow(() -> new RuntimeException("Statement not found for billingDate: " + billingDate));
        snapshot = syncSnapshotFromTransactions(account, snapshot);
        snapshot = refreshStatementPaymentStatus(account, snapshot);

        // 📋 Log payment allocation waterfall
        logPaymentAllocation(
                accountNumber,
                paymentTx.getPaymentId(),
                billingDate,
                paymentAmount,
                remainingMinimumDueBefore,
                remainingBalanceBefore
        );

        return new StatementPaymentResponse(
                accountNumber,
                billingDate,
                paymentOption,
                paymentSource,
                sourceAccountNumber,
                paymentAmount,
                account.getCurrency(),
                statementStatusBefore,
                snapshot.getStatementStatus(),
                remainingMinimumDueBefore,
                roundMoney(safe(snapshot.getRemainingMinimumDue())),
                remainingBalanceBefore,
                roundMoney(safe(snapshot.getRemainingBalance())),
                sourceAccountBalanceAfter,
                paymentTx.getPaymentId(),
                paymentTx.getTransactionDate(),
                request.getNote()
        );
    }

    private LocalDate normalizeBillingDate(LocalDate referenceDate, Integer billingDayOfMonth) {
        int configuredDay = safeInt(billingDayOfMonth, 25);
        YearMonth yearMonth = YearMonth.from(referenceDate);
        int day = Math.min(configuredDay, yearMonth.lengthOfMonth());
        return yearMonth.atDay(day);
    }

    private LocalDate resolveLatestBillingDate(LoanAccount account, LocalDate referenceDate) {
        LocalDate candidate = normalizeBillingDate(referenceDate, account.getBillingDayOfMonth());
        if (candidate.isAfter(referenceDate)) {
            return normalizeBillingDate(referenceDate.minusMonths(1), account.getBillingDayOfMonth());
        }
        return candidate;
    }

    private void validateBillingDateNotInFuture(LocalDate billingDate) {
        if (billingDate != null && billingDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("billingDate cannot be in the future");
        }
    }

    private CreditCardStatementSummaryResponse toSummaryResponse(CreditCardStatement statement) {
        CreditCardStatementSummaryResponse response = new CreditCardStatementSummaryResponse();
        response.setStatementId(statement.getId());
        response.setAccountNumber(statement.getAccountNumber());
        response.setStatementPeriodStart(statement.getStatementPeriodStart());
        response.setStatementPeriodEnd(statement.getStatementPeriodEnd());
        response.setBillingDate(statement.getBillingDate());
        response.setDueDate(statement.getDueDate());
        response.setPreviousBalance(statement.getPreviousBalance());
        response.setTotalCharges(statement.getTotalCharges());
        response.setTotalPayments(statement.getTotalPayments());
        response.setMinimumDue(statement.getMinimumDue());
        response.setCurrentMinimumDue(statement.getCurrentMinimumDue());
        response.setPastDueMinimum(statement.getPastDueMinimum());
        response.setTotalMinimumDueNow(statement.getTotalMinimumDueNow());
        response.setNewBalance(statement.getNewBalance());
        response.setGracePeriodEligible(statement.getGracePeriodEligible());
        response.setInterestRateAnnual(statement.getInterestRateAnnual());
        response.setInterestCharged(statement.getInterestCharged());
        response.setInterestAppliedAt(statement.getInterestAppliedAt());
        response.setLateFeeRate(statement.getLateFeeRate());
        response.setLateFeeFixed(statement.getLateFeeFixed());
        response.setLateFeeCharged(statement.getLateFeeCharged());
        response.setLateFeeAppliedAt(statement.getLateFeeAppliedAt());
        response.setAvailableCredit(statement.getAvailableCreditAtBilling());
        response.setTransactionCount(statement.getTransactionCount());
        response.setStatementStatus(statement.getStatementStatus());
        response.setPaidAmountAfterStatement(statement.getPaidAmountAfterStatement());
        response.setRemainingMinimumDue(statement.getRemainingMinimumDue());
        response.setRemainingBalance(statement.getRemainingBalance());
        response.setLastPaymentDate(statement.getLastPaymentDate());
        response.setGeneratedAt(statement.getCreatedAt());
        return response;
    }

    private CreditCardStatement syncSnapshotFromTransactions(LoanAccount account, CreditCardStatement snapshot) {
        LocalDateTime from = snapshot.getStatementPeriodStart().atStartOfDay();
        LocalDateTime to = snapshot.getStatementPeriodEnd().atTime(LocalTime.MAX);

        double previousBalance = transactionRepository
                .findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                        account.getAccountNumber(),
                        "LOAN",
                        from
                )
                .map(Transaction::getBalanceAfter)
                .orElse(0.0);

        List<Transaction> transactions = transactionRepository
                .findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                        account.getAccountNumber(),
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
            if ("CHARGE".equalsIgnoreCase(tx.getTransactionType())
                    || "INTEREST".equalsIgnoreCase(tx.getTransactionType())
                    || "LATE_FEE".equalsIgnoreCase(tx.getTransactionType())) {
                totalCharges += safe(tx.getAmount());
            } else if ("REFUND".equalsIgnoreCase(tx.getTransactionType())
                    || "REVERSAL".equalsIgnoreCase(tx.getTransactionType())) {
                totalCharges -= safe(tx.getAmount());
            } else if ("PAYMENT".equalsIgnoreCase(tx.getTransactionType())) {
                totalPayments += safe(tx.getAmount());
            }
        }

        double newBalance = roundMoney(previousBalance + totalCharges - totalPayments);
        double currentMinimumDue = calculateMinimumDue(
                newBalance,
                safe(account.getMinimumPaymentRate()),
                safe(account.getMinimumPaymentFloor())
        );
        double pastDueMinimum = calculatePastDueMinimumAtBilling(account, snapshot.getBillingDate(), snapshot.getId());
        double totalMinimumDueNow = roundMoney(currentMinimumDue + pastDueMinimum);
        double availableCredit = roundMoney(safe(account.getPrincipal()) - newBalance);

        snapshot.setPreviousBalance(roundMoney(previousBalance));
        snapshot.setTotalCharges(roundMoney(totalCharges));
        snapshot.setTotalPayments(roundMoney(totalPayments));
        snapshot.setMinimumDue(currentMinimumDue);
        snapshot.setCurrentMinimumDue(currentMinimumDue);
        snapshot.setPastDueMinimum(pastDueMinimum);
        snapshot.setTotalMinimumDueNow(totalMinimumDueNow);
        snapshot.setNewBalance(newBalance);
        snapshot.setInterestRateAnnual(resolveStatementInterestRate(account));
        if (snapshot.getInterestCharged() == null) {
            snapshot.setInterestCharged(0.0);
        }
        snapshot.setLateFeeRate(resolveStatementLateFeeRate(account));
        snapshot.setLateFeeFixed(resolveStatementLateFeeFloor(account));
        if (snapshot.getLateFeeCharged() == null) {
            snapshot.setLateFeeCharged(0.0);
        }
        snapshot.setAvailableCreditAtBilling(availableCredit);
        snapshot.setTransactionCount(transactions.size());
        snapshot.setGracePeriodEligible(isGracePeriodEligible(snapshot));

        return creditCardStatementRepository.save(snapshot);
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
                .map(this::toStatementItem)
                .toList();

        List<LoanStatementItemResponse> postStatementItems = transactionRepository
                .findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                        account.getAccountNumber(),
                        "LOAN",
                        snapshot.getBillingDate().atTime(LocalTime.MAX)
                ).stream()
                .filter(tx -> isStatementLinkedPostBillingTransaction(snapshot, tx))
                .map(this::toStatementItem)
                .toList();

        CreditCardMonthlyStatementResponse response = new CreditCardMonthlyStatementResponse();
        response.setStatementId(snapshot.getId());
        response.setGeneratedAt(snapshot.getCreatedAt());
        response.setAccountNumber(account.getAccountNumber());
        response.setCurrency(account.getCurrency());
        response.setStatementPeriodStart(snapshot.getStatementPeriodStart());
        response.setStatementPeriodEnd(snapshot.getStatementPeriodEnd());
        response.setStatementPeriod(snapshot.getStatementPeriodStart() + " to " + snapshot.getStatementPeriodEnd());
        response.setBillingDate(snapshot.getBillingDate());
        response.setDueDate(snapshot.getDueDate());
        response.setCreditLimit(safe(account.getPrincipal()));
        response.setPreviousBalance(snapshot.getPreviousBalance());
        response.setTotalCharges(snapshot.getTotalCharges());
        response.setTotalPayments(snapshot.getTotalPayments());
        response.setMinimumDue(snapshot.getMinimumDue());
        response.setCurrentMinimumDue(snapshot.getCurrentMinimumDue());
        response.setPastDueMinimum(snapshot.getPastDueMinimum());
        response.setTotalMinimumDueNow(snapshot.getTotalMinimumDueNow());
        response.setRemainingCurrentMinimumDue(Math.max(safe(snapshot.getCurrentMinimumDue()) - Math.max(safe(snapshot.getPaidAmountAfterStatement()) - safe(snapshot.getPastDueMinimum()), 0.0), 0.0));
        response.setRemainingPastDueMinimum(Math.max(safe(snapshot.getPastDueMinimum()) - safe(snapshot.getPaidAmountAfterStatement()), 0.0));
        response.setNewBalance(snapshot.getNewBalance());
        response.setGracePeriodEligible(snapshot.getGracePeriodEligible());
        response.setInterestRateAnnual(snapshot.getInterestRateAnnual());
        response.setInterestCharged(snapshot.getInterestCharged());
        response.setInterestAppliedAt(snapshot.getInterestAppliedAt());
        response.setLateFeeRate(snapshot.getLateFeeRate());
        response.setLateFeeFixed(snapshot.getLateFeeFixed());
        response.setLateFeeCharged(snapshot.getLateFeeCharged());
        response.setLateFeeAppliedAt(snapshot.getLateFeeAppliedAt());
        response.setAvailableCredit(snapshot.getAvailableCreditAtBilling());
        response.setTransactionCount(snapshot.getTransactionCount());
        response.setBillingDayOfMonth(safeInt(account.getBillingDayOfMonth(), 25));
        response.setPaymentDueDays(safeInt(account.getPaymentDueDays(), 20));
        response.setMinimumPaymentRate(safe(account.getMinimumPaymentRate()));
        response.setMinimumPaymentFloor(safe(account.getMinimumPaymentFloor()));
        response.setStatementStatus(snapshot.getStatementStatus());
        response.setPaidAmountAfterStatement(snapshot.getPaidAmountAfterStatement());
        response.setRemainingMinimumDue(snapshot.getRemainingMinimumDue());
        response.setRemainingBalance(snapshot.getRemainingBalance());
        response.setLastPaymentDate(snapshot.getLastPaymentDate());
        response.setItems(items);
        response.setPostStatementItems(postStatementItems);
        return response;
    }
    private LoanStatementItemResponse toStatementItem(Transaction tx) {
        return new LoanStatementItemResponse(
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
        );
    }

    private CreditCardStatement refreshStatementPaymentStatus(LoanAccount account, CreditCardStatement snapshot) {
        snapshot.setGracePeriodEligible(isGracePeriodEligible(snapshot));

        PostStatementPaymentState paymentState = calculatePostStatementPaymentState(account, snapshot);
        snapshot = maybeApplyOverdueInterest(account, snapshot, paymentState);

        paymentState = calculatePostStatementPaymentState(account, snapshot);
        snapshot = maybeApplyOverdueLateFee(account, snapshot, paymentState);

        paymentState = calculatePostStatementPaymentState(account, snapshot);
        String statementStatus = determineStatementStatus(snapshot, paymentState);

        snapshot.setPaidAmountAfterStatement(paymentState.paidAmountAfterStatement());
        snapshot.setRemainingMinimumDue(paymentState.remainingMinimumDue());
        snapshot.setRemainingBalance(paymentState.remainingBalance());
        snapshot.setStatementStatus(statementStatus);
        snapshot.setLastPaymentDate(paymentState.lastPaymentDate());

        return creditCardStatementRepository.save(snapshot);
    }

    private CreditCardStatement maybeApplyOverdueInterest(LoanAccount account,
                                                          CreditCardStatement snapshot,
                                                          PostStatementPaymentState paymentState) {
        if (!LocalDate.now().isAfter(snapshot.getDueDate())) {
            return snapshot;
        }
        // ✅ FIX #1: Bỏ grace period check - interest LUÔN được tính khi quá hạn nếu còn balance
        // (trước đây nếu trả >= minimum nhưng < 100% thì eligible grace period = false nhưng logic ở đây lại return)
        
        // ✅ FIX #2: Cho phép recalculate interest hàng ngày thay vì chỉ 1 lần
        // Nếu InterestAppliedAt != null, vẫn recalculate nhưng không tạo transaction mới
        
        if (paymentState.remainingBalance() <= 0) {
            return snapshot;  // Đã trả 100% → không tính interest
        }

        double annualRate = safe(snapshot.getInterestRateAnnual());
        if (annualRate <= 0) {
            annualRate = resolveStatementInterestRate(account);
        }

        double interestAmount = calculateDailyAccruedInterest(account, snapshot, annualRate);
        if (interestAmount <= 0) {
            return snapshot;
        }

        // Chỉ tạo transaction INTEREST lần đầu tiên
        // Sau đó mỗi ngày chỉ update giá trị interestCharged (daily accrual)
        if (snapshot.getInterestAppliedAt() == null) {
            loanAccountService.applyStatementInterest(
                    account.getAccountNumber(),
                    interestAmount,
                    snapshot.getBillingDate().toString(),
                    annualRate
            );
        }

        snapshot.setInterestRateAnnual(annualRate);
        snapshot.setInterestCharged(interestAmount);  // Update lại giá trị mỗi ngày
        snapshot.setInterestAppliedAt(LocalDateTime.now());
        creditCardStatementRepository.save(snapshot);

        String accountNumber = account.getAccountNumber();
        LoanAccount reloadedAccount = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));
        return syncSnapshotFromTransactions(reloadedAccount, snapshot);
    }

    private CreditCardStatement maybeApplyOverdueLateFee(LoanAccount account,
                                                         CreditCardStatement snapshot,
                                                         PostStatementPaymentState paymentState) {
        if (!LocalDate.now().isAfter(snapshot.getDueDate())) {
            return snapshot;
        }
        if (snapshot.getLateFeeAppliedAt() != null || safe(snapshot.getLateFeeCharged()) > 0) {
            return snapshot;
        }
        if (paymentState.remainingMinimumDue() <= 0) {
            return snapshot;
        }

        double lateFeeRate = safe(snapshot.getLateFeeRate());
        if (lateFeeRate <= 0) {
            lateFeeRate = resolveStatementLateFeeRate(account);
        }
        double lateFeeFloor = safe(snapshot.getLateFeeFixed());
        if (lateFeeFloor <= 0) {
            lateFeeFloor = resolveStatementLateFeeFloor(account);
        }

        double lateFeeAmount = roundMoney(Math.max(paymentState.remainingMinimumDue() * lateFeeRate / 100.0, lateFeeFloor));
        if (lateFeeAmount <= 0) {
            return snapshot;
        }

        loanAccountService.applyStatementLateFee(
                account.getAccountNumber(),
                lateFeeAmount,
                snapshot.getBillingDate().toString()
        );

        snapshot.setLateFeeRate(lateFeeRate);
        snapshot.setLateFeeFixed(lateFeeFloor);
        snapshot.setLateFeeCharged(lateFeeAmount);
        snapshot.setLateFeeAppliedAt(LocalDateTime.now());
        creditCardStatementRepository.save(snapshot);

        String accountNumber = account.getAccountNumber();
        LoanAccount reloadedAccount = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));
        return syncSnapshotFromTransactions(reloadedAccount, snapshot);
    }

    private PostStatementPaymentState calculatePostStatementPaymentState(LoanAccount account, CreditCardStatement snapshot) {
        List<Transaction> postStatementTransactions = transactionRepository
                .findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                        account.getAccountNumber(),
                        "LOAN",
                        snapshot.getBillingDate().atTime(LocalTime.MAX)
                ).stream()
                .filter(tx -> isStatementLinkedPostBillingTransaction(snapshot, tx))
                .toList();

        double paidAmountAfterStatement = 0.0;
        double creditedByAdjustmentsAfterStatement = 0.0;
        double debitedByOverdueChargesAfterStatement = 0.0;
        LocalDateTime lastPaymentDate = null;

        for (Transaction tx : postStatementTransactions) {
            if (!"SUCCESS".equalsIgnoreCase(tx.getStatus())) {
                continue;
            }
            if ("PAYMENT".equalsIgnoreCase(tx.getTransactionType())) {
                paidAmountAfterStatement += safe(tx.getAmount());
                lastPaymentDate = tx.getTransactionDate();
            } else if ("REFUND".equalsIgnoreCase(tx.getTransactionType())
                    || "REVERSAL".equalsIgnoreCase(tx.getTransactionType())) {
                creditedByAdjustmentsAfterStatement += safe(tx.getAmount());
            } else if ("INTEREST".equalsIgnoreCase(tx.getTransactionType())
                    || "LATE_FEE".equalsIgnoreCase(tx.getTransactionType())) {
                debitedByOverdueChargesAfterStatement += safe(tx.getAmount());
            }
        }

        paidAmountAfterStatement = roundMoney(paidAmountAfterStatement);
        double appliedCreditsAfterStatement = roundMoney(paidAmountAfterStatement + creditedByAdjustmentsAfterStatement);
        double remainingPastDueMinimum = roundMoney(Math.max(safe(snapshot.getPastDueMinimum()) - appliedCreditsAfterStatement, 0.0));
        double remainingCreditsAfterPastDue = Math.max(appliedCreditsAfterStatement - safe(snapshot.getPastDueMinimum()), 0.0);
        double remainingCurrentMinimumDue = roundMoney(Math.max(safe(snapshot.getCurrentMinimumDue()) - remainingCreditsAfterPastDue, 0.0));
        double remainingMinimumDue = roundMoney(remainingPastDueMinimum + remainingCurrentMinimumDue);
        double remainingBalance = roundMoney(Math.max(
                safe(snapshot.getNewBalance()) + debitedByOverdueChargesAfterStatement - appliedCreditsAfterStatement,
                0.0
        ));
        return new PostStatementPaymentState(
                paidAmountAfterStatement,
                appliedCreditsAfterStatement,
                remainingCurrentMinimumDue,
                remainingPastDueMinimum,
                remainingMinimumDue,
                remainingBalance,
                lastPaymentDate
        );
    }

    private boolean isStatementLinkedPostBillingTransaction(CreditCardStatement snapshot, Transaction tx) {
        String statementReference = snapshot.getBillingDate() != null ? snapshot.getBillingDate().toString() : null;
        if (statementReference == null) {
            return false;
        }
        return Objects.equals(statementReference, tx.getExternalReference());
    }

    private String determineStatementStatus(CreditCardStatement snapshot,
                                            PostStatementPaymentState paymentState) {
        if (paymentState.remainingBalance() <= 0) {
            return "PAID";
        }
        if (LocalDate.now().isAfter(snapshot.getDueDate()) && paymentState.remainingMinimumDue() > 0) {
            return "OVERDUE";
        }
        if (paymentState.paidAmountAfterStatement() > 0 || LocalDate.now().isAfter(snapshot.getDueDate())) {
            return "PARTIALLY_PAID";
        }
        return "OPEN";
    }
    private double resolvePaymentAmount(String paymentOption,
                                        Double requestedAmount,
                                        double remainingMinimumDue,
                                        double remainingBalance) {
        return switch (paymentOption) {
            case "MINIMUM_DUE" -> {
                if (remainingMinimumDue <= 0) {
                    throw new IllegalArgumentException("Minimum due is already fully paid");
                }
                yield remainingMinimumDue;
            }
            case "STATEMENT_BALANCE" -> remainingBalance;
            case "CUSTOM" -> {
                double amount = safe(requestedAmount);
                if (amount <= 0) {
                    throw new IllegalArgumentException("Custom payment amount must be greater than 0");
                }
                if (amount > remainingBalance) {
                    throw new IllegalArgumentException("Payment exceeds remaining statement balance");
                }
                yield roundMoney(amount);
            }
            default -> throw new IllegalArgumentException("Unsupported paymentOption: " + paymentOption);
        };
    }

    private String normalizeUpper(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized != null ? normalized.trim().toUpperCase() : fallback;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private double calculateDailyAccruedInterest(LoanAccount account,
                                                 CreditCardStatement snapshot,
                                                 double annualRate) {
        double dailyRate = annualRate / 100.0 / 365.0;
        if (dailyRate <= 0) {
            return 0.0;
        }

        LocalDateTime from = snapshot.getStatementPeriodStart().atStartOfDay();
        // ✅ FIX #3: If overdue, calculate interest up to today; otherwise up to due date
        LocalDateTime to = LocalDate.now().isAfter(snapshot.getDueDate()) 
            ? LocalDateTime.now() 
            : snapshot.getDueDate().atTime(LocalTime.MAX);
        List<BalanceEvent> events = new ArrayList<>();

        transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                        account.getAccountNumber(),
                        "LOAN",
                        from,
                        to
                ).stream()
                .filter(tx -> "SUCCESS".equalsIgnoreCase(tx.getStatus()))
                .forEach(tx -> {
                    double delta = transactionDelta(tx);
                    if (delta != 0) {
                        events.add(new BalanceEvent(tx.getTransactionDate(), delta));
                    }
                });

        transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                        account.getAccountNumber(),
                        "LOAN",
                        snapshot.getBillingDate().atTime(LocalTime.MAX)
                ).stream()
                .filter(tx -> !tx.getTransactionDate().isAfter(to))
                .filter(tx -> isStatementLinkedPostBillingTransaction(snapshot, tx))
                .filter(tx -> "SUCCESS".equalsIgnoreCase(tx.getStatus()))
                .filter(tx -> "PAYMENT".equalsIgnoreCase(tx.getTransactionType())
                        || "REFUND".equalsIgnoreCase(tx.getTransactionType())
                        || "REVERSAL".equalsIgnoreCase(tx.getTransactionType()))
                .forEach(tx -> events.add(new BalanceEvent(tx.getTransactionDate(), -safe(tx.getAmount()))));

        events.sort(Comparator.comparing(BalanceEvent::transactionTime));

        double runningBalance = safe(snapshot.getPreviousBalance());
        double interest = 0.0;
        LocalDate currentDate = snapshot.getStatementPeriodStart();
        // ✅ FIX #3: Include today in accrual end if overdue
        LocalDate accrualEndExclusive = LocalDate.now().isAfter(snapshot.getDueDate()) 
            ? LocalDate.now().plusDays(1) 
            : snapshot.getDueDate().plusDays(1);

        for (BalanceEvent event : events) {
            LocalDate eventDate = event.transactionTime().toLocalDate();
            // ✅ FIX #3: Allow events after due date to be included in calculation
            if (eventDate.isAfter(accrualEndExclusive.minusDays(1))) {
                break;
            }
            if (eventDate.isAfter(currentDate)) {
                long days = ChronoUnit.DAYS.between(currentDate, eventDate);
                if (days > 0 && runningBalance > 0) {
                    interest += runningBalance * dailyRate * days;
                }
                currentDate = eventDate;
            }
            runningBalance = roundMoney(runningBalance + event.delta());
        }

        long trailingDays = ChronoUnit.DAYS.between(currentDate, accrualEndExclusive);
        if (trailingDays > 0 && runningBalance > 0) {
            interest += runningBalance * dailyRate * trailingDays;
        }

        return roundMoney(interest);
    }

    private double calculatePastDueMinimumAtBilling(LoanAccount account,
                                                    LocalDate billingDate,
                                                    Long currentStatementId) {
        LocalDateTime cutoff = billingDate.atTime(LocalTime.MAX);
        double total = 0.0;
        for (CreditCardStatement statement : creditCardStatementRepository.findByAccountNumberOrderByBillingDateDesc(account.getAccountNumber())) {
            if (statement.getBillingDate() == null || !statement.getBillingDate().isBefore(billingDate)) {
                continue;
            }
            if (currentStatementId != null && Objects.equals(currentStatementId, statement.getId())) {
                continue;
            }
            if (statement.getDueDate() == null || statement.getDueDate().isAfter(billingDate)) {
                continue;
            }
            double baseMinimum = safe(statement.getTotalMinimumDueNow()) > 0
                    ? safe(statement.getTotalMinimumDueNow())
                    : (safe(statement.getCurrentMinimumDue()) + safe(statement.getPastDueMinimum()) > 0
                    ? safe(statement.getCurrentMinimumDue()) + safe(statement.getPastDueMinimum())
                    : safe(statement.getMinimumDue()));
            double appliedCredits = calculateAppliedCreditsUntil(statement, cutoff);
            total += Math.max(baseMinimum - appliedCredits, 0.0);
        }
        return roundMoney(total);
    }

    private double calculateAppliedCreditsUntil(CreditCardStatement snapshot, LocalDateTime cutoff) {
        return roundMoney(
                transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                                snapshot.getAccountNumber(),
                                "LOAN",
                                snapshot.getBillingDate().atTime(LocalTime.MAX)
                        ).stream()
                        .filter(tx -> !tx.getTransactionDate().isAfter(cutoff))
                        .filter(tx -> isStatementLinkedPostBillingTransaction(snapshot, tx))
                        .filter(tx -> "SUCCESS".equalsIgnoreCase(tx.getStatus()))
                        .filter(tx -> "PAYMENT".equalsIgnoreCase(tx.getTransactionType())
                                || "REFUND".equalsIgnoreCase(tx.getTransactionType())
                                || "REVERSAL".equalsIgnoreCase(tx.getTransactionType()))
                        .mapToDouble(tx -> safe(tx.getAmount()))
                        .sum()
        );
    }

    private boolean isGracePeriodEligible(CreditCardStatement snapshot) {
        // ✅ Grace period = khách hàng đã trả đủ 100% balance trước/tại due date
        // Không chỉ trả minimum → mất grace period → có interest
        if (safe(snapshot.getNewBalance()) <= 0) {
            return true;  // Balance = 0 → grace period eligible
        }
        double appliedCredits = calculateAppliedCreditsUntil(snapshot, snapshot.getDueDate().atTime(LocalTime.MAX));
        // Chỉ eligible nếu (balance - credits) <= 0 tại due date
        return roundMoney(Math.max(safe(snapshot.getNewBalance()) - appliedCredits, 0.0)) <= 0;
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

    private double resolveStatementInterestRate(LoanAccount account) {
        double configuredRate = safe(account.getStatementInterestRateAnnual());
        return configuredRate > 0 ? configuredRate : 30.0;
    }

    private double resolveStatementLateFeeRate(LoanAccount account) {
        double configuredRate = safe(account.getStatementLateFeeRate());
        return configuredRate > 0 ? configuredRate : 4.0;
    }

    private double resolveStatementLateFeeFloor(LoanAccount account) {
        double configuredFee = safe(account.getStatementLateFeeFixed());
        return configuredFee > 0 ? configuredFee : 15.0;
    }

    private double transactionDelta(Transaction tx) {
        if ("CHARGE".equalsIgnoreCase(tx.getTransactionType())
                || "INTEREST".equalsIgnoreCase(tx.getTransactionType())
                || "LATE_FEE".equalsIgnoreCase(tx.getTransactionType())) {
            return safe(tx.getAmount());
        }
        if ("PAYMENT".equalsIgnoreCase(tx.getTransactionType())
                || "REFUND".equalsIgnoreCase(tx.getTransactionType())
                || "REVERSAL".equalsIgnoreCase(tx.getTransactionType())) {
            return -safe(tx.getAmount());
        }
        return 0.0;
    }

    private void logPaymentAllocation(
            String accountNumber,
            String paymentTransactionId,
            LocalDate statementBillingDate,
            double paymentAmount,
            double remainingMinimumDueBefore,
            double remainingBalanceBefore) {
        
        try {
            // ✅ Waterfall allocation logic:
            // Priority 1: Late Fee
            // Priority 2: Past Due Minimum
            // Priority 3: Current Interest
            // Priority 4: Current Balance
            
            double paymentRemaining = paymentAmount;
            
            // Estimate late fee (will be more accurate from actual statement)
            double lateFeeAmount = 0.0;
            if (remainingMinimumDueBefore > 0 && LocalDate.now().isAfter(statementBillingDate.plusDays(20))) {
                // Rough estimate - in reality this comes from statement
                lateFeeAmount = 15.0;  // Default minimum late fee
                paymentRemaining = Math.max(0, paymentRemaining - lateFeeAmount);
            }
            
            // Allocate to Past Due Minimum
            double allocatedToPastDueMin = 0.0;
            // Fetch statement to get actual past due minimum
            var statement = creditCardStatementRepository
                    .findByAccountNumberAndBillingDate(accountNumber, statementBillingDate)
                    .orElse(null);
            
            if (statement != null) {
                double pastDueMinimum = safe(statement.getPastDueMinimum());
                allocatedToPastDueMin = Math.min(paymentRemaining, pastDueMinimum);
                paymentRemaining = Math.max(0, paymentRemaining - allocatedToPastDueMin);
                
                // Allocate to Current Interest
                double currentInterest = safe(statement.getInterestCharged());
                double allocatedToInterest = Math.min(paymentRemaining, currentInterest);
                paymentRemaining = Math.max(0, paymentRemaining - allocatedToInterest);
                
                // Remaining goes to Balance
                double allocatedToBalance = paymentRemaining;
                
                // Create log entry
                PaymentAllocationLog log = new PaymentAllocationLog();
                log.setAccountNumber(accountNumber);
                log.setPaymentTransactionId(paymentTransactionId);
                log.setStatementBillingDate(statementBillingDate);
                log.setPaymentDate(LocalDate.now());
                log.setPaymentAmount(paymentAmount);
                
                log.setAllocatedToLateFee(lateFeeAmount);
                log.setAllocatedToPastDueMinimum(allocatedToPastDueMin);
                log.setAllocatedToCurrentInterest(allocatedToInterest);
                log.setAllocatedToCurrentBalance(allocatedToBalance);
                
                log.setRemainingBalance(Math.max(0, remainingBalanceBefore - allocatedToBalance));
                log.setRemainingMinimumDue(Math.max(0, remainingMinimumDueBefore - allocatedToPastDueMin));
                
                log.setAllocationTime(LocalDateTime.now());
                log.setAllocatedBy("SYSTEM");
                
                // Build readable details
                String details = String.format(
                    "Payment Allocation Waterfall for Statement %s\n" +
                    "================================\n" +
                    "Total Payment Amount: %.2f VND\n\n" +
                    "Waterfall Allocation:\n" +
                    "  1. Late Fee: %.2f VND\n" +
                    "  2. Past Due Minimum: %.2f VND\n" +
                    "  3. Current Interest: %.2f VND\n" +
                    "  4. Current Balance: %.2f VND\n\n" +
                    "Remaining After Payment:\n" +
                    "  - Balance: %.2f VND\n" +
                    "  - Minimum Due: %.2f VND\n",
                    statementBillingDate,
                    paymentAmount,
                    lateFeeAmount,
                    allocatedToPastDueMin,
                    allocatedToInterest,
                    allocatedToBalance,
                    Math.max(0, remainingBalanceBefore - allocatedToBalance),
                    Math.max(0, remainingMinimumDueBefore - allocatedToPastDueMin)
                );
                
                log.setAllocationDetails(details);
                log.setCreatedAt(LocalDateTime.now());
                
                paymentAllocationLogRepository.save(log);
            }
            
        } catch (Exception e) {
            // Log lỗi nhưng không fail payment - allocation logging không critical
            System.err.println("Warning: Failed to log payment allocation: " + e.getMessage());
        }
    }

    private record PostStatementPaymentState(
            double paidAmountAfterStatement,
            double appliedCreditsAfterStatement,
            double remainingCurrentMinimumDue,
            double remainingPastDueMinimum,
            double remainingMinimumDue,
            double remainingBalance,
            LocalDateTime lastPaymentDate
    ) {
    }

    private record BalanceEvent(LocalDateTime transactionTime, double delta) {
    }
}














