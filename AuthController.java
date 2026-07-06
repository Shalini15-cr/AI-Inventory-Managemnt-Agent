package com.inventory.agent.controller;

import com.inventory.agent.dto.LoginRequest;
import com.inventory.agent.dto.LoginResponse;
import com.inventory.agent.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        boolean isAuthenticated = authService.authenticate(request.getUsername(), request.getPassword());

        if (isAuthenticated) {
            // Store session
            HttpSession session = servletRequest.getSession(true);
            session.setAttribute("loggedInUser", request.getUsername());

            return ResponseEntity.ok(new LoginResponse(true, "Login successful", request.getUsername()));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "Invalid username or password", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(new LoginResponse(true, "Logout successful", null));
    }

    @GetMapping("/session-check")
    public ResponseEntity<LoginResponse> checkSession(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null && session.getAttribute("loggedInUser") != null) {
            String username = (String) session.getAttribute("loggedInUser");
            return ResponseEntity.ok(new LoginResponse(true, "Session active", username));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(false, "Session inactive", null));
    }
}
