package com.bkbank.ledger.controller;

import com.bkbank.ledger.client.CmsClient;
import com.bkbank.ledger.dto.response.ApiResponse;
import com.bkbank.ledger.dto.response.CreditCardMonthlyStatementResponse;
import com.bkbank.ledger.dto.response.CreditCardStatementSummaryResponse;
import com.bkbank.ledger.dto.response.LoanStatementResponse;
import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.User;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.UserRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import com.bkbank.ledger.service.ClientService;
import com.bkbank.ledger.service.CreditCardStatementService;
import com.bkbank.ledger.service.StatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final UserRepository userRepository;
    private final ClientService clientService;
    private final CmsClient cmsClient;
    private final TransactionRepository transactionRepository;
    private final StatementService statementService;
    private final CreditCardStatementService creditCardStatementService;

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

    private void ensureLoanBelongsToClient(Client client, String loanId) {
        boolean owned = clientService.getClientLoanAccounts(client.getClientId()).stream()
                .map(LoanAccount::getAccountNumber)
                .anyMatch(loanId::equals);
        if (!owned) {
            throw new RuntimeException("Khong tim thay tai khoan vay cua khach hang");
        }
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
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getMySavingsAccounts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
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

            Pageable pageable = PageRequest.of(page, size);
            int start = Math.min((int) pageable.getOffset(), result.size());
            int end = Math.min((start + pageable.getPageSize()), result.size());
            Page<Map<String, Object>> pagedResult = new PageImpl<>(result.subList(start, end), pageable, result.size());

            return ResponseEntity.ok(ApiResponse.success(pagedResult));
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
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getMyLoanAccounts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
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

            Pageable pageable = PageRequest.of(page, size);
            int start = Math.min((int) pageable.getOffset(), result.size());
            int end = Math.min((start + pageable.getPageSize()), result.size());
            Page<Map<String, Object>> pagedResult = new PageImpl<>(result.subList(start, end), pageable, result.size());

            return ResponseEntity.ok(ApiResponse.success(pagedResult));
        } catch (Exception e) {
            log.error("Error getting loan accounts: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @GetMapping("/me/loans/{loanId}/statement")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<LoanStatementResponse>> getMyLoanStatement(
            Authentication authentication,
            @PathVariable String loanId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        try {
            Client client = getAuthenticatedClient(authentication);
            ensureLoanBelongsToClient(client, loanId);
            LoanStatementResponse statement = statementService.getLoanStatement(loanId, fromDate, toDate);
            return ResponseEntity.ok(ApiResponse.success(statement));
        } catch (Exception e) {
            log.error("Error getting loan statement: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @GetMapping("/me/loans/{loanId}/monthly-statement")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CreditCardMonthlyStatementResponse>> generateMyMonthlyStatement(
            Authentication authentication,
            @PathVariable String loanId,
            @RequestParam LocalDate billingDate) {
        try {
            Client client = getAuthenticatedClient(authentication);
            ensureLoanBelongsToClient(client, loanId);
            CreditCardMonthlyStatementResponse statement = creditCardStatementService.generateMonthlyStatement(loanId,
                    billingDate);
            return ResponseEntity.ok(ApiResponse.success(statement));
        } catch (Exception e) {
            log.error("Error generating monthly statement: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @GetMapping("/me/loans/{loanId}/monthly-statements")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<CreditCardStatementSummaryResponse>>> getMyMonthlyStatementHistory(
            Authentication authentication,
            @PathVariable String loanId) {
        try {
            Client client = getAuthenticatedClient(authentication);
            ensureLoanBelongsToClient(client, loanId);
            List<CreditCardStatementSummaryResponse> statements = creditCardStatementService
                    .getStatementHistory(loanId);
            return ResponseEntity.ok(ApiResponse.success(statements));
        } catch (Exception e) {
            log.error("Error getting monthly statement history: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @GetMapping("/me/loans/{loanId}/monthly-statements/{billingDate}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CreditCardMonthlyStatementResponse>> getMyMonthlyStatementDetail(
            Authentication authentication,
            @PathVariable String loanId,
            @PathVariable LocalDate billingDate) {
        try {
            Client client = getAuthenticatedClient(authentication);
            ensureLoanBelongsToClient(client, loanId);
            CreditCardMonthlyStatementResponse statement = creditCardStatementService.getStatementDetail(loanId,
                    billingDate);
            return ResponseEntity.ok(ApiResponse.success(statement));
        } catch (Exception e) {
            log.error("Error getting monthly statement detail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * Lấy danh sách thẻ của KH (từ CMS service)
     * GET /customer/me/cards
     */
    @GetMapping("/me/cards")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getMyCards(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Client client = getAuthenticatedClient(authentication);

            List<SavingsAccount> savingsAccounts = clientService.getClientSavingsAccounts(client.getClientId());
            List<LoanAccount> loanAccounts = clientService.getClientLoanAccounts(client.getClientId());

            Map<String, SavingsAccount> savingsByAccount = savingsAccounts.stream()
                    .collect(Collectors.toMap(SavingsAccount::getAccountNumber, acc -> acc));
            Map<String, LoanAccount> loansByAccount = loanAccounts.stream()
                    .collect(Collectors.toMap(LoanAccount::getAccountNumber, acc -> acc));

            List<String> accountIds = new ArrayList<>();
            accountIds.addAll(savingsByAccount.keySet());
            accountIds.addAll(loansByAccount.keySet());

            List<Map<String, Object>> cards = cmsClient.getCardsByAccountIds(accountIds);

            if (cards != null) {
                for (Map<String, Object> card : cards) {
                    enrichCardWithAccountData(card, savingsByAccount, loansByAccount);
                }
            } else {
                cards = new ArrayList<>();
            }

            Pageable pageable = PageRequest.of(page, size);
            int start = Math.min((int) pageable.getOffset(), cards.size());
            int end = Math.min((start + pageable.getPageSize()), cards.size());
            Page<Map<String, Object>> pagedResult = new PageImpl<>(cards.subList(start, end), pageable, cards.size());

            return ResponseEntity.ok(ApiResponse.success(pagedResult));
        } catch (Exception e) {
            log.error("Error getting cards: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    private void enrichCardWithAccountData(Map<String, Object> card,
                                           Map<String, SavingsAccount> savingsByAccount,
                                           Map<String, LoanAccount> loansByAccount) {
        String accountId = (String) card.get("accountId");
        String cardType = stringValue(card.get("cardType"));

        if (accountId == null || accountId.isBlank()) {
            return;
        }

        if ("DEBIT".equalsIgnoreCase(cardType)) {
            SavingsAccount savingsAccount = savingsByAccount.get(accountId);
            if (savingsAccount != null) {
                card.put("balance", savingsAccount.getBalance());
                card.put("currency", savingsAccount.getCurrency());
                card.put("accountStatus", savingsAccount.getStatus().name());
            }
            return;
        }

        if ("CREDIT".equalsIgnoreCase(cardType)) {
            LoanAccount loanAccount = loansByAccount.get(accountId);
            if (loanAccount != null) {
                double creditLimit = loanAccount.getPrincipal() != null ? loanAccount.getPrincipal() : 0.0;
                double outstandingBalance = loanAccount.getPrincipalOutstanding() != null ? loanAccount.getPrincipalOutstanding() : 0.0;
                card.put("creditLimit", creditLimit);
                card.put("outstandingBalance", outstandingBalance);
                card.put("availableCredit", Math.max(creditLimit - outstandingBalance, 0.0));
                card.put("currency", loanAccount.getCurrency());
                card.put("accountStatus", loanAccount.getStatus().name());
            }
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Lấy danh sách biến động số dư (Giao dịch) của KH
     * GET /customer/me/transactions
     */
    @GetMapping("/me/transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<Transaction>>> getMyTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Client client = getAuthenticatedClient(authentication);

            // 1. Get all account numbers (Savings & Loans)
            List<String> accountIds = new ArrayList<>();
            clientService.getClientSavingsAccounts(client.getClientId())
                    .forEach(acc -> accountIds.add(acc.getAccountNumber()));
            clientService.getClientLoanAccounts(client.getClientId())
                    .forEach(acc -> accountIds.add(acc.getAccountNumber()));

            // 2. Fetch transactions for these accounts
            Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
            Page<Transaction> transactions = transactionRepository
                    .findByAccountNumberInOrderByTransactionDateDesc(accountIds, pageable);

            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (Exception e) {
            log.error("Error getting transactions: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    // docs: https://docs.expo.dev/push-notifications/sending-notifications/
    public ResponseEntity<String> sendPushNotification(String expoPushToken, String title, String body,
            Map<String, Object> data) throws Exception {
        // Todo: format payload theo định dạng của Expo push notification service.
        // to: có thể gửi cho nhiều client [expoPushToken1, expoPushToken2, ...]
        String payload = """
                {
                  "to": "%s",
                  "title": "%s",
                  "body": "%s",
                  "data": %s
                }
                """.formatted(expoPushToken, title, body,
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://exp.host/--/api/v2/push/send"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to send push notification: " + response.body());
        }

        return ResponseEntity.ok("Notification sent successfully");
    }

    @PostMapping("/push-token")
    public void savePushToken(@RequestBody Map<String, String> body) throws Exception {
        // client gửi token-message của device
        String expoPushToken = body.get("token");
        if (expoPushToken == null || expoPushToken.isBlank()) {
            throw new IllegalArgumentException("Missing 'token' in request body");
        }

        // TODO: lưu token-message vào DB ở đây

        // todo: xóa 4 dòng dưới vì chỉ test client gửi token thành công hay không
        Map<String, Object> data = new HashMap<>();
        data.put("id", "123");
        data.put("message", "Trong đây là dữ liệu chi tiết để người dùng bấm vào xem");
        sendPushNotification(expoPushToken, "Thông báo từ BK-Bank", "Backend đã nhận được token ^^", data);
    }
}

