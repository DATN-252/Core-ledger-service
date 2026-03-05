package com.bkbank.ledger.controller;

import com.bkbank.ledger.client.CmsClient;
import com.bkbank.ledger.dto.ApiResponse;
import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.User;
import com.bkbank.ledger.repository.UserRepository;
import com.bkbank.ledger.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final UserRepository userRepository;
    private final ClientService clientService;
    private final CmsClient cmsClient;

    private Client getAuthenticatedClient(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Not authenticated");
        }
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        Client client = user.getClient();
        if (client == null) {
            throw new RuntimeException("Tài khoản không được liên kết với hồ sơ khách hàng nào");
        }
        return client;
    }

    /**
     * Lấy thông tin cá nhân của khách hàng đang đăng nhập
     * GET /customer/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(Authentication authentication) {
        try {
            Client client = getAuthenticatedClient(authentication);

            Map<String, Object> response = new HashMap<>();
            response.put("clientId", client.getClientId());
            response.put("fullName", client.getFullName());
            response.put("dateOfBirth", client.getDateOfBirth());
            response.put("gender", client.getGender());
            response.put("email", client.getEmail());
            response.put("phoneNumber", client.getPhoneNumber());
            response.put("address", client.getAddress());
            response.put("city", client.getCity());
            response.put("country", client.getCountry());
            response.put("status", client.getStatus());
            
            // Masked ID number for security
            String idNum = client.getIdNumber();
            if (idNum != null && idNum.length() > 4) {
                String masked = "*".repeat(idNum.length() - 4) + idNum.substring(idNum.length() - 4);
                response.put("idNumber", masked);
            }

            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thành công", response));
        } catch (Exception e) {
            log.error("Error getting profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * Lấy danh sách tài khoản thanh toán / tiết kiệm của KH
     * GET /customer/me/savings
     */
    @GetMapping("/me/savings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMySavingsAccounts(Authentication authentication) {
        try {
            Client client = getAuthenticatedClient(authentication);
            List<SavingsAccount> accounts = clientService.getClientSavingsAccounts(client.getClientId());
            
            List<Map<String, Object>> result = accounts.stream().map(acc -> {
                Map<String, Object> map = new HashMap<>();
                map.put("accountNumber", acc.getAccountNumber());
                map.put("balance", acc.getBalance());
                map.put("status", acc.getStatus());
                map.put("createdAt", acc.getCreatedAt());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error getting savings accounts: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * Lấy danh sách tài khoản vay (thẻ tín dụng) của KH
     * GET /customer/me/loans
     */
    @GetMapping("/me/loans")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyLoanAccounts(Authentication authentication) {
        try {
            Client client = getAuthenticatedClient(authentication);
            List<LoanAccount> accounts = clientService.getClientLoanAccounts(client.getClientId());
            
            List<Map<String, Object>> result = accounts.stream().map(acc -> {
                Map<String, Object> map = new HashMap<>();
                map.put("accountNumber", acc.getAccountNumber());
                map.put("principal", acc.getPrincipal());
                map.put("principalOutstanding", acc.getPrincipalOutstanding());
                map.put("status", acc.getStatus());
                map.put("createdAt", acc.getCreatedAt());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error getting loan accounts: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * Lấy danh sách thẻ của KH (từ CMS service)
     * GET /customer/me/cards
     */
    @GetMapping("/me/cards")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyCards(Authentication authentication) {
        try {
            Client client = getAuthenticatedClient(authentication);

            // 1. Lấy tất cả account của Client (cả Savings(ghi nợ) và Loan(tín dụng))
            List<String> accountIds = new ArrayList<>();
            clientService.getClientSavingsAccounts(client.getClientId())
                    .forEach(acc -> accountIds.add(acc.getAccountNumber()));
            clientService.getClientLoanAccounts(client.getClientId())
                    .forEach(acc -> accountIds.add(acc.getAccountNumber()));

            // 2. Gọi sang CMS service lấy mảng thẻ
            List<Map<String, Object>> cards = cmsClient.getCardsByAccountIds(accountIds);

            return ResponseEntity.ok(ApiResponse.success(cards));
        } catch (Exception e) {
            log.error("Error getting cards: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}
