package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.enums.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Client entity
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {
    
    /**
     * Find client by clientId (unique identifier)
     */
    Optional<Client> findByClientId(String clientId);
    
    /**
     * Find client by ID number (CMND/CCCD/Passport)
     */
    Optional<Client> findByIdNumber(String idNumber);
    
    /**
     * Find client by email
     */
    Optional<Client> findByEmail(String email);
    
    /**
     * Check if client ID already exists
     */
    boolean existsByClientId(String clientId);
    
    /**
     * Check if ID number already exists
     */
    boolean existsByIdNumber(String idNumber);
    
    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find all clients by status
     */
    Page<Client> findByStatus(ClientStatus status, Pageable pageable);

    long countByStatus(ClientStatus status);
    
    /**
     * Search clients by name (case-insensitive, partial match)
     */
    Page<Client> findByFullNameContainingIgnoreCase(String name, Pageable pageable);
}
