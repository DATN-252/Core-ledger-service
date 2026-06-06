package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.request.MerchantCreateRequest;
import com.bkbank.ledger.dto.response.AutoSettlementRunResponse;
import com.bkbank.ledger.dto.response.MerchantSettlementAdjustmentResponse;
import com.bkbank.ledger.dto.response.MerchantSettlementBatchResponse;
import com.bkbank.ledger.dto.response.MerchantSettlementPreviewResponse;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.MerchantRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import com.bkbank.ledger.repository.spec.LedgerListSpecifications;
import com.bkbank.ledger.service.SettlementAdjustmentService;
import com.bkbank.ledger.service.SettlementService;
import com.bkbank.ledger.service.MerchantService;
import com.bkbank.ledger.util.PageableSortUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private static final Map<String, String> MERCHANT_SORT_MAPPINGS = Map.ofEntries(
            Map.entry("merchantid", "merchantId"),
            Map.entry("name", "name"),
            Map.entry("category", "category"),
            Map.entry("status", "status"),
            Map.entry("cityname", "cityReference.cityName"),
            Map.entry("createdat", "createdAt"),
            Map.entry("updatedat", "updatedAt"));

    private final MerchantRepository merchantRepository;
    private final MerchantService merchantService;
    private final TransactionRepository transactionRepository;
    private final SettlementAdjustmentService settlementAdjustmentService;
    private final SettlementService settlementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> createMerchant(@RequestBody MerchantCreateRequest request) {
        try {
            Merchant merchant = merchantService.createMerchant(request);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("merchantId", merchant.getMerchantId());
            response.put("name", merchant.getName());
            response.put("status", merchant.getStatus());
            response.put("settlementAccountNumber", merchant.getResolvedSettlementAccountNumber());
            response.put("message", "Merchant created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<Page<Map<String, Object>>> getAllActiveMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "merchantId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Pageable pageable = PageableSortUtils.createPageable(page, size, sortBy, sortDir, "merchantId",
                MERCHANT_SORT_MAPPINGS);
        Page<Merchant> merchants = merchantRepository.findAll(
                LedgerListSpecifications.merchantList(q, status, category),
                pageable);
        Page<Map<String, Object>> result = merchants.map(merchant -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("merchantId", merchant.getMerchantId());
            item.put("name", merchant.getName());
            item.put("category", merchant.getCategory());
            item.put("status", merchant.getStatus());
            item.put("address", merchant.getDisplayAddress());
            item.put("latitude", merchant.getLatitude());
            item.put("longitude", merchant.getLongitude());
            item.put("settlementAccountNumber", merchant.getResolvedSettlementAccountNumber());
            item.put("settlementAccountName", merchant.getResolvedSettlementAccountName());
            item.put("settlementBankName", merchant.getResolvedSettlementBankName());
            item.put("settlementAccountBalance", merchant.getResolvedSettlementAccountBalance());
            return item;
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{merchantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<?> getMerchantDetail(@PathVariable String merchantId) {
        try {
            Merchant merchant = merchantService.getActiveMerchant(merchantId);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("merchantId", merchant.getMerchantId());
            detail.put("name", merchant.getName());
            detail.put("category", merchant.getCategory());
            detail.put("status", merchant.getStatus());
            detail.put("addressLine", merchant.getAddressLine());
            detail.put("ward", merchant.getWard());
            detail.put("district", merchant.getDistrict());
            detail.put("postalCode", merchant.getPostalCode());
            detail.put("address", merchant.getDisplayAddress());
            detail.put("latitude", merchant.getLatitude());
            detail.put("longitude", merchant.getLongitude());
            detail.put("cityName",
                    merchant.getCityReference() != null ? merchant.getCityReference().getCityName() : null);
            detail.put("country",
                    merchant.getCityReference() != null ? merchant.getCityReference().getCountry() : null);
            detail.put("cityPopulation",
                    merchant.getCityReference() != null ? merchant.getCityReference().getPopulation() : null);
            detail.put("settlementAccountNumber", merchant.getResolvedSettlementAccountNumber());
            detail.put("settlementAccountName", merchant.getResolvedSettlementAccountName());
            detail.put("settlementBankName", merchant.getResolvedSettlementBankName());
            detail.put("settlementAccountBalance", merchant.getResolvedSettlementAccountBalance());
            detail.put("createdAt", merchant.getCreatedAt());
            detail.put("updatedAt", merchant.getUpdatedAt());
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{merchantId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<?> getMerchantTransactions(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            merchantService.getActiveMerchant(merchantId);
            Pageable pageable = PageableSortUtils.createPageable(page, size, sortBy, sortDir, "transactionDate", Map.of(
                    "date", "transactionDate",
                    "transactiondate", "transactionDate",
                    "amount", "amount",
                    "type", "transactionType",
                    "status", "status"));
            Page<Transaction> transactions = transactionRepository
                    .findByMerchantIdOrderByTransactionDateDesc(merchantId, pageable);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{merchantId}/settlement/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<?> previewSettlement(
            @PathVariable String merchantId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(defaultValue = "0") Double feeRate) {
        try {
            MerchantSettlementPreviewResponse preview = settlementService.previewSettlement(
                    merchantId,
                    fromDate,
                    toDate,
                    feeRate);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{merchantId}/settlements/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> generateSettlementBatch(
            @PathVariable String merchantId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(defaultValue = "0") Double feeRate,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String note = body != null ? (String) body.get("note") : null;
            MerchantSettlementBatchResponse batch = settlementService.generateSettlementBatch(
                    merchantId,
                    fromDate,
                    toDate,
                    feeRate,
                    note);
            return ResponseEntity.ok(batch);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{merchantId}/settlements/{batchId}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> executeSettlementBatch(
            @PathVariable String merchantId,
            @PathVariable Long batchId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String note = body != null ? (String) body.get("note") : null;
            MerchantSettlementBatchResponse batch = settlementService.executeSettlementBatch(merchantId, batchId, note);
            return ResponseEntity.ok(batch);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{merchantId}/settlements")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<?> getSettlementBatches(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageableSortUtils.createPageable(page, size, "createdAt", "desc", "createdAt",
                    Map.of("createdat", "createdAt"));
            return ResponseEntity.ok(settlementService.getSettlementBatches(merchantId, pageable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{merchantId}/settlements/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<?> getSettlementBatch(
            @PathVariable String merchantId,
            @PathVariable Long batchId) {
        try {
            return ResponseEntity.ok(settlementService.getSettlementBatch(merchantId, batchId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{merchantId}/settlement-adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<?> getSettlementAdjustments(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            merchantService.getActiveMerchant(merchantId);
            Pageable pageable = PageableSortUtils.createPageable(page, size, "createdAt", "desc", "createdAt",
                    Map.of("createdat", "createdAt"));
            return ResponseEntity.ok(settlementAdjustmentService.getAdjustments(merchantId, pageable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{merchantId}/settlement-adjustments/{adjustmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<?> getSettlementAdjustment(
            @PathVariable String merchantId,
            @PathVariable Long adjustmentId) {
        try {
            MerchantSettlementAdjustmentResponse adjustment = settlementAdjustmentService.getAdjustment(merchantId,
                    adjustmentId);
            return ResponseEntity.ok(adjustment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/settlements/auto-run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> runAutoSettlement(
            @RequestParam(required = false) LocalDate settlementDate,
            @RequestParam(defaultValue = "1.5") Double feeRate,
            @RequestParam(defaultValue = "true") boolean execute) {
        try {
            AutoSettlementRunResponse response = settlementService.runAutomaticSettlement(settlementDate, feeRate,
                    execute);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
