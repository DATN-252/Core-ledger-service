package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.response.MerchantSettlementPreviewResponse;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.repository.MerchantRepository;
import com.bkbank.ledger.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantRepository merchantRepository;
    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<Page<Merchant>> getAllActiveMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Merchant> merchants = merchantRepository.findByStatus(Merchant.MerchantStatus.ACTIVE, pageable);
        return ResponseEntity.ok(merchants);
    }

    @GetMapping("/{merchantId}/settlement/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> previewSettlement(
            @PathVariable String merchantId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(defaultValue = "0") Double feeRate
    ) {
        try {
            MerchantSettlementPreviewResponse preview = settlementService.previewSettlement(
                    merchantId,
                    fromDate,
                    toDate,
                    feeRate
            );
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
