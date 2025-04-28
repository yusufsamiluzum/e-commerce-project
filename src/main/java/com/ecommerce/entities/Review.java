package com.ecommerce.entities;


import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Customer;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Entity
@Data
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Min(1) @Max(5)
    private int rating;
    
    private String comment;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
