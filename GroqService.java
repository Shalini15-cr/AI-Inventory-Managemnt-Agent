package com.inventory.agent.service;

import com.inventory.agent.model.Product;
import com.inventory.agent.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroqService {

    private final ProductRepository productRepository;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String modelName;

    /**
     * Processes the user inventory question using the Groq API and current database state.
     */
    public String answerInventoryQuestion(String userQuestion) {
        // 1. Fetch current inventory from MongoDB
        List<Product> products = productRepository.findAll();
        
        // 2. Format the inventory into a markdown table representation for the LLM
        String inventoryMarkdown = formatInventoryToMarkdown(products);

        // 3. Build system prompt
        String systemPrompt = """
                You are an expert Manufacturing Inventory Analyst.
                You analyze inventory professionally.

                Rules:
                1. Detect products below minimum stock levels.
                2. Detect out-of-stock items (quantity is 0 or less).
                3. Recommend specific reorder quantities (e.g., reordering up to a safe margin or economic order quantity).
                4. Explain the direct business impact of stock issues (production downtime, delayed deliveries, lost sales).
                5. Suggest actionable supply chain improvements (e.g., multi-sourcing, updating safety stocks).
                6. NEVER hallucinate details. If the requested information cannot be found in the inventory database provided below, state clearly: "I cannot find this information in the active database."
                7. Use ONLY the provided inventory data to answer the user's questions.
                8. Return your final analysis in structured, clean markdown (using headers, tables, bold text, and lists for key data) that is easy for a manager to read.
                """;

        // 4. Build user content containing inventory data and original question
        String userContent = String.format(
                "### Inventory Data\n%s\n\n### User Question\n%s", 
                inventoryMarkdown, 
                userQuestion
        );

        // 5. Verify API key before making the call
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "### ⚠️ Configuration Required\n\n" +
                   "The Groq API key is not configured. Please add your Groq API key to " +
                   "`backend/src/main/resources/application.properties`:\n\n" +
                   "```properties\n" +
                   "groq.api.key=gsk_...\n" +
                   "```\n" +
                   "Once the key is added and the server is restarted, the AI Analyst will be fully functional.";
        }

        try {
            // Build modern RestClient
            RestClient restClient = RestClient.builder().build();

            // Prepare Request Payload
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("model", modelName);
            requestPayload.put("temperature", 0.2); // Low temperature for high analytical accuracy

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userContent);

            requestPayload.put("messages", List.of(systemMessage, userMessage));

            // Execute POST to Groq endpoint
            Map<String, Object> response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                    if (message != null && message.containsKey("content")) {
                        return message.get("content");
                    }
                }
            }

            return "Error: Empty or invalid response returned from Groq API.";
        } catch (Exception e) {
            e.printStackTrace();
            return "### ⚠️ AI Agent Error\n\n" +
                   "Failed to communicate with the Groq API. Please verify:\n" +
                   "1. Your API key in `application.properties` is correct.\n" +
                   "2. You have internet connectivity.\n" +
                   "3. The API server `https://api.groq.com` is accessible.\n\n" +
                   "**Details:** `" + e.getMessage() + "`";
        }
    }

    /**
     * Helper to serialize products list into a markdown table.
     */
    private String formatInventoryToMarkdown(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "*The inventory is currently empty.*";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("| SKU | Product Name | Category | Quantity | Min Stock | Price | Warehouse Location | Status | Supplier | Supplier Email |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n");

        for (Product p : products) {
            sb.append(String.format("| %s | %s | %s | %d | %d | $%.2f | %s | %s | %s | %s |\n",
                    p.getSku(),
                    p.getProductName(),
                    p.getCategory(),
                    p.getQuantity(),
                    p.getMinimumStock(),
                    p.getPrice(),
                    p.getWarehouseLocation() != null ? p.getWarehouseLocation() : "N/A",
                    p.getStatus(),
                    p.getSupplier(),
                    p.getSupplierEmail()
            ));
        }

        return sb.toString();
    }
}
