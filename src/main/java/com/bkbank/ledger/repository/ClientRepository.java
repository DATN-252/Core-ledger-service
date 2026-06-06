package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.enums.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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

    @Query("select count(c) from Client c where c.clientId not like 'MERCHANT_%'")
    long countVisibleClients();

    @Query("select count(c) from Client c where c.status = :status and c.clientId not like 'MERCHANT_%'")
    long countVisibleClientsByStatus(ClientStatus status);

    @Query("select c from Client c where c.status = :status and c.clientId not like 'MERCHANT_%'")
    Page<Client> findVisibleByStatus(ClientStatus status, Pageable pageable);
    
    /**
     * Search clients by name (case-insensitive, partial match)
     */
    @Query("""
            select c from Client c
            where lower(c.fullName) like lower(concat('%', :name, '%'))
              and c.clientId not like 'MERCHANT_%'
            """)
    Page<Client> findVisibleByFullNameContainingIgnoreCase(String name, Pageable pageable);
}
