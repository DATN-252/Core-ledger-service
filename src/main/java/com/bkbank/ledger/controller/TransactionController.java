package com.bkbank.ledger.controller;

import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionRepository transactionRepository;

    /**
     * Get all transactions (for UI dashboard)
     * GET /transactions
     * Returns latest 50 transactions ordered by date desc
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<List<Transaction>> getAll() {
        List<Transaction> txns = transactionRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, 50,
                        org.springframework.data.domain.Sort.by("transactionDate").descending())
        ).getContent();
        return ResponseEntity.ok(txns);
    }

    /**
     * Get transactions for a specific account
     * GET /transactions?accountId=LOAN_001
     */
    @GetMapping(params = "accountId")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<List<Transaction>> getByAccount(@RequestParam String accountId) {
        return ResponseEntity.ok(
                transactionRepository.findByAccountNumberOrderByTransactionDateDesc(accountId)
        );
    }
}
