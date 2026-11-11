package com.itri623.userservice.controller;

import com.itri623.userservice.model.User;
import com.itri623.userservice.service.AuditService;
import com.itri623.userservice.service.UserService;
import com.itri623.userservice.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger =
            LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String getClientIp(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /*
     * USER REGISTRATION
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> registration,
            HttpServletRequest request) {

        String name = registration.get("name");
        String email = registration.get("email");
        String password = registration.get("password");

        String ip = getClientIp(request);

        /*
         * Validate required fields
         */
        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {

            auditService.logEvent(
                    "REGISTER_FAILED",
                    email,
                    null,
                    "FAILURE",
                    ip,
                    "Missing required registration fields"
            );

            logger.warn(
                    "Registration failed: missing required fields for email: {}",
                    email
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            Map.of(
                                    "error",
                                    "Name, email and password are required"
                            )
                    );
        }

        /*
         * Check if email already exists
         */
        if (userService.getUserByEmail(email).isPresent()) {

            auditService.logEvent(
                    "REGISTER_FAILED",
                    email,
                    null,
                    "FAILURE",
                    ip,
                    "Email already registered"
            );

            logger.warn(
                    "Registration failed: email already exists: {}",
                    email
            );

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                            Map.of(
                                    "error",
                                    "Email already registered"
                            )
                    );
        }

        /*
         * Create new user
         *
         * UserService is responsible for BCrypt password hashing.
         */
        User user = new User(
                name,
                email,
                password,
                "USER"
        );

        User savedUser = userService.createUser(user);

        /*
         * Audit successful registration
         */
        auditService.logEvent(
                "REGISTER_SUCCESS",
                savedUser.getEmail(),
                savedUser.getId(),
                "SUCCESS",
                ip,
                "User registration successful"
        );

        logger.info(
                "User registration successful: userId={}, email={}, role={}",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        /*
         * Return response without password
         */
        Map<String, Object> response =
                new HashMap<>();

        response.put("message", "User registered successfully");
        response.put("userId", savedUser.getId());
        response.put("name", savedUser.getName());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * USER LOGIN
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credentials,
            HttpServletRequest request) {

        String email = credentials.get("email");
        String password = credentials.get("password");

        String ip = getClientIp(request);

        /*
         * Missing credentials
         */
        if (email == null || password == null) {

            auditService.logEvent(
                    "LOGIN_FAILED",
                    email,
                    null,
                    "FAILURE",
                    ip,
                    "Missing credentials"
            );

            logger.warn(
                    "Login attempt with missing credentials from email: {}",
                    email
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            Map.of(
                                    "error",
                                    "Email and password required"
                            )
                    );
        }

        /*
         * Find user
         */
        Optional<User> userOpt =
                userService.getUserByEmail(email);

        /*
         * User does not exist
         */
        if (userOpt.isEmpty()) {

            auditService.logEvent(
                    "LOGIN_FAILED",
                    email,
                    null,
                    "FAILURE",
                    ip,
                    "User not found"
            );

            logger.warn(
                    "Login failed: user not found with email: {}",
                    email
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "error",
                                    "Invalid credentials"
                            )
                    );
        }

        User user = userOpt.get();

        /*
         * Invalid password
         */
        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            auditService.logEvent(
                    "LOGIN_FAILED",
                    email,
                    user.getId(),
                    "FAILURE",
                    ip,
                    "Invalid password"
            );

            logger.warn(
                    "Login failed: invalid password for email: {}",
                    email
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "error",
                                    "Invalid credentials"
                            )
                    );
        }

        /*
         * Generate JWT
         */
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        /*
         * Successful login audit event
         */
        auditService.logEvent(
                "LOGIN_SUCCESS",
                email,
                user.getId(),
                "SUCCESS",
                ip,
                "Login successful"
        );

        logger.info(
                "Login successful: userId={}, email={}, role={}",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        Map<String, Object> response =
                new HashMap<>();

        response.put("token", token);
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }
}