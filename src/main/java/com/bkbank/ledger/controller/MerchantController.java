package com.bkbank.ledger.controller;

import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantRepository merchantRepository;

    @GetMapping
    public ResponseEntity<List<Merchant>> getAllActiveMerchants() {
        // Find all active merchants
        // (Assuming you might want all for POS simulator testing)
        // If the table grows large, this might need pagination, 
        // but for a limited demo list, findAll() is fine.
        List<Merchant> merchants = merchantRepository.findAll().stream()
                .filter(m -> m.getStatus() == Merchant.MerchantStatus.ACTIVE)
                .toList();
        
        return ResponseEntity.ok(merchants);
    }
}
