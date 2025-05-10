package com.ecommerce.controller.impl;

import com.ecommerce.dto.DtoCategory;
import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductImage;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.product.ProductImage;
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.services.IFileStorageService;
import com.ecommerce.services.ProductService;

import java.io.IOException;
import jakarta.validation.Valid; // For input validation
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // For method-level security
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;



/**
 * REST Controller for administrative product management operations.
 * Requires ADMIN role for access.
 */
@RestController
@RequestMapping("/api/v1/admin/products") // Base path for admin product endpoints
@RequiredArgsConstructor // Injects ProductService via constructor
@PreAuthorize("hasRole('ADMIN')") // Class-level security: All methods require ADMIN role
public class AdminProductController {

	
    private final ProductService productService;
    private final IFileStorageService fileStorageService;
    private final ProductImageRepository productImageRepository; 
    private final ProductRepository productRepository; 
    
    
    private static final Logger log = LoggerFactory.getLogger(SellerProductController.class);

    
    
    /**
     * POST /api/v1/admin/products/{productId}/images : Upload an image for a specific product.
     * Requires ADMIN role.
     *
     * @param productId The ID of the product to associate the image with.
     * @param file      The image file uploaded via multipart form data.
     * @param isPrimary Optional request parameter to mark the image as primary.
     * @return ResponseEntity containing the DtoProductImage of the saved image or an error.
     */
    @PostMapping("/{productId}/images")
    public ResponseEntity<?> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select a file to upload."));
        }

        // Ürünü bul
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // TODO: Add validation for file type (e.g., allow only jpg, png) and size

        try {
            // Dosyayı kaydet ve benzersiz adını al
            String uniqueFilename = fileStorageService.saveFile(file);

            // Veritabanına ProductImage kaydı oluştur
            ProductImage productImage = new ProductImage();
            productImage.setProduct(product);
            // URL'yi oluştururken MvcConfig'deki handler path'i kullan (/product-images/)
            productImage.setImageUrl("/product-images/" + uniqueFilename); // Erişilebilir URL
            productImage.setPrimary(isPrimary);
            // productImage.setAltText(...); // İsteğe bağlı alt text

            // Eğer bu primary olarak işaretlendiyse, diğerlerini primary değil yap
            if (isPrimary) {
                productImageRepository.findByProductProductId(productId).forEach(img -> {
                    if (img.isPrimary()) {
                        img.setPrimary(false);
                        productImageRepository.save(img);
                    }
                });
            }

            ProductImage savedImage = productImageRepository.save(productImage);

            // Başarılı yanıtı DTO ile döndür (DtoProductImage DTO'nuz olmalı)
            DtoProductImage responseDto = new DtoProductImage(
                    savedImage.getImageId(),
                    savedImage.getImageUrl(),
                    savedImage.isPrimary(),
                    savedImage.getAltText()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

        } catch (IOException e) {
            log.error("Failed to upload image for product ID: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not upload image. Error: " + e.getMessage()));
        } catch (ResourceNotFoundException e) {
             // Product bulunamazsa 404 dön
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
             log.error("Unexpected error during image upload for product ID: {}", productId, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(Map.of("error", "An unexpected error occurred during image upload."));
         }
    }
    
    



    /**
     * POST /api/v1/admin/products : Create a new product.
     * Requires ADMIN role. The seller ID must be provided.
     *
     * @param sellerId   The ID of the Seller associated with this product.
     * @param dtoProduct The DTO containing product details.
     * @return ResponseEntity with status 201 CREATED and the created DtoProduct,
     * or 404 NOT_FOUND if the specified seller doesn't exist.
     */
    @PostMapping
    public ResponseEntity<DtoProduct> createProduct(
            @RequestParam Long sellerId, // Get seller ID from request param
            @Valid @RequestBody DtoProduct dtoProduct) { // Validate the incoming DTO
        try {
            // Note: Ensure dtoProduct does not contain an ID if creating
            dtoProduct.setProductId(null); // Explicitly nullify ID for creation
            DtoProduct createdProduct = productService.createProduct(dtoProduct, sellerId);

            // Build the location URI of the newly created resource
            URI location = ServletUriComponentsBuilder
                    .fromCurrentContextPath().path("/api/v1/products/{id}") // Point to the public GET endpoint
                    .buildAndExpand(createdProduct.getProductId()).toUri();

            return ResponseEntity.created(location).body(createdProduct);
        } catch (NoSuchElementException e) {
            // Thrown if the sellerId or a categoryId in the DTO is not found
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
             // Catch other potential exceptions during creation
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating product", e);
        }
    }

    /**
     * PUT /api/v1/admin/products/{productId} : Update an existing product.
     * Requires ADMIN role.
     *
     * @param productId  The ID of the product to update.
     * @param dtoProduct The DTO containing updated product details.
     * @return ResponseEntity containing the updated DtoProduct.
     * @throws ResponseStatusException with 404 NOT_FOUND if the product or related entities don't exist.
     */
    @PutMapping("/{productId}")
    public ResponseEntity<DtoProduct> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody DtoProduct dtoProduct) { // Validate the incoming DTO
        try {
            // Ensure the ID in the path matches the DTO if present, or set it
            if (dtoProduct.getProductId() != null && !dtoProduct.getProductId().equals(productId)) {
                 throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID in path must match ID in body");
            }
            dtoProduct.setProductId(productId); // Set ID from path for the service call

            DtoProduct updatedProduct = productService.updateProduct(productId, dtoProduct);
            return ResponseEntity.ok(updatedProduct);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating product", e);
        }
    }

    /**
     * DELETE /api/v1/admin/products/{productId} : Delete a product.
     * Requires ADMIN role.
     *
     * @param productId The ID of the product to delete.
     * @return ResponseEntity with status 204 NO_CONTENT.
     * @throws ResponseStatusException with 404 NOT_FOUND if the product doesn't exist.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        try {
            productService.deleteProduct(productId);
            return ResponseEntity.noContent().build(); // Standard response for successful DELETE
        } catch (NoSuchElementException e) {
            // Service throws if product not found before delete attempt
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found", e);
        } catch (Exception e) {
            // Catch other potential errors during deletion (e.g., constraint violations if not handled)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting product", e);
        }
    }

    /**
     * PATCH /api/v1/admin/products/{productId}/approve : Approve a product.
     * Requires ADMIN role.
     *
     * @param productId The ID of the product to approve.
     * @return ResponseEntity containing the updated DtoProduct with approval status set.
     * @throws ResponseStatusException with 404 NOT_FOUND if the product doesn't exist.
     */
    @PatchMapping("/{productId}/approve")
    public ResponseEntity<DtoProduct> approveProduct(@PathVariable Long productId) {
        try {
            DtoProduct approvedProduct = productService.approveProduct(productId);
            return ResponseEntity.ok(approvedProduct);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found", e);
        } catch (Exception e) {
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error approving product", e);
        }
    }
    
    
    @PostMapping("/categories") // Adjust mapping if in a separate controller, e.g., @RequestMapping("/api/v1/admin/categories") then @PostMapping
    public ResponseEntity<DtoCategory> addCategory(@Valid @RequestBody DtoCategory dtoCategory) {
        try {
            DtoCategory createdCategory = productService.createCategory(dtoCategory); // Assuming productService has createCategory

            // Optionally, build URI for the newly created resource
            URI location = ServletUriComponentsBuilder
                    .fromCurrentContextPath().path("/api/v1/categories/{id}") // Example path to get a category
                    .buildAndExpand(createdCategory.getCategoryId()).toUri();

            return ResponseEntity.created(location).body(createdCategory);
        } catch (DataIntegrityViolationException e) { // Example: if category name must be unique and is violated
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (Exception e) {
            // General error handling
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating category", e);
        }
    }
    
    
}
