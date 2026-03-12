package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.CreditCardStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditCardStatementRepository extends JpaRepository<CreditCardStatement, Long> {
    Optional<CreditCardStatement> findByAccountNumberAndBillingDate(String accountNumber, LocalDate billingDate);
    List<CreditCardStatement> findByAccountNumberOrderByBillingDateDesc(String accountNumber);
}
