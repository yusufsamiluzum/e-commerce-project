package com.ecommerce.services.impl;

// Using user-provided DTOs
import com.ecommerce.dto.DtoAttribute;
import com.ecommerce.dto.DtoCategory;
import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductImage;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.dto.DtoUserSummary; // Assuming this DTO exists
import com.ecommerce.dto.DtoVariant;

import com.ecommerce.entities.product.*;
import com.ecommerce.entities.user.Seller; // Assuming Seller entity exists
// Using standard exceptions
import java.util.NoSuchElementException; // Standard exception for not found

import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductAttributeRepository;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.SellerRepository;

import com.ecommerce.services.ProductService;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

// Imports for Pagination
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor; // Using Lombok for constructor injection

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;


/**
 * Implementation of the ProductService interface using user-provided DTOs.
 * Handles business logic for product management using JPA repositories,
 * including pagination and standard exception handling.
 */
@Service
@RequiredArgsConstructor // Lombok annotation for constructor injection of final fields
public class ProductServiceImpl implements ProductService { // Implement the updated interface

    // Inject necessary repositories via constructor
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SellerRepository sellerRepository; // Assuming this exists
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeRepository productAttributeRepository;
    
    
    
    
    @Override
    @Transactional(readOnly = true)
    public Page<DtoProductSummary> searchProducts(String searchTerm, Pageable pageable) {
        // Sadece arama terimine göre specification oluştur
        Specification<Product> spec = Specification.where(matchesSearchTerm(searchTerm));

        // Specification'ı kullanarak ürünleri bul
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // Sonuçları DTO'ya map et
        List<DtoProductSummary> summaries = productPage.getContent().stream()
                .map(this::mapProductToDtoProductSummary) // Mevcut map'leme metodunu kullan
                .collect(Collectors.toList());

        return new PageImpl<>(summaries, pageable, productPage.getTotalElements());
    }
    
    
    
    private static Specification<Product> matchesSearchTerm(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.isBlank()) {
                // Arama terimi yoksa hiçbir filtre uygulama (veya bir "aktif/onaylı ürün" filtresi eklenebilir)
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + searchTerm.toLowerCase() + "%";

            // Tekrarlanan sonuçları önlemek için (özellikle join yapıldığında)
            query.distinct(true);

            // Kategori adına göre arama yapmak için join (LEFT JOIN daha güvenli olabilir)
            Join<Product, Category> categorySearchJoin = root.join("categories", JoinType.LEFT); // LEFT JOIN

            // Hangi alanlarda arama yapılacağını belirle
            Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern);
            Predicate descriptionPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern);
            Predicate brandPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), likePattern);
            Predicate modelPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("model")), likePattern); // Model eklendi
            Predicate categoryNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(categorySearchJoin.get("name")), likePattern);

            // Bu alanlardan herhangi birinde eşleşme varsa ürünü dahil et (OR)
            return criteriaBuilder.or(
                    namePredicate,
                    descriptionPredicate,
                    brandPredicate,
                    modelPredicate, // Model eklendi
                    categoryNamePredicate
            );
        };
    }
    
    

    @Override
    @Transactional // Ensure atomicity
    public DtoProduct createProduct(DtoProduct dtoProduct, Long sellerId) {
        // 1. Find the seller
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new NoSuchElementException("Seller not found with id: " + sellerId));

        // 2. Map DTO to Product entity
        Product product = mapDtoToProduct(new Product(), dtoProduct);
        product.setSeller(seller);
        product.setApproved(Boolean.FALSE); // New products are not approved by default

        // 3. Handle Categories (Extract IDs from DtoCategory objects)
        Set<Long> categoryIds = dtoProduct.getCategories() != null ?
                                dtoProduct.getCategories().stream()
                                    .map(DtoCategory::getCategoryId)
                                    .filter(id -> id != null) // Filter out null IDs just in case
                                    .collect(Collectors.toSet())
                                : Collections.emptySet();
        Set<Category> categories = findAndValidateCategories(categoryIds);
        product.setCategories(categories);

        // 4. Save the main product entity first to get an ID
        Product savedProduct = productRepository.save(product);

        // 5. Handle Images (associated with the saved product)
        List<ProductImage> images = mapDtoImageToImage(dtoProduct.getImages(), savedProduct);
        if (images != null && !images.isEmpty()) {
            productImageRepository.saveAll(images); // Save images
        }

        // 6. Handle Variants and their Attributes (associated with the saved product)
        List<ProductVariant> variants = mapDtoVariantToVariant(dtoProduct.getVariants(), savedProduct);
         if (variants != null && !variants.isEmpty()) {
            productVariantRepository.saveAll(variants); // Save variants (and cascaded attributes)
         }

        // 7. Reload the product to ensure all associations are fresh before mapping to response
        // Use a fetch method that joins related entities if needed, or rely on transaction context
        Product fullyLoadedProduct = productRepository.findById(savedProduct.getProductId())
             .orElseThrow(() -> new NoSuchElementException("Failed to reload product after creation: " + savedProduct.getProductId())); // Should not happen

        // 8. Map the final saved entity (with associations) to the response DTO
        return mapProductToDtoProduct(fullyLoadedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public DtoProduct getProductById(Long productId) {
        // Use a repository method that potentially fetches associations eagerly if needed for mapping
        Product product = productRepository.findById(productId) // Or findProductWithAssociationsById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + productId));
        return mapProductToDtoProduct(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DtoProductSummary> getAllProducts(Pageable pageable) {
        // Use the repository method that supports pagination
        Page<Product> productPage = productRepository.findAll(pageable);
        // Map the Page<Product> to Page<DtoProductSummary>
        List<DtoProductSummary> summaries = productPage.getContent().stream()
                .map(this::mapProductToDtoProductSummary)
                .collect(Collectors.toList());
        return new PageImpl<>(summaries, pageable, productPage.getTotalElements());
    }

     @Override
     @Transactional(readOnly = true)
     public Page<DtoProductSummary> getProductsBySeller(Long sellerId, Pageable pageable) {
         if (!sellerRepository.existsById(sellerId)) {
             throw new NoSuchElementException("Seller not found with id: " + sellerId);
         }
         // Assuming repository method supports pagination
         Page<Product> productPage = productRepository.findBySellerUserId(sellerId, pageable);
         List<DtoProductSummary> summaries = productPage.getContent().stream()
                 .map(this::mapProductToDtoProductSummary)
                 .collect(Collectors.toList());
         return new PageImpl<>(summaries, pageable, productPage.getTotalElements());
     }

     @Override
     @Transactional(readOnly = true)
     public Page<DtoProductSummary> getProductsByCategory(Long categoryId, Pageable pageable) {
         Category category = categoryRepository.findById(categoryId)
                 .orElseThrow(() -> new NoSuchElementException("Category not found with id: " + categoryId));
         // Assuming repository method supports pagination
          Page<Product> productPage = productRepository.findByCategoriesContains(category, pageable);
          List<DtoProductSummary> summaries = productPage.getContent().stream()
                 .map(this::mapProductToDtoProductSummary)
                 .collect(Collectors.toList());
          return new PageImpl<>(summaries, pageable, productPage.getTotalElements());
     }


    @Override
    @Transactional
    public DtoProduct updateProduct(Long productId, DtoProduct dtoProduct) {
        // 1. Find the existing product
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + productId));

        // TODO: Add authorization check here - does the current user own this product or is an admin?

        // 2. Update basic fields from DTO
        mapDtoToProduct(existingProduct, dtoProduct); // Reuse mapping logic

        // 3. Update Categories
         Set<Long> categoryIds = dtoProduct.getCategories() != null ?
                                 dtoProduct.getCategories().stream()
                                    .map(DtoCategory::getCategoryId)
                                    .filter(id -> id != null)
                                    .collect(Collectors.toSet())
                                 : Collections.emptySet();
        Set<Category> categories = findAndValidateCategories(categoryIds);
        existingProduct.setCategories(categories); // Update the association

        // 4. Update Images (Simplistic approach: Remove old, add new)
        productImageRepository.deleteByProductProductId(existingProduct.getProductId()); // Assumes this method exists
        List<ProductImage> newImages = mapDtoImageToImage(dtoProduct.getImages(), existingProduct);
        if (newImages != null && !newImages.isEmpty()) {
            productImageRepository.saveAll(newImages);
        }
        // Note: For JPA relationships, clearing and adding to the collection on the entity side
        // (e.g., existingProduct.getImages().clear(); existingProduct.getImages().addAll(newImages);)
        // might be needed depending on cascade settings and how JPA manages the relationship.
        // Managing via repository delete/saveAll is often simpler if cascading isn't fully relied upon.

        // 5. Update Variants (Simplistic approach: Remove old, add new)
        // First delete attributes associated with old variants of this product
        List<ProductVariant> oldVariants = productVariantRepository.findByProductProductId(existingProduct.getProductId());
        oldVariants.forEach(variant -> productAttributeRepository.deleteByVariantVariantId(variant.getVariantId())); // Assumes method exists
        // Then delete the old variants
        productVariantRepository.deleteByProductProductId(existingProduct.getProductId()); // Assumes method exists

        List<ProductVariant> newVariants = mapDtoVariantToVariant(dtoProduct.getVariants(), existingProduct);
         if (newVariants != null && !newVariants.isEmpty()) {
            productVariantRepository.saveAll(newVariants); // Saves variants and cascades to attributes
         }
        // Similar note as for images regarding managing the collection on the entity side.

        // 6. Save the updated product
        Product updatedProduct = productRepository.save(existingProduct);

        // 7. Reload and Map to response DTO
        // Fetching again ensures we get the state after all saves/cascades
        Product fullyLoadedProduct = productRepository.findById(updatedProduct.getProductId())
             .orElseThrow(() -> new NoSuchElementException("Failed to reload product after update: " + updatedProduct.getProductId()));

        return mapProductToDtoProduct(fullyLoadedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        // 1. Find product to ensure it exists before attempting delete
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + productId));

        // TODO: Add authorization check

        // 2. Explicitly delete related entities if cascading is not configured or reliable enough.
        // Order matters: delete attributes, then variants, then images before the product.
        List<ProductVariant> variants = productVariantRepository.findByProductProductId(productId);
        variants.forEach(variant -> productAttributeRepository.deleteByVariantVariantId(variant.getVariantId()));
        productVariantRepository.deleteByProductProductId(productId);
        productImageRepository.deleteByProductProductId(productId);

        // 3. Delete the product itself
        productRepository.delete(product);
    }

    @Override
    @Transactional
    public DtoProduct approveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + productId));

        // TODO: Add authorization check (Admin only)

        product.setApproved(Boolean.TRUE);
        product.setApprovedAt(LocalDateTime.now());
        Product savedProduct = productRepository.save(product);

        // Fetching again to ensure the updated state is mapped
        Product fullyLoadedProduct = productRepository.findById(savedProduct.getProductId())
             .orElseThrow(() -> new NoSuchElementException("Failed to reload product after approval: " + savedProduct.getProductId()));

        return mapProductToDtoProduct(fullyLoadedProduct);
    }

    // --- New Category Method Implementation ---

    /**
     * Retrieves all categories from the repository and maps them to DTOs.
     *
     * @return A List of DtoCategory.
     */
    @Override
    @Transactional(readOnly = true) // Use read-only transaction
    public List<DtoCategory> getAllCategories() {
        List<Category> categories = categoryRepository.findAll(); // Fetch all categories
        // Map Category entities to DtoCategory objects
        return categories.stream()
                .map(this::mapCategoryToDtoCategory) // Use a helper mapping method
                .collect(Collectors.toList());
    }
    
    
    
    @Override
    @Transactional
    public DtoCategory createCategory(DtoCategory dtoCategory) {
        if (dtoCategory == null || dtoCategory.getName() == null || dtoCategory.getName().isBlank()) {
            // Add appropriate error handling, e.g., throw new IllegalArgumentException("Category name cannot be empty");
            // Or handle via validation annotations on the DTO at the controller level
        }

        Category category = new Category();
        category.setName(dtoCategory.getName());
        category.setDescription(dtoCategory.getDescription());
        // Set other fields if any from dtoCategory

        // Check if category with the same name already exists to avoid duplicates, if needed
        // Optional<Category> existingCategory = categoryRepository.findByName(dtoCategory.getName()); // Assuming findByName method exists or is added to CategoryRepository
        // if (existingCategory.isPresent()) {
        //     throw new DataIntegrityViolationException("Category with name '" + dtoCategory.getName() + "' already exists.");
        // }


        Category savedCategory = categoryRepository.save(category);
        return mapCategoryToDtoCategory(savedCategory); // Use existing or create a new private helper method if mapCategoryToDtoCategory is not suitable or accessible
    }

    
    
    

    // --- Helper Methods for Mapping (Largely unchanged, ensure they fetch data if needed) ---

    /**
     * Helper method to map a Category entity to a DtoCategory.
     *
     * @param category The Category entity.
     * @return The corresponding DtoCategory.
     */
    private DtoCategory mapCategoryToDtoCategory(Category category) {
        return new DtoCategory(
                category.getCategoryId(),
                category.getName(),
                category.getDescription()
        );
    }

    /**
     * Maps a Product entity to a full DtoProduct.
     * Assumes associations (categories, images, variants, attributes, seller) might need explicit fetching
     * if not loaded eagerly or within the current transaction context.
     */
    private DtoProduct mapProductToDtoProduct(Product product) {
        DtoProduct dto = new DtoProduct();
        // Map basic fields
        dto.setProductId(product.getProductId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setBrand(product.getBrand());
        dto.setModel(product.getModel());
        dto.setDimensions(product.getDimensions());
        dto.setWeight(product.getWeight());
        dto.setColor(product.getColor());
        dto.setWarranty(product.getWarranty());
        dto.setKeyFeatures(product.getKeyFeatures() != null ? new HashSet<>(product.getKeyFeatures()) : null);
        dto.setSpecifications(product.getSpecifications() != null ? new HashMap<>(product.getSpecifications()) : null);
        dto.setAverageRating(product.getAverageRating());
        dto.setReviewCount(product.getReviewCount());

        // Map Seller to DtoUserSummary
        if (product.getSeller() != null) {
            // Assuming DtoUserSummary exists and has appropriate fields/constructor
            DtoUserSummary sellerSummary = new DtoUserSummary(); // Replace with actual constructor/setters
             sellerSummary.setUserId(product.getSeller().getUserId()); // Assuming Seller has getSellerId()
             sellerSummary.setUsername(product.getSeller().getUsername()); // Assuming Seller has getName()
            dto.setSeller(sellerSummary);
        }

        // Map Categories
        // If lazily loaded, this access will trigger fetching within the transaction
        dto.setCategories(product.getCategories().stream()
                .map(cat -> new DtoCategory(cat.getCategoryId(), cat.getName(), cat.getDescription()))
                .collect(Collectors.toSet()));

        // Map Images - Explicit fetch recommended if lazy loading is default
        List<ProductImage> images = productImageRepository.findByProductProductId(product.getProductId());
        dto.setImages(images.stream()
                .map(img -> new DtoProductImage(img.getImageId(), img.getImageUrl(), img.isPrimary(), img.getAltText()))
                .collect(Collectors.toList()));

        // Map Variants and their Attributes - Explicit fetch recommended
        List<ProductVariant> variants = productVariantRepository.findByProductProductId(product.getProductId());
        dto.setVariants(variants.stream()
                .map(var -> {
                    // Fetch attributes for *this* variant explicitly
                    List<ProductAttribute> attributes = productAttributeRepository.findByVariantVariantId(var.getVariantId());
                    List<DtoAttribute> attributeDTOs = attributes.stream()
                            .map(attr -> new DtoAttribute(
                                    attr.getAttributeId(),
                                    attr.getName(),
                                    attr.getValue(),
                                    attr.getUnit(),
                                    attr.getAttributeGroup()))
                            .collect(Collectors.toList());
                    return new DtoVariant(
                            var.getVariantId(),
                            var.getSku(),
                            var.getPriceAdjustment(),
                            var.getStockQuantity(),
                            attributeDTOs);
                })
                .collect(Collectors.toList()));

        // General attributes list in DtoProduct remains unmapped as per previous logic
         dto.setAttributes(Collections.emptyList());

        return dto;
    }

    /**
     * Maps a Product entity to a DtoProductSummary.
     */
     private DtoProductSummary mapProductToDtoProductSummary(Product product) {
         DtoProductSummary summary = new DtoProductSummary();
         summary.setProductId(product.getProductId());
         summary.setName(product.getName());
         summary.setPrice(product.getPrice());
         summary.setAverageRating(product.getAverageRating());
         summary.setBrand(product.getBrand());
         summary.setModel(product.getModel());

         // Find primary image URL efficiently
         // Consider optimizing this (e.g., denormalization or specific query)
         String primaryImageUrl = productImageRepository.findByProductProductIdAndIsPrimary(product.getProductId(), true)
                                     .map(ProductImage::getImageUrl)
                                     .orElseGet(() ->
                                         productImageRepository.findFirstByProductProductIdOrderByImageIdAsc(product.getProductId())
                                             .map(ProductImage::getImageUrl)
                                             .orElse(null) // Or a placeholder URL
                                     );
         summary.setPrimaryImageUrl(primaryImageUrl);

         return summary;
     }


    /**
     * Maps a DtoProduct to a Product entity (for creation or update).
     * Does NOT handle associations like Seller, Categories, Images, Variants.
     */
    private Product mapDtoToProduct(Product product, DtoProduct dto) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setBrand(dto.getBrand());
        product.setModel(dto.getModel());
        product.setDimensions(dto.getDimensions());
        product.setWeight(dto.getWeight());
        product.setColor(dto.getColor());
        product.setWarranty(dto.getWarranty());
        product.setKeyFeatures(dto.getKeyFeatures() != null ? new HashSet<>(dto.getKeyFeatures()) : new HashSet<>());
        product.setSpecifications(dto.getSpecifications() != null ? new HashMap<>(dto.getSpecifications()) : new HashMap<>());
        return product;
    }

     /**
      * Finds categories by ID and validates their existence.
      */
     private Set<Category> findAndValidateCategories(Set<Long> categoryIds) {
         if (categoryIds == null || categoryIds.isEmpty()) {
             return new HashSet<>();
         }
         Set<Category> categories = new HashSet<>(categoryRepository.findAllById(categoryIds));
         if (categories.size() != categoryIds.size()) {
             Set<Long> foundIds = categories.stream().map(Category::getCategoryId).collect(Collectors.toSet());
             Set<Long> missingIds = new HashSet<>(categoryIds);
             missingIds.removeAll(foundIds);
             // Use standard exception
             throw new NoSuchElementException("Could not find categories with IDs: " + missingIds);
         }
         return categories;
     }

     /**
      * Maps DtoProductImage list to ProductImage entities.
      */
     private List<ProductImage> mapDtoImageToImage(List<DtoProductImage> imageDtos, Product product) {
         if (imageDtos == null || imageDtos.isEmpty()) return Collections.emptyList();
         return imageDtos.stream().map(dto -> {
             ProductImage img = new ProductImage();
             img.setProduct(product);
             img.setImageUrl(dto.getImageUrl());
             img.setPrimary(dto.isPrimary());
             img.setAltText(dto.getAltText());
             return img;
         }).collect(Collectors.toList());
     }

    /**
     * Maps DtoVariant list (and their nested DtoAttribute list) to ProductVariant entities.
     */
    private List<ProductVariant> mapDtoVariantToVariant(List<DtoVariant> variantDtos, Product product) {
        if (variantDtos == null || variantDtos.isEmpty()) return Collections.emptyList();

        return variantDtos.stream().map(variantDto -> {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSku(variantDto.getSku());
            variant.setPriceAdjustment(variantDto.getPriceAdjustment());
            variant.setStockQuantity(variantDto.getStockQuantity());

            List<ProductAttribute> attributes = mapDtoAttributeToAttribute(variantDto.getAttributes(), product, variant);
            variant.setAttributes(attributes); // Set attributes back onto the variant

            return variant;
        }).collect(Collectors.toList());
    }

     /**
      * Maps DtoAttribute list to ProductAttribute entities.
      */
     private List<ProductAttribute> mapDtoAttributeToAttribute(List<DtoAttribute> attributeDtos, Product product, ProductVariant variant) {
         if (attributeDtos == null || attributeDtos.isEmpty()) return Collections.emptyList();
         return attributeDtos.stream().map(attrDto -> {
             ProductAttribute attr = new ProductAttribute();
             attr.setProduct(product);
             attr.setVariant(variant);
             attr.setName(attrDto.getName());
             attr.setValue(attrDto.getValue());
             attr.setUnit(attrDto.getUnit());
             attr.setAttributeGroup(attrDto.getAttributeGroup());
             return attr;
         }).collect(Collectors.toList());
     }

     @Override
    @Transactional(readOnly = true)
    public Page<DtoProductSummary> filterProducts(
            String searchTerm,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            // Add other parameters corresponding to new specifications
            Pageable pageable) {

        // Start with a base specification (matches all)
        Specification<Product> spec = Specification.where(null); // Equivalent to criteriaBuilder.conjunction() initially

        // Conditionally add filters by chaining specifications with AND
        spec = spec.and(hasCategory(categoryId));
        spec = spec.and(priceBetween(minPrice, maxPrice));
        spec = spec.and(matchesSearchTerm(searchTerm));

        // Add other filters similarly:
        // spec = spec.and(hasBrand(brand));
        // spec = spec.and(hasMinRating(minRating));


        // Execute the combined specification
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // Map results to DTOs
        List<DtoProductSummary> summaries = productPage.getContent().stream()
                .map(this::mapProductToDtoProductSummary)
                .collect(Collectors.toList());

        return new PageImpl<>(summaries, pageable, productPage.getTotalElements());
    }

    // Inside ProductServiceImpl.java (or a new class ProductSpecifications)
    private static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return criteriaBuilder.conjunction(); // No filter if categoryId is null
            }
            Join<Product, Category> categoryJoin = root.join("categories");
            return criteriaBuilder.equal(categoryJoin.get("categoryId"), categoryId);
        };
    }

    private static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            // Return combined price predicates (or an always-true predicate if no price filter)
            return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


    

    // You already have a helper method mapCategoryToDtoCategory in ProductServiceImpl.java
    // private DtoCategory mapCategoryToDtoCategory(Category category) {
//         return new DtoCategory(
//                 category.getCategoryId(),
//                 category.getName(),
//                 category.getDescription()
//         );
    // }

// Add more specifications as needed (e.g., by brand, by rating)
// private static Specification<Product> hasBrand(String brand) { ... }
// private static Specification<Product> hasMinRating(Double minRating) { ... }
}

