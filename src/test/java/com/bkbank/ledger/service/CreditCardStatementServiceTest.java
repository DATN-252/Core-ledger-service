package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.response.CreditCardMonthlyStatementResponse;
import com.bkbank.ledger.entity.CreditCardStatement;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.entity.enums.AccountStatus;
import com.bkbank.ledger.repository.CreditCardStatementRepository;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCardStatementServiceTest {

    @Mock
    private LoanAccountRepository loanAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CreditCardStatementRepository creditCardStatementRepository;

    @Mock
    private SavingsAccountService savingsAccountService;

    @Mock
    private LoanAccountService loanAccountService;

    @InjectMocks
    private CreditCardStatementService creditCardStatementService;

    private LoanAccount account;
    private CreditCardStatement snapshot;
    private LocalDate billingDate;
    private LocalDate periodStart;
    private LocalDate dueDate;

    @BeforeEach
    void setUp() {
        billingDate = LocalDate.now().minusDays(20);
        periodStart = billingDate.minusDays(4);
        dueDate = billingDate.plusDays(5);

        account = new LoanAccount();
        account.setAccountNumber("C9001");
        account.setPrincipal(100000.0);
        account.setPrincipalOutstanding(1000.0);
        account.setCurrency("USD");
        account.setBillingDayOfMonth(billingDate.getDayOfMonth());
        account.setPaymentDueDays(5);
        account.setMinimumPaymentRate(5.0);
        account.setMinimumPaymentFloor(10.0);
        account.setStatementInterestRateAnnual(36.0);
        account.setStatementLateFeeRate(4.0);
        account.setStatementLateFeeFixed(15.0);
        account.setStatus(AccountStatus.ACTIVE);

        snapshot = new CreditCardStatement();
        snapshot.setId(66L);
        snapshot.setAccountNumber(account.getAccountNumber());
        snapshot.setStatementPeriodStart(periodStart);
        snapshot.setStatementPeriodEnd(billingDate);
        snapshot.setBillingDate(billingDate);
        snapshot.setDueDate(dueDate);
        snapshot.setPreviousBalance(0.0);
        snapshot.setTotalCharges(1000.0);
        snapshot.setTotalPayments(0.0);
        snapshot.setMinimumDue(50.0);
        snapshot.setCurrentMinimumDue(50.0);
        snapshot.setPastDueMinimum(0.0);
        snapshot.setTotalMinimumDueNow(50.0);
        snapshot.setNewBalance(1000.0);
        snapshot.setGracePeriodEligible(false);
        snapshot.setInterestRateAnnual(36.0);
        snapshot.setInterestCharged(0.0);
        snapshot.setInterestAppliedAt(null);
        snapshot.setLateFeeRate(4.0);
        snapshot.setLateFeeFixed(15.0);
        snapshot.setLateFeeCharged(0.0);
        snapshot.setLateFeeAppliedAt(null);
        snapshot.setAvailableCreditAtBilling(99000.0);
        snapshot.setTransactionCount(1);
        snapshot.setStatementStatus("OPEN");
        snapshot.setPaidAmountAfterStatement(0.0);
        snapshot.setRemainingMinimumDue(50.0);
        snapshot.setRemainingBalance(1000.0);
        snapshot.setLastPaymentDate(null);
        snapshot.setCreatedAt(LocalDateTime.now().minusDays(15));

        when(loanAccountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));
        when(creditCardStatementRepository.findByAccountNumberAndBillingDate(account.getAccountNumber(), billingDate))
                .thenReturn(Optional.of(snapshot));
        when(creditCardStatementRepository.save(any(CreditCardStatement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditCardStatementRepository.findByAccountNumberOrderByBillingDateDesc(account.getAccountNumber()))
                .thenReturn(List.of(snapshot));
    }

    @Test
    void getStatementDetail_doesNotApplyInterestOrLateFeeWhenFullyPaidBeforeDueDate() {
        Transaction chargeTx = transaction("CHARGE-1", "CHARGE", 1000.0, 1000.0, periodStart.atTime(9, 0));
        Transaction paymentTx = transaction("PAY-1", "PAYMENT", 1000.0, 0.0, billingDate.plusDays(1).atTime(10, 0));
        paymentTx.setChannel("STATEMENT_PAYMENT");
        paymentTx.setExternalReference(billingDate.toString());

        when(transactionRepository.findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(chargeTx));
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(List.of(paymentTx));

        CreditCardMonthlyStatementResponse response = creditCardStatementService.getStatementDetail(account.getAccountNumber(), billingDate);

        assertNotNull(response);
        assertEquals(1000.0, response.getNewBalance(), 0.001);
        assertEquals(0.0, response.getInterestCharged(), 0.001);
        assertEquals(0.0, response.getLateFeeCharged(), 0.001);
        assertEquals(0.0, response.getRemainingBalance(), 0.001);
        assertEquals(0.0, response.getRemainingMinimumDue(), 0.001);
        assertTrue(Boolean.TRUE.equals(response.getGracePeriodEligible()));
        assertEquals("PAID", response.getStatementStatus());

        verify(loanAccountService, never()).applyStatementInterest(any(), any(), any(), any());
        verify(loanAccountService, never()).applyStatementLateFee(any(), any(), any());
    }

    @Test
    void getStatementDetail_appliesDailyInterestButNotLateFeeWhenPaidAboveMinimumBeforeDueDate() {
        Transaction chargeTx = transaction("CHARGE-1", "CHARGE", 1000.0, 1000.0, periodStart.atTime(9, 0));
        Transaction paymentTx = transaction("PAY-1", "PAYMENT", 100.0, 900.0, billingDate.plusDays(2).atTime(10, 0));
        paymentTx.setChannel("STATEMENT_PAYMENT");
        paymentTx.setExternalReference(billingDate.toString());
        Transaction interestTx = transaction("INT-1", "INTEREST", 9.47, 909.47, dueDate.plusDays(1).atTime(9, 0));
        interestTx.setChannel("STATEMENT_INTEREST");
        interestTx.setExternalReference(billingDate.toString());

        when(transactionRepository.findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(chargeTx));
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(
                        List.of(paymentTx),
                        List.of(paymentTx),
                        List.of(paymentTx),
                        List.of(paymentTx, interestTx),
                        List.of(paymentTx, interestTx)
                );

        CreditCardMonthlyStatementResponse response = creditCardStatementService.getStatementDetail(account.getAccountNumber(), billingDate);

        assertNotNull(response);
        assertFalse(Boolean.TRUE.equals(response.getGracePeriodEligible()));
        assertEquals(1000.0, response.getNewBalance(), 0.001);
        assertEquals(9.47, response.getInterestCharged(), 0.001);
        assertEquals(0.0, response.getLateFeeCharged(), 0.001);
        assertEquals(0.0, response.getRemainingMinimumDue(), 0.001);
        assertEquals(909.47, response.getRemainingBalance(), 0.001);
        assertEquals("PARTIALLY_PAID", response.getStatementStatus());

        verify(loanAccountService).applyStatementInterest(account.getAccountNumber(), 9.47, billingDate.toString(), 36.0);
        verify(loanAccountService, never()).applyStatementLateFee(any(), any(), any());
    }

    @Test
    void getStatementDetail_appliesInterestAndLateFeeWhenPaymentIsBelowMinimum() {
        Transaction chargeTx = transaction("CHARGE-1", "CHARGE", 1000.0, 1000.0, periodStart.atTime(9, 0));
        Transaction paymentTx = transaction("PAY-1", "PAYMENT", 20.0, 980.0, billingDate.plusDays(2).atTime(10, 0));
        paymentTx.setChannel("STATEMENT_PAYMENT");
        paymentTx.setExternalReference(billingDate.toString());
        Transaction interestTx = transaction("INT-1", "INTEREST", 9.78, 989.78, dueDate.plusDays(1).atTime(9, 0));
        interestTx.setChannel("STATEMENT_INTEREST");
        interestTx.setExternalReference(billingDate.toString());
        Transaction lateFeeTx = transaction("LATE-1", "LATE_FEE", 15.0, 1004.78, dueDate.plusDays(1).atTime(9, 5));
        lateFeeTx.setChannel("STATEMENT_LATE_FEE");
        lateFeeTx.setExternalReference(billingDate.toString());

        when(transactionRepository.findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(chargeTx));
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(
                        List.of(paymentTx),
                        List.of(paymentTx),
                        List.of(paymentTx),
                        List.of(paymentTx, interestTx),
                        List.of(paymentTx, interestTx, lateFeeTx),
                        List.of(paymentTx, interestTx, lateFeeTx)
                );

        CreditCardMonthlyStatementResponse response = creditCardStatementService.getStatementDetail(account.getAccountNumber(), billingDate);

        assertNotNull(response);
        assertFalse(Boolean.TRUE.equals(response.getGracePeriodEligible()));
        assertEquals(9.78, response.getInterestCharged(), 0.001);
        assertEquals(15.0, response.getLateFeeCharged(), 0.001);
        assertEquals(30.0, response.getRemainingMinimumDue(), 0.001);
        assertEquals(1004.78, response.getRemainingBalance(), 0.001);
        assertEquals("OVERDUE", response.getStatementStatus());

        verify(loanAccountService).applyStatementInterest(account.getAccountNumber(), 9.78, billingDate.toString(), 36.0);
        verify(loanAccountService).applyStatementLateFee(account.getAccountNumber(), 15.0, billingDate.toString());
    }

    @Test
    void getStatementDetail_carriesPastDueMinimumFromPreviousStatementWithoutMergingIntoCurrentMinimum() {
        CreditCardStatement previousStatement = new CreditCardStatement();
        previousStatement.setId(65L);
        previousStatement.setAccountNumber(account.getAccountNumber());
        previousStatement.setBillingDate(billingDate.minusMonths(1));
        previousStatement.setDueDate(billingDate.minusDays(10));
        previousStatement.setMinimumDue(40.0);
        previousStatement.setCurrentMinimumDue(40.0);
        previousStatement.setPastDueMinimum(0.0);
        previousStatement.setTotalMinimumDueNow(40.0);

        Transaction chargeTx = transaction("CHARGE-1", "CHARGE", 1000.0, 1000.0, periodStart.atTime(9, 0));

        when(creditCardStatementRepository.findByAccountNumberOrderByBillingDateDesc(account.getAccountNumber()))
                .thenReturn(List.of(snapshot, previousStatement));
        when(transactionRepository.findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(chargeTx));
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        CreditCardMonthlyStatementResponse response = creditCardStatementService.getStatementDetail(account.getAccountNumber(), billingDate);

        assertEquals(50.0, response.getCurrentMinimumDue(), 0.001);
        assertEquals(40.0, response.getPastDueMinimum(), 0.001);
        assertEquals(90.0, response.getTotalMinimumDueNow(), 0.001);
        assertEquals(90.0, response.getRemainingMinimumDue(), 0.001);
    }

    private Transaction transaction(String paymentId,
                                    String transactionType,
                                    double amount,
                                    double balanceAfter,
                                    LocalDateTime transactionDate) {
        Transaction tx = new Transaction();
        tx.setAccountNumber(account.getAccountNumber());
        tx.setAccountType("LOAN");
        tx.setPaymentId(paymentId);
        tx.setTransactionType(transactionType);
        tx.setAmount(amount);
        tx.setCurrency(account.getCurrency());
        tx.setBalanceAfter(balanceAfter);
        tx.setStatus("SUCCESS");
        tx.setResponseCode("00");
        tx.setResponseMessage("Approved");
        tx.setTransactionDate(transactionDate);
        return tx;
    }
}