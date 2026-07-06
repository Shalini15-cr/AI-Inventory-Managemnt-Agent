package com.inventory.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id;
    
    private String productName;
    private String sku;
    private String category;
    private Integer quantity;
    private Integer minimumStock;
    private String supplier;
    private String supplierEmail;
    private Double price;
    private String warehouseLocation;
    private String description;
    private LocalDateTime lastUpdated;
    private String status; // "HEALTHY", "LOW_STOCK", "OUT_OF_STOCK"

    /**
     * Dynamically updates the product status based on quantity and minimumStock.
     */
    public void calculateStatus() {
        if (this.quantity == null || this.quantity <= 0) {
            this.status = "OUT_OF_STOCK";
        } else if (this.minimumStock != null && this.quantity <= this.minimumStock) {
            this.status = "LOW_STOCK";
        } else {
            this.status = "HEALTHY";
        }
    }
}
