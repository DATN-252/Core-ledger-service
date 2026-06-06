package com.bkbank.ledger.controller;

import com.bkbank.ledger.entity.Branch;
import com.bkbank.ledger.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<List<Map<String, Object>>> getBranches() {
        return ResponseEntity.ok(branchService.getActiveBranches().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<Map<String, Object>> getBranch(@PathVariable String branchId) {
        return ResponseEntity.ok(toResponse(branchService.getBranch(branchId)));
    }

    private Map<String, Object> toResponse(Branch branch) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("branchId", branch.getBranchId());
        response.put("branchCode", branch.getBranchCode());
        response.put("branchName", branch.getBranchName());
        response.put("branchType", branch.getBranchType());
        response.put("status", branch.getStatus());
        response.put("addressLine", branch.getAddressLine());
        response.put("cityName", branch.getCityName());
        response.put("phoneNumber", branch.getPhoneNumber());
        return response;
    }
}
