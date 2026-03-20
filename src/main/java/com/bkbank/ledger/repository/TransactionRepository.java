package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Page<Transaction> findByAccountNumberOrderByTransactionDateDesc(String accountNumber, Pageable pageable);
    
    Page<Transaction> findByAccountNumberInOrderByTransactionDateDesc(List<String> accountNumbers, Pageable pageable);
    
    List<Transaction> findTop10ByAccountNumberOrderByTransactionDateDesc(String accountNumber);

    Optional<Transaction> findByPaymentId(String paymentId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    long countByStatus(String status);

    List<Transaction> findTop8ByOrderByTransactionDateDesc();

    List<Transaction> findByTransactionDateGreaterThanEqualOrderByTransactionDateAsc(LocalDateTime from);

    @Query("select t.transactionType, count(t) from Transaction t group by t.transactionType")
    List<Object[]> countGroupedByTransactionType();

    List<Transaction> findByAccountNumberAndAccountTypeAndTransactionDateBetweenOrderByTransactionDateAsc(
            String accountNumber,
            String accountType,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<Transaction> findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
            String accountNumber,
            String accountType,
            LocalDateTime before
    );

    List<Transaction> findByMerchantIdAndStatusAndTransactionDateBetweenOrderByTransactionDateAsc(
            String merchantId,
            String status,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Transaction> findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
            String accountNumber,
            String accountType,
            LocalDateTime after
    );
}
