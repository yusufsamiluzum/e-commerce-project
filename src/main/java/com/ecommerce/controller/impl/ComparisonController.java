package com.ecommerce.controller.impl;

import com.ecommerce.dto.DtoComparison;
import com.ecommerce.services.impl.ComparisonService; // Assuming this path
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Import other necessary exceptions or handlers if needed
// import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
// import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map; // For the save request body

@RestController
@RequestMapping("/api/v1/comparisons") // Base path for comparison endpoints
@RequiredArgsConstructor
public class ComparisonController {

    private final ComparisonService comparisonService; // Inject the service

    /**
     * Gets a list of superficial comparisons for a customer or session.
     * Requires either 'customerId' or 'sessionId'.
     * GET /api/v1/comparisons?customerId=123 OR /api/v1/comparisons?sessionId=xyz
     */
    @GetMapping
    public ResponseEntity<List<DtoComparison>> getComparisons(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String sessionId) {
        // Basic validation: Ensure at least one identifier is present
        if (customerId == null && (sessionId == null || sessionId.trim().isEmpty())) {
            // Consider a custom exception or return BadRequest
            return ResponseEntity.badRequest().build();
        }
        List<DtoComparison> comparisons = comparisonService.getSuperficialComparisons(customerId, sessionId); //
        return ResponseEntity.ok(comparisons);
    }

    /**
     * Gets the detailed view of a specific comparison.
     * GET /api/v1/comparisons/{comparisonId}
     */
    @GetMapping("/{comparisonId}")
    public ResponseEntity<DtoComparison> getDetailedComparison(
            @PathVariable Long comparisonId) {
        // Consider adding try-catch for NotFoundException if not handled globally
        DtoComparison comparison = comparisonService.getDetailedComparison(comparisonId); //
        return ResponseEntity.ok(comparison);
    }

    /**
     * Adds the first product to a comparison (creates if necessary).
     * Requires 'productId' and either 'customerId' or 'sessionId'.
     * POST /api/v1/comparisons
     * Body: { "productId": 1 }
     * Headers/Params: Provide customerId or sessionId
     */
    @PostMapping
    public ResponseEntity<DtoComparison> addFirstProduct(
            @RequestBody ProductIdRequest request, // Simple DTO for productId
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String sessionId) {
        // Basic validation
        if (customerId == null && (sessionId == null || sessionId.trim().isEmpty())) {
             return ResponseEntity.badRequest().body(null); // Or throw specific exception
        }
         if (request == null || request.getProductId() == null) {
             return ResponseEntity.badRequest().body(null);
         }

        DtoComparison comparison = comparisonService.addFirstProductToComparison(request.getProductId(), customerId, sessionId); //
        return ResponseEntity.status(HttpStatus.CREATED).body(comparison);
    }

     /**
     * Adds a product to an existing comparison.
     * POST /api/v1/comparisons/{comparisonId}/products
     * Body: { "productId": 2 }
     */
    @PostMapping("/{comparisonId}/products")
    public ResponseEntity<DtoComparison> addProductToExisting(
            @PathVariable Long comparisonId,
            @RequestBody ProductIdRequest request) {
         if (request == null || request.getProductId() == null) {
             return ResponseEntity.badRequest().body(null);
         }
        DtoComparison comparison = comparisonService.addProductToExistingComparison(comparisonId, request.getProductId()); //
        return ResponseEntity.ok(comparison);
    }


    /**
     * Removes a product from a specific comparison.
     * DELETE /api/v1/comparisons/{comparisonId}/products/{productId}
     */
    @DeleteMapping("/{comparisonId}/products/{productId}")
    public ResponseEntity<DtoComparison> removeProduct(
            @PathVariable Long comparisonId,
            @PathVariable Long productId) {
        DtoComparison updatedComparison = comparisonService.removeProductFromComparison(comparisonId, productId); //
        // Decide: return updated comparison (OK) or just confirmation (NoContent)?
        // Returning updated state is often useful for the client.
        return ResponseEntity.ok(updatedComparison);
        // Alternatively:
        // comparisonService.removeProductFromComparison(comparisonId, productId);
        // return ResponseEntity.noContent().build();
    }

     /**
     * Saves/names a comparison for a logged-in user.
     * PUT /api/v1/comparisons/{comparisonId}/save
     * Requires 'customerId' (e.g., from authentication context or request param)
     * Body: { "name": "My Tech Comparison" }
     */
    @PutMapping("/{comparisonId}/save")
    public ResponseEntity<DtoComparison> saveComparison(
            @PathVariable Long comparisonId,
            @RequestParam Long customerId, // Assuming customerId is required and passed
            @RequestBody @Valid SaveComparisonRequest request) {
         // Here, customerId might ideally come from Spring Security's Principal
         // e.g., @AuthenticationPrincipal UserDetails userDetails -> get customer ID
         DtoComparison savedComparison = comparisonService.saveComparison(comparisonId, customerId, request.getName()); //
         return ResponseEntity.ok(savedComparison);
    }


    // --- Helper DTOs for Request Bodies ---

    // Could be placed in a separate 'dto' or 'request' package
    static class ProductIdRequest {
        private Long productId;
        // Getter and Setter
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
    }

    static class SaveComparisonRequest {
        @NotBlank(message = "Comparison name cannot be blank")
        private String name;
        // Getter and Setter
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

     // --- Optional: Exception Handling ---
     /*
     @ExceptionHandler(NotFoundException.class) // Example for handling specific exceptions
     public ResponseEntity<String> handleNotFound(NotFoundException ex) {
         // Log the exception maybe
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
     }

     @ExceptionHandler(ValidationException.class) // Example for validation errors
     public ResponseEntity<String> handleValidation(ValidationException ex) {
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
     }

     // Catch-all for other runtime exceptions from the service
     @ExceptionHandler(RuntimeException.class)
     public ResponseEntity<String> handleGenericRuntimeException(RuntimeException ex) {
        // Log the exception
        Throwable cause = ex.getCause();
        if (cause instanceof NotFoundException) { // Handle nested NotFoundException
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resource not found."); // More generic message
        }
        // Log ex
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred.");
     }
     */

}