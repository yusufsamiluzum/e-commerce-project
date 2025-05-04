package com.ecommerce.entities.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ecommerce.entities.Review;
import com.ecommerce.entities.cart.CartItem;
import com.ecommerce.entities.order.OrderItem;
import com.ecommerce.entities.user.Seller;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;
    
    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Seller seller;
    
    // Brand and model for comparison
    @Column(name = "brand", length = 50)
    private String brand;
    
    @Column(name = "model", length = 50)
    private String model;
    
    // Basic specifications for comparison
    private String dimensions;
    private String weight;
    private String color;
    private String warranty;
    
    // For easy comparison and filtering
    @ElementCollection
    private Set<String> keyFeatures = new HashSet<>();
    
    // Technical specs as key-value pairs for detailed comparison
    @ElementCollection
    private Map<String, String> specifications = new HashMap<>();
    
    // Rating metrics for comparison
    private Double averageRating;
    private Integer reviewCount;
    
    // For comparison of similar products
    @ManyToMany
    @JoinTable(
        name = "product_category",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();
    
    @OneToMany(mappedBy = "product")
    private List<CartItem> cartItems = new ArrayList<>();
    
    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems = new ArrayList<>();
    
    @OneToMany(mappedBy = "product")
    private List<Review> reviews = new ArrayList<>();
    
    // Additional fields
    @Column(name = "is_approved")
    private boolean isApproved = false;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
