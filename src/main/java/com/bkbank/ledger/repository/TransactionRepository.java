package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    
    Page<Transaction> findByAccountNumberOrderByTransactionDateDesc(String accountNumber, Pageable pageable);
    
    Page<Transaction> findByAccountNumberInOrderByTransactionDateDesc(List<String> accountNumbers, Pageable pageable);
    
    List<Transaction> findTop10ByAccountNumberOrderByTransactionDateDesc(String accountNumber);

    Optional<Transaction> findByPaymentId(String paymentId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    long countByStatus(String status);

    List<Transaction> findTop8ByOrderByTransactionDateDesc();

    List<Transaction> findByTransactionDateGreaterThanEqualOrderByTransactionDateAsc(LocalDateTime from);

    @Query("""
            select case when t.channel = 'SETTLEMENT' then 'SETTLEMENT' else t.transactionType end, count(t)
            from Transaction t
            group by case when t.channel = 'SETTLEMENT' then 'SETTLEMENT' else t.transactionType end
            """)
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

    Page<Transaction> findByMerchantIdOrderByTransactionDateDesc(String merchantId, Pageable pageable);

    List<Transaction> findByAccountNumberAndAccountTypeAndTransactionDateAfterOrderByTransactionDateAsc(
            String accountNumber,
            String accountType,
            LocalDateTime after
    );

    @Modifying
    @Query("""
            update Transaction t
            set t.transactionDate = :transactionDate
            where t.paymentId = :paymentId
            """)
    void updateTransactionDateByPaymentId(@Param("paymentId") String paymentId,
                                          @Param("transactionDate") LocalDateTime transactionDate);

    @Query("""
            select coalesce(avg(t.amount), 0.0)
            from Transaction t
            where t.accountNumber = :accountNumber
              and t.status = 'SUCCESS'
              and t.transactionDate >= :since
            """)
    Double getAverageAmountSince(@Param("accountNumber") String accountNumber,
                                 @Param("since") LocalDateTime since);

    @Query("""
            select count(t)
            from Transaction t
            where t.accountNumber = :accountNumber
              and t.status = 'SUCCESS'
              and t.transactionDate >= :since
            """)
    long countTransactionsSince(@Param("accountNumber") String accountNumber,
                                @Param("since") LocalDateTime since);
}

