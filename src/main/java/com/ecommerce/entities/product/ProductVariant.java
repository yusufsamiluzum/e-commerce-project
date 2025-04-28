package com.ecommerce.entities.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    private String sku;
    private BigDecimal priceAdjustment;
    private int stockQuantity;
    
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL)
    private List<ProductAttribute> attributes = new ArrayList<>();
}
