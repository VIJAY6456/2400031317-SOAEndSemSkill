package com.klu.spring.mvc.auth;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.spring.mvc.common.JwtUtil;
import com.klu.spring.mvc.common.TokenValidationResult;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // Demo users: username -> {password, role}
    private static final Map<String, String[]> USERS = new HashMap<>();
    static {
        USERS.put("admin", new String[] {"admin123", "ADMIN"});
        USERS.put("user", new String[] {"user123", "USER"});
    }

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** Public: issue a token. Demo users: admin/admin123 (ADMIN), user/user123 (USER). */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String[] record = USERS.get(request.getUsername());

        if (record == null || !record[0].equals(request.getPassword())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 401);
            error.put("error", "Unauthorized");
            error.put("message", "Invalid username or password");
            return ResponseEntity.status(401).body(error);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", jwtUtil.generateToken(request.getUsername(), record[1]));
        body.put("tokenType", "Bearer");
        body.put("expiresInSeconds", jwtUtil.getExpirationSeconds());
        body.put("role", record[1]);
        return ResponseEntity.ok(body);
    }

    /** Public: check any token and see why it is rejected. */
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResult> validate(@Valid @RequestBody TokenRequest request) {
        TokenValidationResult result = jwtUtil.validateToken(request.getToken());
        return result.isValid() ? ResponseEntity.ok(result) : ResponseEntity.status(401).body(result);
    }
}
