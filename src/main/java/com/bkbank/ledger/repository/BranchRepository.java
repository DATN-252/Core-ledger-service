package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.Branch;
import com.bkbank.ledger.entity.enums.BranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchId(String branchId);

    boolean existsByBranchId(String branchId);

    List<Branch> findByStatusOrderByBranchNameAsc(BranchStatus status);
}
