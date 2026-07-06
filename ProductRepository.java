package com.inventory.agent.repository;

import com.inventory.agent.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findBySku(String sku);

    List<Product> findByStatus(String status);

    // Out of stock: quantity <= 0
    @Query("{ 'quantity': { '$lte': 0 } }")
    List<Product> findOutOfStock();

    // Low stock: quantity <= minimumStock AND quantity > 0
    @Query("{ '$expr': { '$and': [ { '$lte': [ '$quantity', '$minimumStock' ] }, { '$gt': [ '$quantity', 0 ] } ] } }")
    List<Product> findLowStock();

    // Search and filter with pagination
    Page<Product> findByProductNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String productName, String sku, String category, Pageable pageable);

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);
    
    Page<Product> findByStatusIgnoreCase(String status, Pageable pageable);
}
