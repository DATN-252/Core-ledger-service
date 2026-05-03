package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.request.ClientCreateRequest;
import com.bkbank.ledger.dto.request.ClientUpdateRequest;
import com.bkbank.ledger.entity.Branch;
import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.enums.ClientStatus;
import com.bkbank.ledger.repository.BranchRepository;
import com.bkbank.ledger.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Service for Client management
 */
@Service
public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    private final ClientRepository clientRepository;
    private final BranchRepository branchRepository;

    public ClientService(ClientRepository clientRepository, BranchRepository branchRepository) {
        this.clientRepository = clientRepository;
        this.branchRepository = branchRepository;
    }

    /**
     * Create a new client
     */
    @Transactional
    public Client createClient(ClientCreateRequest request) {
        log.info("Creating client: {}", request.getClientId());

        // Validation: Client ID must be unique
        if (clientRepository.existsByClientId(request.getClientId())) {
            throw new RuntimeException("Client ID already exists: " + request.getClientId());
        }

        // Validation: ID number must be unique
        if (clientRepository.existsByIdNumber(request.getIdNumber())) {
            throw new RuntimeException("ID number already exists: " + request.getIdNumber());
        }

        // Validation: Email must be unique
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        // Create client entity
        Client client = new Client();

        // Set required fields
        client.setClientId(request.getClientId());
        client.setFullName(request.getFullName());
        client.setDateOfBirth(request.getDateOfBirth());
        client.setGender(request.getGender());
        client.setEmail(request.getEmail());
        client.setPhoneNumber(request.getPhoneNumber());
        client.setAddress(request.getAddress());
        client.setIdNumber(request.getIdNumber());
        client.setIdType(request.getIdType());
        client.setHomeBranch(resolveBranch(request.getHomeBranchId()));
        // status is set to ACTIVE by default in constructor

        // Set optional fields (if provided)
        client.setCity(request.getCity());
        client.setCountry(request.getCountry());
        client.setIdIssueDate(request.getIdIssueDate());
        client.setIdExpiryDate(request.getIdExpiryDate());
        client.setOccupation(request.getOccupation());
        client.setEmployerName(request.getEmployerName());
        client.setEmployerAddress(request.getEmployerAddress());
        client.setEmploymentType(request.getEmploymentType());
        client.setMonthlyIncome(request.getMonthlyIncome());
        client.setYearsAtCurrentJob(request.getYearsAtCurrentJob());

        Client savedClient = clientRepository.save(client);
        log.info("Client created successfully: {}", savedClient.getClientId());

        return savedClient;
    }

    /**
     * Get client by clientId
     */
    public Client getClient(String clientId) {
        return clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));
    }

    /**
     * Get client by ID number
     */
    public Client getClientByIdNumber(String idNumber) {
        return clientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Client not found with ID number: " + idNumber));
    }

    /**
     * Update client information
     */
    @Transactional
    public Client updateClient(String clientId, ClientUpdateRequest request) {
        log.info("Updating client: {}", clientId);

        Client client = getClient(clientId);

        // Update fields if provided (not null)
        if (request.getFullName() != null) {
            client.setFullName(request.getFullName());
        }
        if (request.getDateOfBirth() != null) {
            client.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            client.setGender(request.getGender());
        }
        if (request.getEmail() != null) {
            // Check email uniqueness
            if (!client.getEmail().equals(request.getEmail()) &&
                    clientRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists: " + request.getEmail());
            }
            client.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            client.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            client.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            client.setCity(request.getCity());
        }
        if (request.getCountry() != null) {
            client.setCountry(request.getCountry());
        }
        if (request.getHomeBranchId() != null) {
            client.setHomeBranch(resolveBranch(request.getHomeBranchId()));
        }
        if (request.getIdIssueDate() != null) {
            client.setIdIssueDate(request.getIdIssueDate());
        }
        if (request.getIdExpiryDate() != null) {
            client.setIdExpiryDate(request.getIdExpiryDate());
        }
        if (request.getOccupation() != null) {
            client.setOccupation(request.getOccupation());
        }
        if (request.getEmployerName() != null) {
            client.setEmployerName(request.getEmployerName());
        }
        if (request.getEmployerAddress() != null) {
            client.setEmployerAddress(request.getEmployerAddress());
        }
        if (request.getEmploymentType() != null) {
            client.setEmploymentType(request.getEmploymentType());
        }
        if (request.getMonthlyIncome() != null) {
            client.setMonthlyIncome(request.getMonthlyIncome());
        }
        if (request.getYearsAtCurrentJob() != null) {
            client.setYearsAtCurrentJob(request.getYearsAtCurrentJob());
        }
        if (request.getStatus() != null) {
            client.setStatus(request.getStatus());
        }

        Client updatedClient = clientRepository.save(client);
        log.info("Client updated successfully: {}", clientId);

        return updatedClient;
    }

    /**
     * Soft delete client (set status to INACTIVE)
     */
    @Transactional
    public void deleteClient(String clientId) {
        log.info("Deleting (soft) client: {}", clientId);

        Client client = getClient(clientId);
        client.setStatus(ClientStatus.INACTIVE);
        clientRepository.save(client);

        log.info("Client status set to INACTIVE: {}", clientId);
    }

    /**
     * Get all active clients
     */
    public Page<Client> getAllActiveClients(Pageable pageable) {
        return clientRepository.findVisibleByStatus(ClientStatus.ACTIVE, pageable);
    }

    /**
     * Search clients by name
     */
    public Page<Client> searchClientsByName(String name, Pageable pageable) {
        return clientRepository.findVisibleByFullNameContainingIgnoreCase(name, pageable);
    }

    public Page<Client> findClients(Specification<Client> specification, Pageable pageable) {
        return clientRepository.findAll(specification, pageable);
    }

    /**
     * Get client's savings accounts
     */
    public List<SavingsAccount> getClientSavingsAccounts(String clientId) {
        Client client = getClient(clientId);
        return client.getSavingsAccounts();
    }

    /**
     * Get client's loan accounts
     */
    public List<LoanAccount> getClientLoanAccounts(String clientId) {
        Client client = getClient(clientId);
        return client.getLoanAccounts();
    }

    private Branch resolveBranch(String branchId) {
        if (branchId == null || branchId.isBlank()) {
            return null;
        }
        return branchRepository.findByBranchId(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + branchId));
    }
}
