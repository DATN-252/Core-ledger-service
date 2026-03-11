package com.bkbank.ledger.controller;

import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantRepository merchantRepository;

    @GetMapping
    public ResponseEntity<Page<Merchant>> getAllActiveMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Merchant> merchants = merchantRepository.findByStatus(Merchant.MerchantStatus.ACTIVE, pageable);
        return ResponseEntity.ok(merchants);
    }
}
