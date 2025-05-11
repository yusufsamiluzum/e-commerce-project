package com.ecommerce.controller.impl;

import com.ecommerce.dto.DtoOrderResponse;
import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductImage;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.product.ProductImage;
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.services.IFileStorageService;
import com.ecommerce.services.OrderService;
// Import the new service interface
import com.ecommerce.services.SellerProductService; // CHANGED
import com.ecommerce.config.securityconfig.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger; // Loglama için
import org.slf4j.LoggerFactory; 

/**
 * REST Controller for Seller-specific product management operations.
 * Requires SELLER role for access. Operations are scoped to the authenticated seller's products.
 */
@RestController
@RequestMapping("/api/v1/seller/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerProductController {

    // Inject the new SellerProductService instead of the base ProductService
    private final SellerProductService sellerProductService; // CHANGED
    private final OrderService orderService;
    private final IFileStorageService fileStorageService; // <-- YENİ
    private final ProductImageRepository productImageRepository; // <-- YENİ
    private final ProductRepository productRepository; 
    
    
    private static final Logger log = LoggerFactory.getLogger(SellerProductController.class); // <-- YENİ
    

    /**
     * POST /api/v1/seller/products : Create a new product for the authenticated seller.
     * Calls SellerProductService to handle creation.
     */
    
    
    /**
     * POST /api/v1/seller/products/{productId}/images : Upload an image for a specific product owned by the authenticated seller.
     * Requires SELLER role.
     *
     * @param productId The ID of the product to associate the image with.
     * @param file      The image file uploaded via multipart form data.
     * @param isPrimary Optional request parameter to mark the image as primary.
     * @param authentication The authentication object for the current seller.
     * @return ResponseEntity containing the DtoProductImage of the saved image or an error.
     */
    @PostMapping("/{productId}/images")
    public ResponseEntity<?> uploadMyProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
            Authentication authentication) {

        Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select a file to upload."));
        }

        // Ürünü bul ve satıcıya ait olduğunu kontrol et
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (product.getSeller() == null || !product.getSeller().getUserId().equals(sellerId)) {
            log.warn("Seller {} attempted to upload image to product {} not owned by them.", sellerId, productId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You do not have permission to upload images for this product."));
        }

        // TODO: Add validation for file type (e.g., allow only jpg, png) and size

        try {
            String uniqueFilename = fileStorageService.saveFile(file);

            ProductImage productImage = new ProductImage();
            productImage.setProduct(product);
            productImage.setImageUrl("/product-images/" + uniqueFilename); // Erişilebilir URL
            productImage.setPrimary(isPrimary);

            if (isPrimary) {
                productImageRepository.findByProductProductId(productId).forEach(img -> {
                    if (img.isPrimary()) {
                        img.setPrimary(false);
                        productImageRepository.save(img);
                    }
                });
            }

            ProductImage savedImage = productImageRepository.save(productImage);

            DtoProductImage responseDto = new DtoProductImage(
                    savedImage.getImageId(),
                    savedImage.getImageUrl(),
                    savedImage.isPrimary(),
                    savedImage.getAltText()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

        } catch (IOException e) {
            log.error("Failed to upload image for product ID: {} by seller ID: {}", productId, sellerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not upload image. Error: " + e.getMessage()));
        } catch (ResourceNotFoundException e) {
           return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
           log.error("Unexpected error during image upload for product ID: {} by seller ID: {}", productId, sellerId, e);
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(Map.of("error", "An unexpected error occurred during image upload."));
        }
    }
    
    
    @PostMapping // CREATE
    public ResponseEntity<DtoProduct> createMyProduct(
            Authentication authentication,
            @Valid @RequestBody DtoProduct dtoProduct) {
        try {
            Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);
            dtoProduct.setProductId(null);

            // Call the method on the injected sellerProductService
            DtoProduct createdProduct = sellerProductService.createMyProduct(dtoProduct, sellerId); // CHANGED

            URI location = ServletUriComponentsBuilder
                    .fromCurrentContextPath().path("/api/v1/products/{id}")
                    .buildAndExpand(createdProduct.getProductId()).toUri();

            return ResponseEntity.created(location).body(createdProduct);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating product", e);
        }
    }

    /**
     * GET /api/v1/seller/products/my : Get products listed by the authenticated seller.
     * Calls SellerProductService to handle retrieval.
     */
    @GetMapping("/my") // READ (Seller's own)
    public ResponseEntity<Page<DtoProductSummary>> getMyProducts(Authentication authentication, Pageable pageable) {
        Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);
        // Call the method on the injected sellerProductService
        Page<DtoProductSummary> productPage = sellerProductService.getMyProducts(sellerId, pageable); // CHANGED
        return ResponseEntity.ok(productPage);
    }

    /**
     * PUT /api/v1/seller/products/{productId} : Update a product owned by the authenticated seller.
     * Calls SellerProductService to handle update and authorization.
     */
    @PutMapping("/{productId}") // UPDATE
    public ResponseEntity<DtoProduct> updateMyProduct(
            Authentication authentication,
            @PathVariable Long productId,
            @Valid @RequestBody DtoProduct dtoProduct) {
        try {
            Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);

            if (dtoProduct.getProductId() != null && !dtoProduct.getProductId().equals(productId)) {
                 throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID in path must match ID in body");
            }
            dtoProduct.setProductId(productId);

            // Call the method on the injected sellerProductService
            DtoProduct updatedProduct = sellerProductService.updateMyProduct(productId, dtoProduct, sellerId); // CHANGED
            return ResponseEntity.ok(updatedProduct);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (AccessDeniedException e) { // Catch the specific exception thrown by the service
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating product", e);
        }
    }

    /**
     * DELETE /api/v1/seller/products/{productId} : Delete a product owned by the authenticated seller.
     * Calls SellerProductService to handle deletion and authorization.
     */
    @DeleteMapping("/{productId}") // DELETE
    public ResponseEntity<Void> deleteMyProduct(Authentication authentication, @PathVariable Long productId) {
        try {
            Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);
            // Call the method on the injected sellerProductService
            sellerProductService.deleteMyProduct(productId, sellerId); // CHANGED
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found", e);
        } catch (AccessDeniedException e) { // Catch the specific exception thrown by the service
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting product", e);
        }
    }
}

// --- SecurityUtils Helper remains the same ---