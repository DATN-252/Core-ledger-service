package com.bkbank.ledger.config;

import com.bkbank.ledger.entity.User;
import com.bkbank.ledger.entity.enums.UserRole;
import com.bkbank.ledger.repository.UserRepository;
import com.bkbank.ledger.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantService merchantService;

    @Override
    public void run(String... args) {
        createUserIfNotExists("admin", "admin123", "System Administrator", UserRole.ADMIN);
        createUserIfNotExists("teller01", "teller123", "Nguyen Van A - Teller", UserRole.TELLER);
        createUserIfNotExists("teller02", "teller123", "Tran Thi B - Teller", UserRole.TELLER);
        log.info("Default users initialized (if not already existing)");
        
        // Initialize merchants
        try {
            merchantService.createDemoMerchantsIfNotExist();
        } catch (Exception e) {
            log.warn("Failed to initialize demo merchants: {}", e.getMessage());
        }
    }

    private void createUserIfNotExists(String username, String rawPassword, String fullName, UserRole role) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .fullName(fullName)
                    .role(role)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Created default user: {} ({})", username, role);
        }
    }
}
