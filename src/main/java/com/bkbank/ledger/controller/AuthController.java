package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.response.ApiResponse;
import com.bkbank.ledger.dto.request.LoginRequest;
import com.bkbank.ledger.dto.response.LoginResponse;
import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.User;
import com.bkbank.ledger.entity.enums.UserRole;
import com.bkbank.ledger.repository.ClientRepository;
import com.bkbank.ledger.repository.UserRepository;
import com.bkbank.ledger.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Login endpoint — hỗ trợ cả admin (username) lẫn customer (phone/CCCD)
     * POST /auth/login
     * Body: { "username": "0912345678", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
        } catch (Exception e) {
            log.warn("Login failed for '{}': {}", request.getEffectiveUsername(), e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "Tên đăng nhập hoặc mật khẩu không đúng"));
        }
    }

    /**
     * Tạo tài khoản mobile cho khách hàng (chỉ admin dùng)
     * POST /auth/register-customer
     * Body: { "clientId": "CLI_001", "password": "abc12345" }
     */
    @PostMapping("/register-customer")
    public ResponseEntity<ApiResponse<Map<String, String>>> registerCustomer(@RequestBody Map<String, String> body) {
        try {
            String clientId = body.get("clientId");
            String password = body.get("password");

            if (clientId == null || password == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "clientId và password là bắt buộc"));
            }

            Client client = clientRepository.findByClientId(clientId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + clientId));

            if (userRepository.findByClientClientId(clientId).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Khách hàng đã có tài khoản đăng nhập"));
            }

            User user = User.builder()
                    .username(client.getPhoneNumber())  // username = số điện thoại
                    .password(passwordEncoder.encode(password))
                    .fullName(client.getFullName())
                    .role(UserRole.CUSTOMER)
                    .client(client)
                    .build();

            userRepository.save(user);
            log.info("Created customer account for clientId='{}', phone='{}'", clientId, client.getPhoneNumber());

            return ResponseEntity.ok(ApiResponse.success("Tạo tài khoản thành công", Map.of(
                    "username", client.getPhoneNumber(),
                    "clientId", clientId
            )));
        } catch (Exception e) {
            log.error("Failed to register customer: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    /**
     * Verify token validity
     * GET /auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> me(org.springframework.security.core.Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "username", auth.getName(),
                "role", auth.getAuthorities().iterator().next().getAuthority()
        )));
    }
}

