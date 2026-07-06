package com.inventory.agent.controller;

import com.inventory.agent.dto.DashboardStatsDTO;
import com.inventory.agent.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = productService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
