package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.request.LoginRequest;
import com.bkbank.ledger.dto.response.LoginResponse;
import com.bkbank.ledger.entity.User;
import com.bkbank.ledger.repository.UserRepository;
import com.bkbank.ledger.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    public LoginResponse login(LoginRequest request) {
        String nameAcc = request.getEffectiveUsername();

        // Tìm User theo: username → phoneNumber → idNumber (CCCD)
        User user = userRepository.findByUsername(nameAcc)
                .or(() -> userRepository.findByClientPhoneNumber(nameAcc))
                .or(() -> userRepository.findByClientIdNumber(nameAcc))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + nameAcc));

        // Xác thực password (dùng username thực của User)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
        );

        String token = jwtUtil.generateToken(user);

        // Lấy clientId nếu là customer
        String clientId = (user.getClient() != null) ? user.getClient().getClientId() : null;

        log.info("User '{}' logged in with role '{}' (clientId={})",
                user.getUsername(), user.getRole(), clientId);

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                expiration,
                clientId
        );
    }
}

