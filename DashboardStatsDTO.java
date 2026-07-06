package com.inventory.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private long totalProducts;
    private long lowStockCount;
    private long outOfStockCount;
    private long healthyCount;
    private int categoriesCount;
    private double totalInventoryValue;
    private double inventoryHealthPercentage;
    private List<String> recentActivities;
    private Map<String, Long> categoryDistribution;
}
