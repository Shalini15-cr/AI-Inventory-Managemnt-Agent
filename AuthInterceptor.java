package com.inventory.agent.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Pre-flight OPTIONS requests (useful for CORS if frontend runs separately)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Get session without creating a new one
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("loggedInUser") != null) {
            return true; // Authenticated
        }

        // Return 401 Unauthorized for REST API calls
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"status\":401, \"message\":\"Unauthorized: Session expired or invalid. Please login.\"}");
        return false;
    }
}
