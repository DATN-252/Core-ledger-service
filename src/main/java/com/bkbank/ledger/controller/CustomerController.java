package com.bkbank.ledger.controller;

import com.bkbank.ledger.client.CmsClient;
import com.bkbank.ledger.dto.request.PushNotificationTestRequest;
import com.bkbank.ledger.dto.request.StatementPaymentRequest;
import com.bkbank.ledger.dto.request.PushTokenRegistrationRequest;
import com.bkbank.ledger.dto.request.PushTokenUnregisterRequest;
import com.bkbank.ledger.dto.response.ApiResponse;
import com.bkbank.ledger.dto.response.CreditCardMonthlyStatementResponse;
import com.bkbank.ledger.dto.response.CreditCardStatementSummaryResponse;
import com.bkbank.ledger.dto.response.LoanStatementResponse;
import com.bkbank.ledger.dto.response.StatementPaymentResponse;
import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.User;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.UserRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import com.bkbank.ledger.service.ClientService;
import com.bkbank.ledger.service.CreditCardStatementService;
import com.bkbank.ledger.service.PushDeviceTokenService;
import com.bkbank.ledger.service.PushNotificationService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final TransactionRepository transactionRepository;
    private final StatementService statementService;
    private final CreditCardStatementService creditCardStatementService;
    private final PushDeviceTokenService pushDeviceTokenService;
    private final PushNotificationService pushNotificationService;

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

    @GetMapping("/me/statement")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CreditCardMonthlyStatementResponse>> getMyStatement(
            Authentication authentication,
            @RequestParam(required = false) String loanId,
            @RequestParam(required = false) LocalDate billingDate) {
        try {
            Client client = getAuthenticatedClient(authentication);
            String resolvedLoanId = resolveStatementLoanId(client, loanId);
            CreditCardMonthlyStatementResponse statement = billingDate != null
                    ? creditCardStatementService.getOrGenerateStatement(resolvedLoanId, billingDate)
                    : creditCardStatementService.getCurrentStatement(resolvedLoanId);
            return ResponseEntity.ok(ApiResponse.success(statement));
        } catch (Exception e) {
            log.error("Error getting my statement: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @GetMapping("/me/statements")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<CreditCardStatementSummaryResponse>>> getMyStatements(
            Authentication authentication,
            @RequestParam(required = false) String loanId) {
        try {
            Client client = getAuthenticatedClient(authentication);
            String resolvedLoanId = resolveStatementLoanId(client, loanId);
            List<CreditCardStatementSummaryResponse> statements = creditCardStatementService.getStatementHistory(resolvedLoanId);
            return ResponseEntity.ok(ApiResponse.success(statements));
        } catch (Exception e) {
            log.error("Error getting my statements: {}", e.getMessage());
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

    @PostMapping("/me/loans/{loanId}/monthly-statements/{billingDate}/payments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<StatementPaymentResponse>> payMyMonthlyStatement(
            Authentication authentication,
            @PathVariable String loanId,
            @PathVariable LocalDate billingDate,
            @RequestBody StatementPaymentRequest request) {
        try {
            Client client = getAuthenticatedClient(authentication);
            ensureLoanBelongsToClient(client, loanId);
            if (request.getPaymentSource() != null && !"INTERNAL_SAVINGS".equalsIgnoreCase(request.getPaymentSource())) {
                throw new RuntimeException("Khach hang chi duoc thanh toan sao ke tu tai khoan noi bo");
            }
            request.setPaymentSource("INTERNAL_SAVINGS");
            StatementPaymentResponse response = creditCardStatementService.payStatement(loanId, billingDate, request);
            return ResponseEntity.ok(ApiResponse.success("Thanh toan sao ke thanh cong", response));
        } catch (Exception e) {
            log.error("Error paying monthly statement: {}", e.getMessage());
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

    private String resolveStatementLoanId(Client client, String loanId) {
        if (loanId != null && !loanId.isBlank()) {
            ensureLoanBelongsToClient(client, loanId);
            return loanId;
        }

        List<LoanAccount> loanAccounts = clientService.getClientLoanAccounts(client.getClientId());
        if (loanAccounts.isEmpty()) {
            throw new RuntimeException("Khach hang khong co tai khoan vay nao");
        }
        if (loanAccounts.size() > 1) {
            throw new RuntimeException("Vui long truyen loanId vi khach hang co nhieu the tin dung");
        }
        return loanAccounts.get(0).getAccountNumber();
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

    @PostMapping("/push-token")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> savePushToken(
            Authentication authentication,
            @RequestBody PushTokenRegistrationRequest request) {
        try {
            Client client = getAuthenticatedClient(authentication);
            var token = pushDeviceTokenService.registerToken(client, request);

            Map<String, Object> response = new HashMap<>();
            response.put("id", token.getId());
            response.put("expoPushToken", token.getExpoPushToken());
            response.put("deviceId", token.getDeviceId());
            response.put("platform", token.getPlatform());
            response.put("status", token.getStatus().name());
            return ResponseEntity.ok(ApiResponse.success("Luu push token thanh cong", response));
        } catch (Exception e) {
            log.error("Error saving push token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PostMapping("/push-token/test")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testPushNotification(
            Authentication authentication,
            @RequestBody(required = false) PushNotificationTestRequest request) {
        try {
            Client client = getAuthenticatedClient(authentication);

            String title = request != null && request.getTitle() != null && !request.getTitle().isBlank()
                    ? request.getTitle().trim()
                    : "Test BKBank";
            String body = request != null && request.getBody() != null && !request.getBody().isBlank()
                    ? request.getBody().trim()
                    : "Thong bao test tu ledger-service";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("source", "ledger-test-api");
            payload.put("clientId", client.getClientId());
            payload.put("message", "Test push notification");
            if (request != null && request.getData() != null && !request.getData().isEmpty()) {
                payload.putAll(request.getData());
            }

            Map<String, Object> response = new LinkedHashMap<>();
            if (request != null && request.getExpoPushToken() != null && !request.getExpoPushToken().isBlank()) {
                String expoPushToken = request.getExpoPushToken().trim();
                pushNotificationService.sendToSingleToken(expoPushToken, title, body, payload);
                response.put("mode", "single-token");
                response.put("expoPushToken", expoPushToken);
                response.put("sentCount", 1);
            } else {
                int sentCount = pushNotificationService.sendToClientTokens(client.getClientId(), title, body, payload);
                response.put("mode", "active-tokens");
                response.put("clientId", client.getClientId());
                response.put("sentCount", sentCount);
            }

            response.put("title", title);
            response.put("body", body);
            response.put("data", payload);
            return ResponseEntity.ok(ApiResponse.success("Gui test push notification thanh cong", response));
        } catch (Exception e) {
            log.error("Error sending test push notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @DeleteMapping("/push-token")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> deletePushToken(
            Authentication authentication,
            @RequestBody PushTokenUnregisterRequest request) {
        try {
            Client client = getAuthenticatedClient(authentication);
            pushDeviceTokenService.unregisterToken(client, request);
            return ResponseEntity.ok(ApiResponse.success("Xoa push token thanh cong", "OK"));
        } catch (Exception e) {
            log.error("Error deleting push token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}

