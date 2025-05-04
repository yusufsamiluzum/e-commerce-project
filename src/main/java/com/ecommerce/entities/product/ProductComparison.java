package com.ecommerce.entities.product;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import com.ecommerce.entities.user.Customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ProductComparison {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long comparisonId;
    
    // Optional - can be null for guest users
    @ManyToOne
    @JoinColumn(name = "customer_id") // Foreign key column in the database
    private Customer customer; // Field name MUST be "customer"
    
    // Session ID for guest users
    private String sessionId;
    
    @ManyToMany
    @JoinTable(
        name = "comparison_products",
        joinColumns = @JoinColumn(name = "comparison_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> products = new HashSet<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Optional - name of saved comparison
    @Column(name = "name", length = 100,unique = true)
    private String name;
    
    // Limit comparisons to same category for meaningful comparison
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
