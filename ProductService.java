package com.inventory.agent.service;

import com.inventory.agent.dto.DashboardStatsDTO;
import com.inventory.agent.dto.ProductDTO;
import com.inventory.agent.model.Product;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    
    List<Product> getAllProducts();
    
    Page<Product> getProductsPaginated(String search, String category, String status, int page, int size, String sortBy, String direction);
    
    Product getProductById(String id);
    
    Product createProduct(ProductDTO dto);
    
    Product updateProduct(String id, ProductDTO dto);
    
    void deleteProduct(String id);
    
    List<Product> getLowStockProducts();
    
    List<Product> getOutOfStockProducts();
    
    DashboardStatsDTO getDashboardStats();
    
    void seedSampleData();
}
