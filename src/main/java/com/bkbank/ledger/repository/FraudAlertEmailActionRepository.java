package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.FraudAlertEmailAction;
import com.bkbank.ledger.entity.enums.FraudAlertEmailActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FraudAlertEmailActionRepository extends JpaRepository<FraudAlertEmailAction, Long> {

    Optional<FraudAlertEmailAction> findByTokenHash(String tokenHash);

    List<FraudAlertEmailAction> findByFraudAlertIdAndStatus(Long fraudAlertId, FraudAlertEmailActionStatus status);
}
