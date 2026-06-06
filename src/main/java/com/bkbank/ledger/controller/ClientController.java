package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.request.ClientCreateRequest;
import com.bkbank.ledger.dto.request.ClientUpdateRequest;
import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.repository.spec.LedgerListSpecifications;
import com.bkbank.ledger.service.ClientService;
import com.bkbank.ledger.util.PageableSortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Controller for Client management
 */
@RestController
@RequestMapping("/clients")
public class ClientController {

    private static final Map<String, String> CLIENT_SORT_MAPPINGS = Map.ofEntries(
            Map.entry("clientid", "clientId"),
            Map.entry("name", "fullName"),
            Map.entry("fullname", "fullName"),
            Map.entry("email", "email"),
            Map.entry("status", "status"),
            Map.entry("branchid", "homeBranch.branchId"),
            Map.entry("branchname", "homeBranch.branchName"),
            Map.entry("createdat", "createdAt"),
            Map.entry("updatedat", "updatedAt"));

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Create a new client
     * POST /clients
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createClient(@RequestBody ClientCreateRequest request) {
        log.info("POST /clients - Creating client: {}", request.getClientId());

        try {
            Client client = clientService.createClient(request);

            Map<String, Object> response = new HashMap<>();
            response.put("clientId", client.getClientId());
            response.put("fullName", client.getFullName());
            response.put("status", client.getStatus());
            response.put("message", "Client created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating client: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get client details
     * GET /clients/{clientId}
     */
    @GetMapping("/{clientId}")
    public ResponseEntity<Map<String, Object>> getClient(@PathVariable String clientId) {
        log.info("GET /clients/{}", clientId);

        try {
            Client client = clientService.getClient(clientId);

            Map<String, Object> response = buildClientResponse(client);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting client: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update client information
     * PUT /clients/{clientId}
     */
    @PutMapping("/{clientId}")
    public ResponseEntity<Map<String, Object>> updateClient(
            @PathVariable String clientId,
            @RequestBody ClientUpdateRequest request) {
        log.info("PUT /clients/{}", clientId);

        try {
            Client client = clientService.updateClient(clientId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("clientId", client.getClientId());
            response.put("message", "Client updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating client: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete client (soft delete)
     * DELETE /clients/{clientId}
     */
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Map<String, Object>> deleteClient(@PathVariable String clientId) {
        log.info("DELETE /clients/{}", clientId);

        try {
            clientService.deleteClient(clientId);

            Map<String, Object> response = new HashMap<>();
            response.put("clientId", clientId);
            response.put("message", "Client deactivated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting client: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all active clients
     * GET /clients
     */
    @GetMapping
    public ResponseEntity<?> getAllClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) String branchId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("GET /clients");

        try {
            Pageable pageable = PageableSortUtils.createPageable(page, size, sortBy, sortDir, "createdAt",
                    CLIENT_SORT_MAPPINGS);
            Page<Client> clients = clientService.findClients(
                    LedgerListSpecifications.clientList(q, status, branchId),
                    pageable);

            Page<Map<String, Object>> response = clients.map(this::buildClientSummary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting clients: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search clients by name
     * GET /clients/search?name={name}
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchClients(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /clients/search?name={}", name);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Client> clients = clientService.searchClientsByName(name, pageable);

            Page<Map<String, Object>> response = clients.map(this::buildClientSummary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching clients: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get client's all accounts
     * GET /clients/{clientId}/accounts
     */
    @GetMapping("/{clientId}/accounts")
    public ResponseEntity<Map<String, Object>> getClientAccounts(@PathVariable String clientId) {
        log.info("GET /clients/{}/accounts", clientId);

        try {
            Client client = clientService.getClient(clientId);
            List<SavingsAccount> savingsAccounts = clientService.getClientSavingsAccounts(clientId);
            List<LoanAccount> loanAccounts = clientService.getClientLoanAccounts(clientId);

            Map<String, Object> response = new HashMap<>();
            response.put("clientId", client.getClientId());
            response.put("clientName", client.getFullName());
            response.put("homeBranchId", client.getHomeBranch() != null ? client.getHomeBranch().getBranchId() : null);
            response.put("homeBranchName",
                    client.getHomeBranch() != null ? client.getHomeBranch().getBranchName() : null);
            response.put("savingsAccounts", buildSavingsAccountList(savingsAccounts));
            response.put("loanAccounts", buildLoanAccountList(loanAccounts));
            response.put("totalSavingsAccounts", savingsAccounts.size());
            response.put("totalLoanAccounts", loanAccounts.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting client accounts: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get client's savings accounts only
     * GET /clients/{clientId}/savings
     */
    @GetMapping("/{clientId}/savings")
    public ResponseEntity<Map<String, Object>> getClientSavingsAccounts(@PathVariable String clientId) {
        log.info("GET /clients/{}/savings", clientId);

        try {
            List<SavingsAccount> accounts = clientService.getClientSavingsAccounts(clientId);

            Map<String, Object> response = new HashMap<>();
            response.put("clientId", clientId);
            response.put("accounts", buildSavingsAccountList(accounts));
            response.put("total", accounts.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting savings accounts: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get client's loan accounts only
     * GET /clients/{clientId}/loans
     */
    @GetMapping("/{clientId}/loans")
    public ResponseEntity<Map<String, Object>> getClientLoanAccounts(@PathVariable String clientId) {
        log.info("GET /clients/{}/loans", clientId);

        try {
            List<LoanAccount> accounts = clientService.getClientLoanAccounts(clientId);

            Map<String, Object> response = new HashMap<>();
            response.put("clientId", clientId);
            response.put("accounts", buildLoanAccountList(accounts));
            response.put("total", accounts.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting loan accounts: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    private Map<String, Object> buildClientResponse(Client client) {
        Map<String, Object> response = new HashMap<>();

        // Core info
        response.put("clientId", client.getClientId());
        response.put("fullName", client.getFullName());
        response.put("dateOfBirth", client.getDateOfBirth());
        response.put("gender", client.getGender());
        response.put("email", client.getEmail());
        response.put("phoneNumber", client.getPhoneNumber());
        response.put("address", client.getAddress());
        response.put("city", client.getCity());
        response.put("country", client.getCountry());
        response.put("homeBranchId", client.getHomeBranch() != null ? client.getHomeBranch().getBranchId() : null);
        response.put("homeBranchName", client.getHomeBranch() != null ? client.getHomeBranch().getBranchName() : null);

        // ID info
        response.put("idNumber", client.getIdNumber());
        response.put("idType", client.getIdType());
        response.put("idIssueDate", client.getIdIssueDate());
        response.put("idExpiryDate", client.getIdExpiryDate());
        response.put("idExpired", client.isIdExpired());
        response.put("idExpiringSoon", client.isIdExpiringSoon());

        // Employment info
        response.put("occupation", client.getOccupation());
        response.put("employerName", client.getEmployerName());
        response.put("employerAddress", client.getEmployerAddress());
        response.put("employmentType", client.getEmploymentType());
        response.put("monthlyIncome", client.getMonthlyIncome());
        response.put("yearsAtCurrentJob", client.getYearsAtCurrentJob());
        response.put("suggestedCreditLimit", client.calculateSuggestedCreditLimit());

        // Status & audit
        response.put("status", client.getStatus());
        response.put("totalSavingsAccounts", client.getSavingsAccounts().size());
        response.put("totalLoanAccounts", client.getLoanAccounts().size());
        response.put("createdAt", client.getCreatedAt());
        response.put("updatedAt", client.getUpdatedAt());

        return response;
    }

    private Map<String, Object> buildClientSummary(Client client) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("clientId", client.getClientId());
        summary.put("fullName", client.getFullName());
        summary.put("email", client.getEmail());
        summary.put("phoneNumber", client.getPhoneNumber());
        summary.put("status", client.getStatus());
        summary.put("totalAccounts", client.getSavingsAccounts().size() + client.getLoanAccounts().size());
        summary.put("city", client.getCity());
        summary.put("homeBranchId", client.getHomeBranch() != null ? client.getHomeBranch().getBranchId() : null);
        summary.put("homeBranchName", client.getHomeBranch() != null ? client.getHomeBranch().getBranchName() : null);
        summary.put("createdAt", client.getCreatedAt());
        return summary;
    }

    private List<Map<String, Object>> buildSavingsAccountList(List<SavingsAccount> accounts) {
        return accounts.stream().map(acc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("accountNumber", acc.getAccountNumber());
            map.put("balance", acc.getBalance());
            map.put("status", acc.getStatus());
            map.put("branchId", acc.getBranchId());
            map.put("branchName", acc.getBranchName());
            return map;
        }).toList();
    }

    private List<Map<String, Object>> buildLoanAccountList(List<LoanAccount> accounts) {
        return accounts.stream().map(acc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("accountNumber", acc.getAccountNumber());
            map.put("principal", acc.getPrincipal());
            map.put("principalOutstanding", acc.getPrincipalOutstanding());
            map.put("status", acc.getStatus());
            map.put("branchId", acc.getBranchId());
            map.put("branchName", acc.getBranchName());
            return map;
        }).toList();
    }
}
