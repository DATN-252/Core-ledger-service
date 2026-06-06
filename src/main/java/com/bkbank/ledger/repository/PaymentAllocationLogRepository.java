package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.PaymentAllocationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentAllocationLogRepository extends JpaRepository<PaymentAllocationLog, Long> {

    @Query("SELECT p FROM PaymentAllocationLog p WHERE p.accountNumber = ?1 AND p.statementBillingDate = ?2 ORDER BY p.allocationTime DESC")
    List<PaymentAllocationLog> findByAccountAndStatement(String accountNumber, LocalDate billingDate);

    @Query("SELECT p FROM PaymentAllocationLog p WHERE p.accountNumber = ?1 ORDER BY p.allocationTime DESC LIMIT 10")
    List<PaymentAllocationLog> findRecentAllocations(String accountNumber);

    @Query("SELECT p FROM PaymentAllocationLog p WHERE p.accountNumber = ?1 AND p.paymentDate BETWEEN ?2 AND ?3 ORDER BY p.paymentDate DESC")
    List<PaymentAllocationLog> findByAccountAndDateRange(String accountNumber, LocalDate fromDate, LocalDate toDate);
}
