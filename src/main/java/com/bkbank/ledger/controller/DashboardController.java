package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.response.ApiResponse;
import com.bkbank.ledger.dto.response.DashboardSummaryResponse;
import com.bkbank.ledger.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ApiResponse<DashboardSummaryResponse> getSummary(
            @RequestParam(defaultValue = "14") int days
    ) {
        return ApiResponse.success(dashboardService.getSummary(days));
    }
}
