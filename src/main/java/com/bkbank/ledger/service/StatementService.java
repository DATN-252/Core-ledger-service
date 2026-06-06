package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.response.LoanStatementItemResponse;
import com.bkbank.ledger.dto.response.LoanStatementResponse;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final LoanAccountRepository loanAccountRepository;
    private final TransactionRepository transactionRepository;

    public LoanStatementResponse getLoanStatement(String accountNumber, LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be before or equal to toDate");
        }

        LoanAccount account = loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
                        accountNumber,
                        "LOAN",
                        from,
                        to
                );

        double openingOutstanding = transactionRepository
                .findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                        accountNumber,
                        "LOAN",
                        from
                )
                .map(Transaction::getBalanceAfter)
                .orElse(0.0);

        double totalCharges = 0.0;
        double totalPayments = 0.0;

        List<LoanStatementItemResponse> items = transactions.stream()
                .map(tx -> {
                    if (("CHARGE".equalsIgnoreCase(tx.getTransactionType())
                            || "INTEREST".equalsIgnoreCase(tx.getTransactionType())
                            || "LATE_FEE".equalsIgnoreCase(tx.getTransactionType()))
                            && "SUCCESS".equalsIgnoreCase(tx.getStatus())) {
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
                    if ("PAYMENT".equalsIgnoreCase(tx.getTransactionType()) && "SUCCESS".equalsIgnoreCase(tx.getStatus())) {
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
                })
                .toList();

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

        double closingOutstanding = transactions.isEmpty()
                ? openingOutstanding
                : safe(transactions.get(transactions.size() - 1).getBalanceAfter());

        return new LoanStatementResponse(
                account.getAccountNumber(),
                account.getCurrency(),
                fromDate,
                toDate,
                safe(account.getPrincipal()),
                openingOutstanding,
                totalCharges,
                totalPayments,
                closingOutstanding,
                safe(account.getAvailableCredit()),
                items.size(),
                items
        );
    }

    private double safe(Double value) {
        return value != null ? value : 0.0;
    }
}
