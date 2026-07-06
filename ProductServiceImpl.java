package com.inventory.agent.service;

import com.inventory.agent.dto.DashboardStatsDTO;
import com.inventory.agent.dto.ProductDTO;
import com.inventory.agent.exception.ResourceNotFoundException;
import com.inventory.agent.model.Product;
import com.inventory.agent.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Page<Product> getProductsPaginated(String search, String category, String status, int page, int size, String sortBy, String direction) {
        Query query = new Query();

        // 1. Search filter: search in productName, sku, category, supplier
        if (search != null && !search.trim().isEmpty()) {
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("productName").regex(search, "i"),
                    Criteria.where("sku").regex(search, "i"),
                    Criteria.where("category").regex(search, "i"),
                    Criteria.where("supplier").regex(search, "i")
            );
            query.addCriteria(searchCriteria);
        }

        // 2. Category filter
        if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) {
            query.addCriteria(Criteria.where("category").is(category));
        }

        // 3. Status filter
        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status)) {
            query.addCriteria(Criteria.where("status").is(status.toUpperCase()));
        }

        // Count total matches
        long total = mongoTemplate.count(query, Product.class);

        // Sorting
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        query.with(pageable);

        // Fetch paginated results
        List<Product> products = mongoTemplate.find(query, Product.class);

        return new PageImpl<>(products, pageable, total);
    }

    @Override
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    @Override
    public Product createProduct(ProductDTO dto) {
        // Check SKU uniqueness
        if (productRepository.findBySku(dto.getSku()).isPresent()) {
            throw new IllegalArgumentException("Product SKU already exists: " + dto.getSku());
        }

        Product product = Product.builder()
                .productName(dto.getProductName())
                .sku(dto.getSku())
                .category(dto.getCategory())
                .quantity(dto.getQuantity())
                .minimumStock(dto.getMinimumStock())
                .supplier(dto.getSupplier())
                .supplierEmail(dto.getSupplierEmail())
                .price(dto.getPrice())
                .warehouseLocation(dto.getWarehouseLocation())
                .description(dto.getDescription())
                .lastUpdated(LocalDateTime.now())
                .build();

        product.calculateStatus();
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(String id, ProductDTO dto) {
        Product existingProduct = getProductById(id);

        // SKU check (cannot duplicate other product's SKU)
        Optional<Product> productWithSameSku = productRepository.findBySku(dto.getSku());
        if (productWithSameSku.isPresent() && !productWithSameSku.get().getId().equals(id)) {
            throw new IllegalArgumentException("Product SKU already exists: " + dto.getSku());
        }

        existingProduct.setProductName(dto.getProductName());
        existingProduct.setSku(dto.getSku());
        existingProduct.setCategory(dto.getCategory());
        existingProduct.setQuantity(dto.getQuantity());
        existingProduct.setMinimumStock(dto.getMinimumStock());
        existingProduct.setSupplier(dto.getSupplier());
        existingProduct.setSupplierEmail(dto.getSupplierEmail());
        existingProduct.setPrice(dto.getPrice());
        existingProduct.setWarehouseLocation(dto.getWarehouseLocation());
        existingProduct.setDescription(dto.getDescription());
        existingProduct.setLastUpdated(LocalDateTime.now());
        
        existingProduct.calculateStatus();
        return productRepository.save(existingProduct);
    }

    @Override
    public void deleteProduct(String id) {
        Product existingProduct = getProductById(id);
        productRepository.delete(existingProduct);
    }

    @Override
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStock();
    }

    @Override
    public List<Product> getOutOfStockProducts() {
        return productRepository.findOutOfStock();
    }

    @Override
    public DashboardStatsDTO getDashboardStats() {
        List<Product> allProducts = productRepository.findAll();
        
        long totalProducts = allProducts.size();
        long outOfStock = allProducts.stream().filter(p -> p.getQuantity() <= 0).count();
        long lowStock = allProducts.stream().filter(p -> p.getQuantity() > 0 && p.getQuantity() <= p.getMinimumStock()).count();
        long healthy = allProducts.stream().filter(p -> p.getQuantity() > p.getMinimumStock()).count();

        // Calculate total inventory valuation
        double totalValue = allProducts.stream()
                .mapToDouble(p -> p.getPrice() * p.getQuantity())
                .sum();

        // Calculate health percentage
        double healthPercentage = totalProducts > 0 
                ? ((double) healthy / totalProducts) * 100 
                : 100.0;

        // Categories list
        Set<String> categories = allProducts.stream()
                .map(Product::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Category distribution maps
        Map<String, Long> categoryMap = allProducts.stream()
                .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));

        // Recent Activities based on last updated products
        List<String> activities = allProducts.stream()
                .sorted(Comparator.comparing(Product::getLastUpdated, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(p -> String.format("Stock level for '%s' (%s) is %d in %s (Updated: %s)",
                        p.getProductName(), p.getSku(), p.getQuantity(), 
                        p.getWarehouseLocation() != null ? p.getWarehouseLocation() : "General WH",
                        p.getLastUpdated().toLocalDate().toString()))
                .collect(Collectors.toList());

        return DashboardStatsDTO.builder()
                .totalProducts(totalProducts)
                .outOfStockCount(outOfStock)
                .lowStockCount(lowStock)
                .healthyCount(healthy)
                .categoriesCount(categories.size())
                .totalInventoryValue(Math.round(totalValue * 100.0) / 100.0)
                .inventoryHealthPercentage(Math.round(healthPercentage * 100.0) / 100.0)
                .recentActivities(activities)
                .categoryDistribution(categoryMap)
                .build();
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void seedSampleData() {
        if (productRepository.count() == 0) {
            List<Product> samples = List.of(
                    Product.builder()
                            .productName("Heavy Duty Steel Gaskets")
                            .sku("SG-STEEL-001")
                            .category("Hardware")
                            .quantity(150)
                            .minimumStock(50)
                            .supplier("Apex Metal Works")
                            .supplierEmail("sales@apexmetals.com")
                            .price(12.50)
                            .warehouseLocation("Aisles 4-A")
                            .description("Industrial grade steel gaskets for piping flanges.")
                            .lastUpdated(LocalDateTime.now().minusDays(5))
                            .status("HEALTHY").build(),
                    Product.builder()
                            .productName("Pneumatic Actuator Valve")
                            .sku("VALVE-PNEU-90")
                            .category("Pneumatics")
                            .quantity(8)
                            .minimumStock(15)
                            .supplier("Flow Control Inc.")
                            .supplierEmail("orders@flowcontrol.com")
                            .price(189.99)
                            .warehouseLocation("Cabinet B-2")
                            .description("Double acting rotational pneumatic control valve.")
                            .lastUpdated(LocalDateTime.now().minusDays(2))
                            .status("LOW_STOCK").build(),
                    Product.builder()
                            .productName("Microcontroller Control Board")
                            .sku("PCB-MAIN-V2")
                            .category("Electronics")
                            .quantity(0)
                            .minimumStock(10)
                            .supplier("ElectroSem Circuits")
                            .supplierEmail("support@electrosem.com")
                            .price(45.00)
                            .warehouseLocation("ESD-Safe Room bin 1")
                            .description("Main logical motherboard for unit controller assemblies.")
                            .lastUpdated(LocalDateTime.now().minusDays(1))
                            .status("OUT_OF_STOCK").build(),
                    Product.builder()
                            .productName("Copper Wire Reels (100m)")
                            .sku("WIRE-COP-100")
                            .category("Electrical")
                            .quantity(60)
                            .minimumStock(20)
                            .supplier("VoltSource Cables")
                            .supplierEmail("volt@voltsource.com")
                            .price(75.25)
                            .warehouseLocation("Aisle 9-C")
                            .description("Solid core insulated 14 AWG copper wiring reels.")
                            .lastUpdated(LocalDateTime.now().minusDays(8))
                            .status("HEALTHY").build(),
                    Product.builder()
                            .productName("Hydraulic Fluid ISO 46 (5gal)")
                            .sku("HYD-ISO46-5G")
                            .category("Chemicals")
                            .quantity(4)
                            .minimumStock(12)
                            .supplier("PetroLube Distributors")
                            .supplierEmail("lubricants@petrolube.com")
                            .price(85.00)
                            .warehouseLocation("Hazmat Depot Shell 1")
                            .description("Anti-wear hydraulic oil for heavy manufacturing press equipment.")
                            .lastUpdated(LocalDateTime.now().minusDays(3))
                            .status("LOW_STOCK").build(),
                    Product.builder()
                            .productName("Industrial Ball Bearings (10pk)")
                            .sku("BEAR-BALL-20")
                            .category("Hardware")
                            .quantity(200)
                            .minimumStock(30)
                            .supplier("Apex Metal Works")
                            .supplierEmail("sales@apexmetals.com")
                            .price(35.00)
                            .warehouseLocation("Cabinet A-4")
                            .description("Deep groove radial miniature bearings for conveyor motors.")
                            .lastUpdated(LocalDateTime.now().minusDays(12))
                            .status("HEALTHY").build(),
                    Product.builder()
                            .productName("Digital Temperature Sensor")
                            .sku("SENS-TEMP-X")
                            .category("Electronics")
                            .quantity(12)
                            .minimumStock(15)
                            .supplier("ElectroSem Circuits")
                            .supplierEmail("support@electrosem.com")
                            .price(18.75)
                            .warehouseLocation("ESD-Safe Room bin 4")
                            .description("Thermocouple thermometer module with digital readout compatibility.")
                            .lastUpdated(LocalDateTime.now().minusHours(4))
                            .status("LOW_STOCK").build(),
                    Product.builder()
                            .productName("Compressed Air Filter Regulator")
                            .sku("REG-AIR-05")
                            .category("Pneumatics")
                            .quantity(0)
                            .minimumStock(5)
                            .supplier("Flow Control Inc.")
                            .supplierEmail("orders@flowcontrol.com")
                            .price(59.90)
                            .warehouseLocation("Cabinet B-1")
                            .description("Modular moisture trap air filter with visual pressure dial gauge.")
                            .lastUpdated(LocalDateTime.now().minusHours(1))
                            .status("OUT_OF_STOCK").build()
            );

            productRepository.saveAll(samples);
            System.out.println("--- Seeding complete. Loaded " + samples.size() + " sample products. ---");
        }
    }
}
