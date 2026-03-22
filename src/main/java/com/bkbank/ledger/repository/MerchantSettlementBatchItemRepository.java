package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.MerchantSettlementBatchItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantSettlementBatchItemRepository extends JpaRepository<MerchantSettlementBatchItem, Long> {
    List<MerchantSettlementBatchItem> findByBatchIdOrderByTransactionDateAsc(Long batchId);
}
