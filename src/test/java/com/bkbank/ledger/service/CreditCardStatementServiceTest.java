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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @BeforeEach
    void setUp() {
        billingDate = LocalDate.now().minusDays(25);

        account = new LoanAccount();
        account.setAccountNumber("C9001");
        account.setPrincipal(100000.0);
        account.setPrincipalOutstanding(1300.0);
        account.setCurrency("USD");
        account.setBillingDayOfMonth(billingDate.getDayOfMonth());
        account.setPaymentDueDays(10);
        account.setMinimumPaymentRate(5.0);
        account.setMinimumPaymentFloor(10.0);
        account.setStatementInterestRateMonthly(2.5);
        account.setStatementLateFeeFixed(15.0);
        account.setStatus(AccountStatus.ACTIVE);

        snapshot = new CreditCardStatement();
        snapshot.setId(66L);
        snapshot.setAccountNumber(account.getAccountNumber());
        snapshot.setStatementPeriodStart(billingDate.minusMonths(1).plusDays(1));
        snapshot.setStatementPeriodEnd(billingDate);
        snapshot.setBillingDate(billingDate);
        snapshot.setDueDate(billingDate.plusDays(10));
        snapshot.setPreviousBalance(1000.0);
        snapshot.setTotalCharges(500.0);
        snapshot.setTotalPayments(200.0);
        snapshot.setMinimumDue(65.0);
        snapshot.setNewBalance(1300.0);
        snapshot.setInterestRateMonthly(2.5);
        snapshot.setInterestCharged(0.0);
        snapshot.setLateFeeFixed(15.0);
        snapshot.setLateFeeCharged(0.0);
        snapshot.setAvailableCreditAtBilling(98700.0);
        snapshot.setTransactionCount(2);
        snapshot.setStatementStatus("OPEN");
        snapshot.setPaidAmountAfterStatement(0.0);
        snapshot.setRemainingMinimumDue(65.0);
        snapshot.setRemainingBalance(1300.0);
        snapshot.setCreatedAt(LocalDateTime.now().minusDays(20));
    }

    @Test
    void getStatementDetail_appliesInterestAndLateFeeOnceForOverdueUnpaidStatement() {
        Transaction openingTx = transaction("OPENING", "CHARGE", 0.0, 1000.0, snapshot.getStatementPeriodStart().minusDays(1).atStartOfDay());
        Transaction chargeTx = transaction("CHARGE-1", "CHARGE", 500.0, 1500.0, billingDate.minusDays(5).atTime(10, 0));
        Transaction paymentTx = transaction("PAY-1", "PAYMENT", 200.0, 1300.0, billingDate.minusDays(3).atTime(11, 0));
        Transaction interestTx = transaction("INT-1", "INTEREST", 32.5, 1332.5, snapshot.getDueDate().plusDays(1).atTime(9, 0));
        interestTx.setChannel("STATEMENT_INTEREST");
        interestTx.setExternalReference(billingDate.toString());
        Transaction lateFeeTx = transaction("LATE-1", "LATE_FEE", 15.0, 1347.5, snapshot.getDueDate().plusDays(1).atTime(9, 5));
        lateFeeTx.setChannel("STATEMENT_LATE_FEE");
        lateFeeTx.setExternalReference(billingDate.toString());

        when(loanAccountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));
        when(creditCardStatementRepository.findByAccountNumberAndBillingDate(account.getAccountNumber(), billingDate))
                .thenReturn(Optional.of(snapshot));
        when(creditCardStatementRepository.save(any(CreditCardStatement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(openingTx));
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(chargeTx, paymentTx));
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(List.of(), List.of(interestTx), List.of(interestTx, lateFeeTx));

        CreditCardMonthlyStatementResponse response =
                creditCardStatementService.getStatementDetail(account.getAccountNumber(), billingDate);

        assertNotNull(response);
        assertEquals(1300.0, response.getNewBalance(), 0.001);
        assertEquals(32.5, response.getInterestCharged(), 0.001);
        assertEquals(15.0, response.getLateFeeCharged(), 0.001);
        assertEquals(1347.5, response.getRemainingBalance(), 0.001);
        assertEquals(65.0, response.getRemainingMinimumDue(), 0.001);
        assertEquals("OVERDUE", response.getStatementStatus());
        assertNotNull(response.getInterestAppliedAt());
        assertNotNull(response.getLateFeeAppliedAt());

        verify(loanAccountService).applyStatementInterest(account.getAccountNumber(), 32.5, billingDate.toString(), 2.5);
        verify(loanAccountService).applyStatementLateFee(account.getAccountNumber(), 15.0, billingDate.toString());
    }

    @Test
    void getStatementDetail_doesNotApplyDuplicateChargesWhenAlreadyApplied() {
        snapshot.setInterestCharged(32.5);
        snapshot.setInterestAppliedAt(LocalDateTime.now().minusDays(1));
        snapshot.setLateFeeCharged(15.0);
        snapshot.setLateFeeAppliedAt(LocalDateTime.now().minusDays(1));

        Transaction openingTx = transaction("OPENING", "CHARGE", 0.0, 1000.0, snapshot.getStatementPeriodStart().minusDays(1).atStartOfDay());
        Transaction chargeTx = transaction("CHARGE-1", "CHARGE", 500.0, 1500.0, billingDate.minusDays(5).atTime(10, 0));
        Transaction paymentTx = transaction("PAY-1", "PAYMENT", 200.0, 1300.0, billingDate.minusDays(3).atTime(11, 0));
        Transaction interestTx = transaction("INT-1", "INTEREST", 32.5, 1332.5, snapshot.getDueDate().plusDays(1).atTime(9, 0));
        interestTx.setExternalReference(billingDate.toString());
        Transaction lateFeeTx = transaction("LATE-1", "LATE_FEE", 15.0, 1347.5, snapshot.getDueDate().plusDays(1).atTime(9, 5));
        lateFeeTx.setExternalReference(billingDate.toString());

        when(loanAccountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));
        when(creditCardStatementRepository.findByAccountNumberAndBillingDate(account.getAccountNumber(), billingDate))
                .thenReturn(Optional.of(snapshot));
        when(creditCardStatementRepository.save(any(CreditCardStatement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(openingTx));
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(chargeTx, paymentTx));
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(List.of(interestTx, lateFeeTx));

        CreditCardMonthlyStatementResponse response =
                creditCardStatementService.getStatementDetail(account.getAccountNumber(), billingDate);

        assertEquals(32.5, response.getInterestCharged(), 0.001);
        assertEquals(15.0, response.getLateFeeCharged(), 0.001);
        assertEquals(1347.5, response.getRemainingBalance(), 0.001);
        assertEquals("OVERDUE", response.getStatementStatus());

        verifyNoMoreInteractions(loanAccountService);
    }

    @Test
    void getStatementDetail_ignoresPostStatementChargesThatBelongToAnotherBillingCycle() {
        snapshot.setInterestCharged(20.0);
        snapshot.setInterestAppliedAt(LocalDateTime.now().minusDays(10));
        snapshot.setLateFeeCharged(15.0);
        snapshot.setLateFeeAppliedAt(LocalDateTime.now().minusDays(10));
        snapshot.setNewBalance(800.0);
        snapshot.setMinimumDue(40.0);

        Transaction chargeTx = transaction("CHARGE-1", "CHARGE", 800.0, 800.0, billingDate.minusDays(5).atTime(10, 0));
        Transaction statementInterestTx = transaction("INT-1", "INTEREST", 20.0, 820.0, snapshot.getDueDate().plusDays(1).atTime(9, 0));
        statementInterestTx.setExternalReference(billingDate.toString());
        Transaction statementLateFeeTx = transaction("LATE-1", "LATE_FEE", 15.0, 835.0, snapshot.getDueDate().plusDays(1).atTime(9, 5));
        statementLateFeeTx.setExternalReference(billingDate.toString());
        Transaction nextCycleInterestTx = transaction("INT-2", "INTEREST", 53.38, 2188.38, snapshot.getDueDate().plusDays(32).atTime(9, 0));
        nextCycleInterestTx.setExternalReference(billingDate.plusMonths(1).toString());
        Transaction nextCycleLateFeeTx = transaction("LATE-2", "LATE_FEE", 15.0, 2203.38, snapshot.getDueDate().plusDays(32).atTime(9, 5));
        nextCycleLateFeeTx.setExternalReference(billingDate.plusMonths(1).toString());

        when(loanAccountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));
        when(creditCardStatementRepository.findByAccountNumberAndBillingDate(account.getAccountNumber(), billingDate))
                .thenReturn(Optional.of(snapshot));
        when(creditCardStatementRepository.save(any(CreditCardStatement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(chargeTx));
        when(transactionRepository.findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
                eq(account.getAccountNumber()), eq("LOAN"), any(LocalDateTime.class)))
                .thenReturn(List.of(statementInterestTx, statementLateFeeTx, nextCycleInterestTx, nextCycleLateFeeTx));

        CreditCardMonthlyStatementResponse response =
                creditCardStatementService.getStatementDetail(account.getAccountNumber(), billingDate);

        assertEquals(835.0, response.getRemainingBalance(), 0.001);
        assertEquals(2, response.getPostStatementItems().size());
        assertEquals("INT-1", response.getPostStatementItems().get(0).getPaymentId());
        assertEquals("LATE-1", response.getPostStatementItems().get(1).getPaymentId());
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
