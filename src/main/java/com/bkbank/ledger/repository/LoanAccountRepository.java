package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.enums.AccountStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long>, JpaSpecificationExecutor<LoanAccount> {
    
    Optional<LoanAccount> findByAccountNumber(String accountNumber);
    
    boolean existsByAccountNumber(String accountNumber);

    long countByStatus(AccountStatus status);

    @Query("select coalesce(sum(l.principal), 0) from LoanAccount l")
    Double sumPrincipal();

    @Query("select coalesce(sum(l.principalOutstanding), 0) from LoanAccount l")
    Double sumPrincipalOutstanding();
}
