package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByMerchantId(String merchantId);
    Page<Merchant> findByStatus(Merchant.MerchantStatus status, Pageable pageable);
}
