package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.MerchantSettlementBatchItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantSettlementBatchItemRepository extends JpaRepository<MerchantSettlementBatchItem, Long> {
    List<MerchantSettlementBatchItem> findByBatchIdOrderByTransactionDateAsc(Long batchId);

    @Query("""
            select i
            from MerchantSettlementBatchItem i
            join fetch i.batch b
            where i.transactionId = :transactionId
            order by i.id desc
            """)
    List<MerchantSettlementBatchItem> findWithBatchByTransactionIdOrderByIdDesc(Long transactionId);
}
