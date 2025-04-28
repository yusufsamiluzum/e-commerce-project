package com.ecommerce.entities.product;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    
    @Column(name = "name", nullable = false, unique = true)
    @NotNull(message = "Category name cannot be null")
    private String name;

    private String description;
    
    @ManyToMany(mappedBy = "categories")
    private Set<Product> products = new HashSet<>();
    
}
