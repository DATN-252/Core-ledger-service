package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.ClientCreateRequest;
import com.bkbank.ledger.dto.ClientUpdateRequest;
import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.service.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Client management
 */
@RestController
@RequestMapping("/clients")
public class ClientController {
    
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
    public ResponseEntity<Map<String, Object>> getAllClients() {
        log.info("GET /clients");
        
        try {
            List<Client> clients = clientService.getAllActiveClients();
            
            List<Map<String, Object>> clientList = clients.stream()
                    .map(this::buildClientSummary)
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("clients", clientList);
            response.put("total", clients.size());
            
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
    public ResponseEntity<Map<String, Object>> searchClients(@RequestParam String name) {
        log.info("GET /clients/search?name={}", name);
        
        try {
            List<Client> clients = clientService.searchClientsByName(name);
            
            List<Map<String, Object>> clientList = clients.stream()
                    .map(this::buildClientSummary)
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("clients", clientList);
            response.put("total", clients.size());
            response.put("searchTerm", name);
            
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
        Map<String, Object> summary = new HashMap<>();
        summary.put("clientId", client.getClientId());
        summary.put("fullName", client.getFullName());
        summary.put("email", client.getEmail());
        summary.put("phoneNumber", client.getPhoneNumber());
        summary.put("status", client.getStatus());
        summary.put("totalAccounts", client.getSavingsAccounts().size() + client.getLoanAccounts().size());
        return summary;
    }
    
    private List<Map<String, Object>> buildSavingsAccountList(List<SavingsAccount> accounts) {
        return accounts.stream().map(acc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("accountNumber", acc.getAccountNumber());
            map.put("balance", acc.getBalance());
            map.put("status", acc.getStatus());
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
            return map;
        }).toList();
    }
}
