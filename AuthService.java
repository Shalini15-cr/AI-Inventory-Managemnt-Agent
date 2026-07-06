package com.inventory.agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    /**
     * Authenticates the admin user.
     * In a production environment, this would verify credentials against a database or external identity provider.
     * For this system, this now uses configurable credentials from application properties.
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        return adminUsername.equalsIgnoreCase(username.trim()) && adminPassword.equals(password);
    }
}
