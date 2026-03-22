package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.MerchantSettlementBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface MerchantSettlementBatchRepository extends JpaRepository<MerchantSettlementBatch, Long> {

    Optional<MerchantSettlementBatch> findByIdAndMerchantId(Long id, String merchantId);

    Page<MerchantSettlementBatch> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    boolean existsByMerchantIdAndFromDateAndToDateAndStatusIn(
            String merchantId,
            LocalDate fromDate,
            LocalDate toDate,
            Collection<MerchantSettlementBatch.SettlementStatus> statuses
    );
}
