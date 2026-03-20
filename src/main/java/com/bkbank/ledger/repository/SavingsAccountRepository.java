package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {
    
    Optional<SavingsAccount> findByAccountNumber(String accountNumber);
    
    boolean existsByAccountNumber(String accountNumber);

    long countByStatus(AccountStatus status);
}
