package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.MerchantSettlementAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantSettlementAdjustmentRepository extends JpaRepository<MerchantSettlementAdjustment, Long> {

    Page<MerchantSettlementAdjustment> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    Optional<MerchantSettlementAdjustment> findByIdAndMerchantId(Long id, String merchantId);

    List<MerchantSettlementAdjustment> findByMerchantIdAndStatusOrderByCreatedAtAsc(
            String merchantId,
            MerchantSettlementAdjustment.AdjustmentStatus status
    );

    List<MerchantSettlementAdjustment> findByMerchantIdAndStatusInAndReservedBatchIdOrderByCreatedAtAsc(
            String merchantId,
            Collection<MerchantSettlementAdjustment.AdjustmentStatus> statuses,
            Long reservedBatchId
    );

    boolean existsByAdjustmentTransactionId(Long adjustmentTransactionId);
}
