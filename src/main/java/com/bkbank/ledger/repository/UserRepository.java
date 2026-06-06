package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    // For mobile login: find by linked Client's phoneNumber or idNumber
    Optional<User> findByClientPhoneNumber(String phoneNumber);
    Optional<User> findByClientIdNumber(String idNumber);
    Optional<User> findByClientClientId(String clientId);
}
